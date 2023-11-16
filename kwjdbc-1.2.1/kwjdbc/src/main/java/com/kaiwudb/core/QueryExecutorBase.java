/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.core;

import com.kaiwudb.KWConnection;
import com.kaiwudb.KWNotification;
import com.kaiwudb.KWProperty;
import com.kaiwudb.jdbc.AutoSave;
import com.kaiwudb.jdbc.EscapeSyntaxCallMode;
import com.kaiwudb.jdbc.PreferQueryMode;
import com.kaiwudb.util.HostSpec;
import com.kaiwudb.util.LruCache;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.util.ServerErrorMessage;
import com.kaiwudb.util.KSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class QueryExecutorBase implements QueryExecutor {

  private static final Logger LOGGER = Logger.getLogger(QueryExecutorBase.class.getName());
  protected final KWStream kwStream;
  private final String user;
  private final String database;
  private final int cancelSignalTimeout;

  private int cancelPid;
  private int cancelKey;
  private boolean closed = false;
  private String serverVersion;
  private int serverVersionNum = 0;
  private TransactionState transactionState;
  private final boolean reWriteBatchedInserts;
  private final boolean columnSanitiserDisabled;
  private final EscapeSyntaxCallMode escapeSyntaxCallMode;
  private final PreferQueryMode preferQueryMode;
  private AutoSave autoSave;
  private boolean flushCacheOnDeallocate = true;
  protected final boolean logServerErrorDetail;

  // default value for server versions that don't report standard_conforming_strings
  private boolean standardConformingStrings = false;

  private SQLWarning warnings;
  private final ArrayList<KWNotification> notifications = new ArrayList<KWNotification>();

  private final LruCache<Object, CachedQuery> statementCache;
  private final CachedQueryCreateAction cachedQueryCreateAction;

  // For getParameterStatuses(), GUC_REPORT tracking
  private final TreeMap<String,String> parameterStatuses
      = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

  protected QueryExecutorBase(KWStream kwStream, String user,
                              String database, int cancelSignalTimeout, Properties info) throws SQLException {
    this.kwStream = kwStream;
    this.user = user;
    this.database = database;
    this.cancelSignalTimeout = cancelSignalTimeout;
    this.reWriteBatchedInserts = KWProperty.REWRITE_BATCHED_INSERTS.getBoolean(info);
    this.columnSanitiserDisabled = KWProperty.DISABLE_COLUMN_SANITISER.getBoolean(info);
    String callMode = KWProperty.ESCAPE_SYNTAX_CALL_MODE.get(info);
    this.escapeSyntaxCallMode = EscapeSyntaxCallMode.of(callMode);
    String preferMode = KWProperty.PREFER_QUERY_MODE.get(info);
    this.preferQueryMode = PreferQueryMode.of(preferMode);
    this.autoSave = AutoSave.of(KWProperty.AUTOSAVE.get(info));
    this.logServerErrorDetail = KWProperty.LOG_SERVER_ERROR_DETAIL.getBoolean(info);
    this.cachedQueryCreateAction = new CachedQueryCreateAction(this);
    statementCache = new LruCache<Object, CachedQuery>(
        Math.max(0, KWProperty.PREPARED_STATEMENT_CACHE_QUERIES.getInt(info)),
        Math.max(0, KWProperty.PREPARED_STATEMENT_CACHE_SIZE_MIB.getInt(info) * 1024 * 1024),
        false,
        cachedQueryCreateAction,
        new LruCache.EvictAction<CachedQuery>() {
          @Override
          public void evict(CachedQuery cachedQuery) throws SQLException {
            cachedQuery.query.close();
          }
        });
  }

  protected abstract void sendCloseMessage() throws IOException;

  @Override
  public void setNetworkTimeout(int milliseconds) throws IOException {
    kwStream.setNetworkTimeout(milliseconds);
  }

  @Override
  public int getNetworkTimeout() throws IOException {
    return kwStream.getNetworkTimeout();
  }

  @Override
  public HostSpec getHostSpec() {
    return kwStream.getHostSpec();
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public String getDatabase() {
    return database;
  }

  public void setBackendKeyData(int cancelPid, int cancelKey) {
    this.cancelPid = cancelPid;
    this.cancelKey = cancelKey;
  }

  @Override
  public int getBackendPID() {
    return cancelPid;
  }

  @Override
  public void abort() {
    try {
      kwStream.getSocket().close();
    } catch (IOException e) {
      // ignore
    }
    closed = true;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      LOGGER.log(Level.FINEST, " FE=> Terminate");
      sendCloseMessage();
      kwStream.flush();
      kwStream.close();
    } catch (IOException ioe) {
      LOGGER.log(Level.FINEST, "Discarding IOException on close:", ioe);
    }

    closed = true;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void sendQueryCancel() throws SQLException {
    if (cancelPid <= 0) {
      return;
    }

    KWStream cancelStream = null;

    // Now we need to construct and send a cancel packet
    try {
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.log(Level.FINEST, " FE=> CancelRequest(pid={0},ckey={1})", new Object[]{cancelPid, cancelKey});
      }

      cancelStream =
          new KWStream(kwStream.getSocketFactory(), kwStream.getHostSpec(), cancelSignalTimeout);
      if (cancelSignalTimeout > 0) {
        cancelStream.getSocket().setSoTimeout(cancelSignalTimeout);
      }
      cancelStream.sendInteger4(16);
      cancelStream.sendInteger2(1234);
      cancelStream.sendInteger2(5678);
      cancelStream.sendInteger4(cancelPid);
      cancelStream.sendInteger4(cancelKey);
      cancelStream.flush();
      cancelStream.receiveEOF();
    } catch (IOException e) {
      // Safe to ignore.
      LOGGER.log(Level.FINEST, "Ignoring exception on cancel request:", e);
    } finally {
      if (cancelStream != null) {
        try {
          cancelStream.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  public synchronized void addWarning(SQLWarning newWarning) {
    if (warnings == null) {
      warnings = newWarning;
    } else {
      warnings.setNextWarning(newWarning);
    }
  }

  public synchronized void addNotification(KWNotification notification) {
    notifications.add(notification);
  }

  @Override
  public synchronized KWNotification[] getNotifications() throws SQLException {
    KWNotification[] array = notifications.toArray(new KWNotification[0]);
    notifications.clear();
    return array;
  }

  @Override
  public synchronized SQLWarning getWarnings() {
    SQLWarning chain = warnings;
    warnings = null;
    return chain;
  }

  @Override
  public String getServerVersion() {
    return serverVersion;
  }

  @Override
  public int getServerVersionNum() {
    if (serverVersionNum != 0) {
      return serverVersionNum;
    }
    return serverVersionNum = Utils.parseServerVersionStr(serverVersion);
  }

  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setServerVersionNum(int serverVersionNum) {
    this.serverVersionNum = serverVersionNum;
  }

  public synchronized void setTransactionState(TransactionState state) {
    transactionState = state;
  }

  public synchronized void setStandardConformingStrings(boolean value) {
    standardConformingStrings = value;
  }

  @Override
  public synchronized boolean getStandardConformingStrings() {
    return standardConformingStrings;
  }

  @Override
  public synchronized TransactionState getTransactionState() {
    return transactionState;
  }

  public void setEncoding(Encoding encoding) throws IOException {
    kwStream.setEncoding(encoding);
  }

  @Override
  public Encoding getEncoding() {
    return kwStream.getEncoding();
  }

  @Override
  public boolean isReWriteBatchedInsertsEnabled() {
    return this.reWriteBatchedInserts;
  }

  @Override
  public final CachedQuery borrowQuery(String sql) throws SQLException {
    return statementCache.borrow(sql);
  }

  @Override
  public final CachedQuery borrowCallableQuery(String sql) throws SQLException {
    return statementCache.borrow(new CallableQueryKey(sql));
  }

  @Override
  public final CachedQuery borrowReturningQuery(String sql, String[] columnNames) throws SQLException {
    return statementCache.borrow(new QueryWithReturningColumnsKey(sql, true, true,
        columnNames
    ));
  }

  @Override
  public CachedQuery borrowQueryByKey(Object key) throws SQLException {
    return statementCache.borrow(key);
  }

  @Override
  public void releaseQuery(CachedQuery cachedQuery) {
    statementCache.put(cachedQuery.key, cachedQuery);
  }

  @Override
  public final Object createQueryKey(String sql, boolean escapeProcessing,
      boolean isParameterized, String... columnNames) {
    Object key;
    if (columnNames == null || columnNames.length != 0) {
      // Null means "return whatever sensible columns are" (e.g. primary key, or serial, or something like that)
      key = new QueryWithReturningColumnsKey(sql, isParameterized, escapeProcessing, columnNames);
    } else if (isParameterized) {
      // If no generated columns requested, just use the SQL as a cache key
      key = sql;
    } else {
      key = new BaseQueryKey(sql, false, escapeProcessing);
    }
    return key;
  }

  @Override
  public CachedQuery createQueryByKey(Object key) throws SQLException {
    return cachedQueryCreateAction.create(key);
  }

  @Override
  public final CachedQuery createQuery(String sql, boolean escapeProcessing,
      boolean isParameterized, String... columnNames)
      throws SQLException {
    Object key = createQueryKey(sql, escapeProcessing, isParameterized, columnNames);
    // Note: cache is not reused here for two reasons:
    //   1) Simplify initial implementation for simple statements
    //   2) Non-prepared statements are likely to have literals, thus query reuse would not be often
    return createQueryByKey(key);
  }

  @Override
  public boolean isColumnSanitiserDisabled() {
    return columnSanitiserDisabled;
  }

  @Override
  public EscapeSyntaxCallMode getEscapeSyntaxCallMode() {
    return escapeSyntaxCallMode;
  }

  @Override
  public PreferQueryMode getPreferQueryMode() {
    return preferQueryMode;
  }

  @Override
  public AutoSave getAutoSave() {
    return autoSave;
  }

  @Override
  public void setAutoSave(AutoSave autoSave) {
    this.autoSave = autoSave;
  }

  protected boolean willHealViaReparse(SQLException e) {
    if (e == null || e.getSQLState() == null) {
      return false;
    }

    // "prepared statement \"S_2\" does not exist"
    if (KSQLState.INVALID_SQL_STATEMENT_NAME.getState().equals(e.getSQLState())) {
      return true;
    }
    if (!KSQLState.NOT_IMPLEMENTED.getState().equals(e.getSQLState())) {
      return false;
    }

    if (!(e instanceof KSQLException)) {
      return false;
    }

    KSQLException pe = (KSQLException) e;

    ServerErrorMessage serverErrorMessage = pe.getServerErrorMessage();
    if (serverErrorMessage == null) {
      return false;
    }
    // "cached plan must not change result type"
    String routine = pe.getServerErrorMessage().getRoutine();
    return "RevalidateCachedQuery".equals(routine) // 9.2+
        || "RevalidateCachedPlan".equals(routine); // <= 9.1
  }

  @Override
  public boolean willHealOnRetry(SQLException e) {
    if (autoSave == AutoSave.NEVER && getTransactionState() == TransactionState.FAILED) {
      // If autorollback is not activated, then every statement will fail with
      // 'transaction is aborted', etc, etc
      return false;
    }
    return willHealViaReparse(e);
  }

  public boolean isFlushCacheOnDeallocate() {
    return flushCacheOnDeallocate;
  }

  @Override
  public void setFlushCacheOnDeallocate(boolean flushCacheOnDeallocate) {
    this.flushCacheOnDeallocate = flushCacheOnDeallocate;
  }

  protected boolean hasNotifications() {
    return notifications.size() > 0;
  }

  @Override
  public final Map<String,String> getParameterStatuses() {
    return Collections.unmodifiableMap(parameterStatuses);
  }

  @Override
  public final String getParameterStatus(String parameterName) {
    return parameterStatuses.get(parameterName);
  }

  /**
   * Update the parameter status map in response to a new ParameterStatus
   * wire protocol message.
   *
   * <p>The server sends ParameterStatus messages when GUC_REPORT settings are
   * initially assigned and whenever they change.</p>
   *
   * <p>A future version may invoke a client-defined listener class at this point,
   * so this should be the only access path.</p>
   *
   * <p>Keys are case-insensitive and case-preserving.</p>
   *
   * <p>The server doesn't provide a way to report deletion of a reportable
   * parameter so we don't expose one here.</p>
   *
   * @param parameterName case-insensitive case-preserving name of parameter to create or update
   * @param parameterStatus new value of parameter
   * @see KWConnection#getParameterStatuses
   * @see KWConnection#getParameterStatus
   */
  protected void onParameterStatus(String parameterName, String parameterStatus) {
    if (parameterName == null || parameterName.equals("")) {
      throw new IllegalStateException("attempt to set GUC_REPORT parameter with null or empty-string name");
    }

    parameterStatuses.put(parameterName, parameterStatus);
  }
}
