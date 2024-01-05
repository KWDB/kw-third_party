/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
// Copyright (c) 2004, Open Cloud Limited.

package com.kaiwudb.core.v3;

import com.kaiwudb.KWProperty;
import com.kaiwudb.core.ConnectionFactory;
import com.kaiwudb.core.KWStream;
import com.kaiwudb.core.QueryExecutor;
import com.kaiwudb.core.ServerVersion;
import com.kaiwudb.core.SetupQueryRunner;
import com.kaiwudb.core.SocketFactoryFactory;
import com.kaiwudb.core.Utils;
import com.kaiwudb.core.Version;
import com.kaiwudb.gss.MakeGSS;
import com.kaiwudb.hostchooser.CandidateHost;
import com.kaiwudb.hostchooser.GlobalHostStatusTracker;
import com.kaiwudb.hostchooser.HostChooser;
import com.kaiwudb.hostchooser.HostChooserFactory;
import com.kaiwudb.hostchooser.HostRequirement;
import com.kaiwudb.hostchooser.HostStatus;
import com.kaiwudb.jdbc.SslMode;
import com.kaiwudb.jre7.sasl.ScramAuthenticator;
import com.kaiwudb.ssl.MakeSSL;
import com.kaiwudb.sspi.ISSPIClient;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.HostSpec;
import com.kaiwudb.util.MD5Digest;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.util.ServerErrorMessage;
import com.kaiwudb.util.KSQLException;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.SocketFactory;

/**
 * ConnectionFactory implementation for version 3 (7.4+) connections.
 *
 * @author Oliver Jowett (oliver@opencloud.com), based on the previous implementation
 */
public class ConnectionFactoryImpl extends ConnectionFactory {

  private static final Logger LOGGER = Logger.getLogger(ConnectionFactoryImpl.class.getName());
  private static final int AUTH_REQ_OK = 0;
  private static final int AUTH_REQ_KRB4 = 1;
  private static final int AUTH_REQ_KRB5 = 2;
  private static final int AUTH_REQ_PASSWORD = 3;
  private static final int AUTH_REQ_CRYPT = 4;
  private static final int AUTH_REQ_MD5 = 5;
  private static final int AUTH_REQ_SCM = 6;
  private static final int AUTH_REQ_GSS = 7;
  private static final int AUTH_REQ_GSS_CONTINUE = 8;
  private static final int AUTH_REQ_SSPI = 9;
  private static final int AUTH_REQ_SASL = 10;
  private static final int AUTH_REQ_SASL_CONTINUE = 11;
  private static final int AUTH_REQ_SASL_FINAL = 12;

  private ISSPIClient createSSPI(KWStream kwStream,
                                 String spnServiceClass,
                                 boolean enableNegotiate) {
    try {
      @SuppressWarnings("unchecked")
      Class<ISSPIClient> c = (Class<ISSPIClient>) Class.forName("com.kaiwudb.sspi.SSPIClient");
      return c.getDeclaredConstructor(KWStream.class, String.class, boolean.class)
          .newInstance(kwStream, spnServiceClass, enableNegotiate);
    } catch (Exception e) {
      // This catched quite a lot exceptions, but until Java 7 there is no ReflectiveOperationException
      throw new IllegalStateException("Unable to load SSPIClient."
          + " Please check that SSPIClient is included in your kwjdbc distribution.", e);
    }
  }

  private KWStream tryConnect(String user, String database,
                              Properties info, SocketFactory socketFactory, HostSpec hostSpec,
                              SslMode sslMode)
      throws SQLException, IOException {
    int connectTimeout = KWProperty.CONNECT_TIMEOUT.getInt(info) * 1000;

    KWStream newStream = new KWStream(socketFactory, hostSpec, connectTimeout);

    // Set the socket timeout if the "socketTimeout" property has been set.
    int socketTimeout = KWProperty.SOCKET_TIMEOUT.getInt(info);
    if (socketTimeout > 0) {
      newStream.getSocket().setSoTimeout(socketTimeout * 1000);
    }

    // Enable TCP keep-alive probe if required.
    boolean requireTCPKeepAlive = KWProperty.TCP_KEEP_ALIVE.getBoolean(info);
    newStream.getSocket().setKeepAlive(requireTCPKeepAlive);

    // Try to set SO_SNDBUF and SO_RECVBUF socket options, if requested.
    // If receiveBufferSize and send_buffer_size are set to a value greater
    // than 0, adjust. -1 means use the system default, 0 is ignored since not
    // supported.

    // Set SO_RECVBUF read buffer size
    int receiveBufferSize = KWProperty.RECEIVE_BUFFER_SIZE.getInt(info);
    if (receiveBufferSize > -1) {
      // value of 0 not a valid buffer size value
      if (receiveBufferSize > 0) {
        newStream.getSocket().setReceiveBufferSize(receiveBufferSize);
      } else {
        LOGGER.log(Level.WARNING, "Ignore invalid value for receiveBufferSize: {0}", receiveBufferSize);
      }
    }

    // Set SO_SNDBUF write buffer size
    int sendBufferSize = KWProperty.SEND_BUFFER_SIZE.getInt(info);
    if (sendBufferSize > -1) {
      if (sendBufferSize > 0) {
        newStream.getSocket().setSendBufferSize(sendBufferSize);
      } else {
        LOGGER.log(Level.WARNING, "Ignore invalid value for sendBufferSize: {0}", sendBufferSize);
      }
    }

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(Level.FINE, "Receive Buffer Size is {0}", newStream.getSocket().getReceiveBufferSize());
      LOGGER.log(Level.FINE, "Send Buffer Size is {0}", newStream.getSocket().getSendBufferSize());
    }

    // Construct and send an ssl startup packet if requested.
    newStream = enableSSL(newStream, sslMode, info, connectTimeout);

    List<String[]> paramList = getParametersForStartup(user, database, info);
    sendStartupPacket(newStream, paramList);

    // Do authentication (until AuthenticationOk).
    doAuthentication(newStream, hostSpec.getHost(), user, info);

    return newStream;
  }

  @Override
  public QueryExecutor openConnectionImpl(HostSpec[] hostSpecs, String user, String database,
                                          Properties info) throws SQLException {
    SslMode sslMode = SslMode.of(info);

    HostRequirement targetServerType;
    String targetServerTypeStr = KWProperty.TARGET_SERVER_TYPE.get(info);
    try {
      targetServerType = HostRequirement.getTargetServerType(targetServerTypeStr);
    } catch (IllegalArgumentException ex) {
      throw new KSQLException(
          GT.tr("Invalid targetServerType value: {0}", targetServerTypeStr),
          KSQLState.CONNECTION_UNABLE_TO_CONNECT);
    }

    SocketFactory socketFactory = SocketFactoryFactory.getSocketFactory(info);

    HostChooser hostChooser =
        HostChooserFactory.createHostChooser(hostSpecs, targetServerType, info);
    Iterator<CandidateHost> hostIter = hostChooser.iterator();
    Map<HostSpec, HostStatus> knownStates = new HashMap<HostSpec, HostStatus>();
    while (hostIter.hasNext()) {
      CandidateHost candidateHost = hostIter.next();
      HostSpec hostSpec = candidateHost.hostSpec;
      LOGGER.log(Level.FINE, "Trying to establish a protocol version 3 connection to {0}", hostSpec);

      // Note: per-connect-attempt status map is used here instead of GlobalHostStatusTracker
      // for the case when "no good hosts" match (e.g. all the hosts are known as "connectfail")
      // In that case, the system tries to connect to each host in order, thus it should not look into
      // GlobalHostStatusTracker
      HostStatus knownStatus = knownStates.get(hostSpec);
      if (knownStatus != null && !candidateHost.targetServerType.allowConnectingTo(knownStatus)) {
        if (LOGGER.isLoggable(Level.FINER)) {
          LOGGER.log(Level.FINER, "Known status of host {0} is {1}, and required status was {2}. Will try next host",
                     new Object[]{hostSpec, knownStatus, candidateHost.targetServerType});
        }
        continue;
      }

      //
      // Establish a connection.
      //

      KWStream newStream = null;
      try {
        try {
          newStream = tryConnect(user, database, info, socketFactory, hostSpec, sslMode);
        } catch (SQLException e) {
          if (sslMode == SslMode.PREFER
              && KSQLState.INVALID_AUTHORIZATION_SPECIFICATION.getState().equals(e.getSQLState())) {
            // Try non-SSL connection to cover case like "non-ssl only db"
            // Note: PREFER allows loss of encryption, so no significant harm is made
            Throwable ex = null;
            try {
              newStream =
                  tryConnect(user, database, info, socketFactory, hostSpec, SslMode.DISABLE);
              LOGGER.log(Level.FINE, "Downgraded to non-encrypted connection for host {0}",
                  hostSpec);
            } catch (SQLException ee) {
              ex = ee;
            } catch (IOException ee) {
              ex = ee; // Can't use multi-catch in Java 6 :(
            }
            if (ex != null) {
              log(Level.FINE, "sslMode==PREFER, however non-SSL connection failed as well", ex);
              // non-SSL failed as well, so re-throw original exception
              //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
              // Add non-SSL exception as suppressed
              e.addSuppressed(ex);
              //#endif
              throw e;
            }
          } else if (sslMode == SslMode.ALLOW
              && KSQLState.INVALID_AUTHORIZATION_SPECIFICATION.getState().equals(e.getSQLState())) {
            // Try using SSL
            Throwable ex = null;
            try {
              newStream =
                  tryConnect(user, database, info, socketFactory, hostSpec, SslMode.REQUIRE);
              LOGGER.log(Level.FINE, "Upgraded to encrypted connection for host {0}",
                  hostSpec);
            } catch (SQLException ee) {
              ex = ee;
            } catch (IOException ee) {
              ex = ee; // Can't use multi-catch in Java 6 :(
            }
            if (ex != null) {
              log(Level.FINE, "sslMode==ALLOW, however SSL connection failed as well", ex);
              // non-SSL failed as well, so re-throw original exception
              //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
              // Add SSL exception as suppressed
              e.addSuppressed(ex);
              //#endif
              throw e;
            }

          } else {
            throw e;
          }
        }

        int cancelSignalTimeout = KWProperty.CANCEL_SIGNAL_TIMEOUT.getInt(info) * 1000;

        // Do final startup.
        QueryExecutor queryExecutor = new QueryExecutorImpl(newStream, user, database,
            cancelSignalTimeout, info);

        // Check Master or Secondary
        HostStatus hostStatus = HostStatus.ConnectOK;
        if (candidateHost.targetServerType != HostRequirement.any) {
          hostStatus = isMaster(queryExecutor) ? HostStatus.Master : HostStatus.Secondary;
        }
        GlobalHostStatusTracker.reportHostStatus(hostSpec, hostStatus);
        knownStates.put(hostSpec, hostStatus);
        if (!candidateHost.targetServerType.allowConnectingTo(hostStatus)) {
          queryExecutor.close();
          continue;
        }

        runInitialQueries(queryExecutor, info);

        // And we're done.
        return queryExecutor;
      } catch (ConnectException cex) {
        // Added by Peter Mount <peter@retep.org.uk>
        // ConnectException is thrown when the connection cannot be made.
        // we trap this an return a more meaningful message for the end user
        GlobalHostStatusTracker.reportHostStatus(hostSpec, HostStatus.ConnectFail);
        knownStates.put(hostSpec, HostStatus.ConnectFail);
        if (hostIter.hasNext()) {
          log(Level.FINE, "ConnectException occurred while connecting to {0}", cex, hostSpec);
          // still more addresses to try
          continue;
        }
        throw new KSQLException(GT.tr(
            "Connection to {0} refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.",
            hostSpec), KSQLState.CONNECTION_UNABLE_TO_CONNECT, cex);
      } catch (IOException ioe) {
        closeStream(newStream);
        GlobalHostStatusTracker.reportHostStatus(hostSpec, HostStatus.ConnectFail);
        knownStates.put(hostSpec, HostStatus.ConnectFail);
        if (hostIter.hasNext()) {
          log(Level.FINE, "IOException occurred while connecting to {0}", ioe, hostSpec);
          // still more addresses to try
          continue;
        }
        throw new KSQLException(GT.tr("The connection attempt failed."),
            KSQLState.CONNECTION_UNABLE_TO_CONNECT, ioe);
      } catch (SQLException se) {
        closeStream(newStream);
        GlobalHostStatusTracker.reportHostStatus(hostSpec, HostStatus.ConnectFail);
        knownStates.put(hostSpec, HostStatus.ConnectFail);
        if (hostIter.hasNext()) {
          log(Level.FINE, "SQLException occurred while connecting to {0}", se, hostSpec);
          // still more addresses to try
          continue;
        }
        throw se;
      }
    }
    throw new KSQLException(GT
        .tr("Could not find a server with specified targetServerType: {0}", targetServerType),
        KSQLState.CONNECTION_UNABLE_TO_CONNECT);
  }

  private List<String[]> getParametersForStartup(String user, String database, Properties info) {
    List<String[]> paramList = new ArrayList<String[]>();
    paramList.add(new String[]{"user", user});
    paramList.add(new String[]{"database", database});
    paramList.add(new String[]{"client_encoding", "UTF8"});
    paramList.add(new String[]{"DateStyle", "ISO"});
    paramList.add(new String[]{"TimeZone", createPostgresTimeZone()});

    Version assumeVersion = ServerVersion.from(KWProperty.ASSUME_MIN_SERVER_VERSION.get(info));

    if (assumeVersion.getVersionNum() >= ServerVersion.v9_0.getVersionNum()) {
      // User is explicitly telling us this is a 9.0+ server so set properties here:
      paramList.add(new String[]{"extra_float_digits", "3"});
      String appName = KWProperty.APPLICATION_NAME.get(info);
      if (appName != null) {
        paramList.add(new String[]{"application_name", appName});
      }
    } else {
      // User has not explicitly told us that this is a 9.0+ server so stick to old default:
      paramList.add(new String[]{"extra_float_digits", "2"});
    }

    String replication = KWProperty.REPLICATION.get(info);
    if (replication != null && assumeVersion.getVersionNum() >= ServerVersion.v9_4.getVersionNum()) {
      paramList.add(new String[]{"replication", replication});
    }

    String currentSchema = KWProperty.CURRENT_SCHEMA.get(info);
    if (currentSchema != null) {
      paramList.add(new String[]{"search_path", currentSchema});
    }

    String options = KWProperty.OPTIONS.get(info);
    if (options != null) {
      paramList.add(new String[]{"options", options});
    }

//    // Expand the KWDB connection parameters.
//    String mode = KWProperty.KWDB_MODE.get(info);
//    if (mode != null) {
//      paramList.add(new String[]{"mode", mode});
//    }
//
//    String connId = KWProperty.KWDB_CONN_ID.get(info);
//    if (connId != null) {
//      paramList.add(new String[]{"conn_id", connId});
//    }
//
//    String tsUrlExpect = KWProperty.KWDB_TS_URL_EXPECT.get(info);
//    if (tsUrlExpect != null) {
//      paramList.add(new String[]{"ts_url_expect", tsUrlExpect});
//    }

    return paramList;
  }

  private static void log(Level level, String msg, Throwable thrown, Object... params) {
    if (!LOGGER.isLoggable(level)) {
      return;
    }
    LogRecord rec = new LogRecord(level, msg);
    // Set the loggerName of the LogRecord with the current logger
    rec.setLoggerName(LOGGER.getName());
    rec.setParameters(params);
    rec.setThrown(thrown);
    LOGGER.log(rec);
  }

  /**
   * Convert Java time zone to kaiwudb time zone. All others stay the same except that GMT+nn
   * changes to GMT-nn and vise versa.
   *
   * @return The current JVM time zone in kaiwudb format.
   */
  private static String createPostgresTimeZone() {
    String tz = TimeZone.getDefault().getID();
    if (tz.length() <= 3 || !tz.startsWith("GMT")) {
      return tz;
    }
    char sign = tz.charAt(3);
    String start;
    switch (sign) {
      case '+':
        start = "GMT-";
        break;
      case '-':
        start = "GMT+";
        break;
      default:
        // unknown type
        return tz;
    }

    return start + tz.substring(4);
  }

  private KWStream enableSSL(KWStream kwStream, SslMode sslMode, Properties info,
                             int connectTimeout)
      throws IOException, KSQLException {
    if (sslMode == SslMode.DISABLE) {
      return kwStream;
    }
    if (sslMode == SslMode.ALLOW) {
      // Allow ==> start with plaintext, use encryption if required by server
      return kwStream;
    }

    LOGGER.log(Level.FINEST, " FE=> SSLRequest");

    // Send SSL request packet
    kwStream.sendInteger4(8);
    kwStream.sendInteger2(1234);
    kwStream.sendInteger2(5679);
    kwStream.flush();

    // Now get the response from the backend, one of N, E, S.
    int beresp = kwStream.receiveChar();
    switch (beresp) {
      case 'E':
        LOGGER.log(Level.FINEST, " <=BE SSLError");

        // Server doesn't even know about the SSL handshake protocol
        if (sslMode.requireEncryption()) {
          throw new KSQLException(GT.tr("The server does not support SSL."),
              KSQLState.CONNECTION_REJECTED);
        }

        // We have to reconnect to continue.
        kwStream.close();
        return new KWStream(kwStream.getSocketFactory(), kwStream.getHostSpec(), connectTimeout);

      case 'N':
        LOGGER.log(Level.FINEST, " <=BE SSLRefused");

        // Server does not support ssl
        if (sslMode.requireEncryption()) {
          throw new KSQLException(GT.tr("The server does not support SSL."),
              KSQLState.CONNECTION_REJECTED);
        }

        return kwStream;

      case 'S':
        LOGGER.log(Level.FINEST, " <=BE SSLOk");

        // Server supports ssl
        MakeSSL.convert(kwStream, info);
        return kwStream;

      default:
        throw new KSQLException(GT.tr("An error occurred while setting up the SSL connection."),
            KSQLState.PROTOCOL_VIOLATION);
    }
  }

  private void sendStartupPacket(KWStream kwStream, List<String[]> params)
      throws IOException {
    if (LOGGER.isLoggable(Level.FINEST)) {
      StringBuilder details = new StringBuilder();
      for (int i = 0; i < params.size(); ++i) {
        if (i != 0) {
          details.append(", ");
        }
        details.append(params.get(i)[0]);
        details.append("=");
        details.append(params.get(i)[1]);
      }
      LOGGER.log(Level.FINEST, " FE=> StartupPacket({0})", details);
    }

    // Precalculate message length and encode params.
    int length = 4 + 4;
    byte[][] encodedParams = new byte[params.size() * 2][];
    for (int i = 0; i < params.size(); ++i) {
      encodedParams[i * 2] = params.get(i)[0].getBytes("UTF-8");
      encodedParams[i * 2 + 1] = params.get(i)[1].getBytes("UTF-8");
      length += encodedParams[i * 2].length + 1 + encodedParams[i * 2 + 1].length + 1;
    }

    length += 1; // Terminating \0

    // Send the startup message.
    kwStream.sendInteger4(length);
    kwStream.sendInteger2(3); // protocol major
    kwStream.sendInteger2(0); // protocol minor
    for (byte[] encodedParam : encodedParams) {
      kwStream.send(encodedParam);
      kwStream.sendChar(0);
    }

    kwStream.sendChar(0);
    kwStream.flush();
  }

  private void doAuthentication(KWStream kwStream, String host, String user, Properties info) throws IOException, SQLException {
    // Now get the response from the backend, either an error message
    // or an authentication request

    String password = KWProperty.PASSWORD.get(info);

    /* SSPI negotiation state, if used */
    ISSPIClient sspiClient = null;

    //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
    /* SCRAM authentication state, if used */
    ScramAuthenticator scramAuthenticator = null;
    //#endif

    try {
      authloop: while (true) {
        int beresp = kwStream.receiveChar();

        switch (beresp) {
          case 'E':
            // An error occurred, so pass the error message to the
            // user.
            //
            // The most common one to be thrown here is:
            // "User authentication failed"
            //
            int elen = kwStream.receiveInteger4();

            ServerErrorMessage errorMsg =
                new ServerErrorMessage(kwStream.receiveErrorString(elen - 4));
            LOGGER.log(Level.FINEST, " <=BE ErrorMessage({0})", errorMsg);
            throw new KSQLException(errorMsg, KWProperty.LOG_SERVER_ERROR_DETAIL.getBoolean(info));

          case 'R':
            // Authentication request.
            // Get the message length
            int msgLen = kwStream.receiveInteger4();

            // Get the type of request
            int areq = kwStream.receiveInteger4();

            // Process the request.
            switch (areq) {
              case AUTH_REQ_MD5: {
                byte[] md5Salt = kwStream.receive(4);
                if (LOGGER.isLoggable(Level.FINEST)) {
                  LOGGER.log(Level.FINEST, " <=BE AuthenticationReqMD5(salt={0})", Utils.toHexString(md5Salt));
                }

                if (password == null) {
                  throw new KSQLException(
                      GT.tr(
                          "The server requested password-based authentication, but no password was provided."),
                      KSQLState.CONNECTION_REJECTED);
                }

                byte[] digest =
                    MD5Digest.encode(user.getBytes("UTF-8"), password.getBytes("UTF-8"), md5Salt);

                if (LOGGER.isLoggable(Level.FINEST)) {
                  LOGGER.log(Level.FINEST, " FE=> Password(md5digest={0})", new String(digest, "US-ASCII"));
                }

                kwStream.sendChar('p');
                kwStream.sendInteger4(4 + digest.length + 1);
                kwStream.send(digest);
                kwStream.sendChar(0);
                kwStream.flush();

                break;
              }

              case AUTH_REQ_PASSWORD: {
                LOGGER.log(Level.FINEST, "<=BE AuthenticationReqPassword");
                LOGGER.log(Level.FINEST, " FE=> Password(password=<not shown>)");

                if (password == null) {
                  throw new KSQLException(
                      GT.tr(
                          "The server requested password-based authentication, but no password was provided."),
                      KSQLState.CONNECTION_REJECTED);
                }

                byte[] encodedPassword = password.getBytes("UTF-8");

                kwStream.sendChar('p');
                kwStream.sendInteger4(4 + encodedPassword.length + 1);
                kwStream.send(encodedPassword);
                kwStream.sendChar(0);
                kwStream.flush();

                break;
              }

              case AUTH_REQ_GSS:
              case AUTH_REQ_SSPI:
                /*
                 * Use GSSAPI if requested on all platforms, via JSSE.
                 *
                 * For SSPI auth requests, if we're on Windows attempt native SSPI authentication if
                 * available, and if not disabled by setting a kerberosServerName. On other
                 * platforms, attempt JSSE GSSAPI negotiation with the SSPI server.
                 *
                 * Note that this is slightly different to libpq, which uses SSPI for GSSAPI where
                 * supported. We prefer to use the existing Java JSSE Kerberos support rather than
                 * going to native (via JNA) calls where possible, so that JSSE system properties
                 * etc continue to work normally.
                 *
                 * Note that while SSPI is often Kerberos-based there's no guarantee it will be; it
                 * may be NTLM or anything else. If the client responds to an SSPI request via
                 * GSSAPI and the other end isn't using Kerberos for SSPI then authentication will
                 * fail.
                 */
                final String gsslib = KWProperty.GSS_LIB.get(info);
                final boolean usespnego = KWProperty.USE_SPNEGO.getBoolean(info);

                boolean useSSPI = false;

                /*
                 * Use SSPI if we're in auto mode on windows and have a request for SSPI auth, or if
                 * it's forced. Otherwise use gssapi. If the user has specified a Kerberos server
                 * name we'll always use JSSE GSSAPI.
                 */
                if (gsslib.equals("gssapi")) {
                  LOGGER.log(Level.FINE, "Using JSSE GSSAPI, param gsslib=gssapi");
                } else if (areq == AUTH_REQ_GSS && !gsslib.equals("sspi")) {
                  LOGGER.log(Level.FINE,
                      "Using JSSE GSSAPI, gssapi requested by server and gsslib=sspi not forced");
                } else {
                  /* Determine if SSPI is supported by the client */
                  sspiClient = createSSPI(kwStream, KWProperty.SSPI_SERVICE_CLASS.get(info),
                      /* Use negotiation for SSPI, or if explicitly requested for GSS */
                      areq == AUTH_REQ_SSPI || (areq == AUTH_REQ_GSS && usespnego));

                  useSSPI = sspiClient.isSSPISupported();
                  LOGGER.log(Level.FINE, "SSPI support detected: {0}", useSSPI);

                  if (!useSSPI) {
                    /* No need to dispose() if no SSPI used */
                    sspiClient = null;

                    if (gsslib.equals("sspi")) {
                      throw new KSQLException(
                          "SSPI forced with gsslib=sspi, but SSPI not available; set loglevel=2 for details",
                          KSQLState.CONNECTION_UNABLE_TO_CONNECT);
                    }
                  }

                  if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Using SSPI: {0}, gsslib={1} and SSPI support detected", new Object[]{useSSPI, gsslib});
                  }
                }

                if (useSSPI) {
                  /* SSPI requested and detected as available */
                  sspiClient.startSSPI();
                } else {
                  /* Use JGSS's GSSAPI for this request */
                  MakeGSS.authenticate(kwStream, host, user, password,
                      KWProperty.JAAS_APPLICATION_NAME.get(info),
                      KWProperty.KERBEROS_SERVER_NAME.get(info), usespnego,
                      KWProperty.JAAS_LOGIN.getBoolean(info),
                      KWProperty.LOG_SERVER_ERROR_DETAIL.getBoolean(info));
                }
                break;

              case AUTH_REQ_GSS_CONTINUE:
                /*
                 * Only called for SSPI, as GSS is handled by an inner loop in MakeGSS.
                 */
                sspiClient.continueSSPI(msgLen - 8);
                break;

              case AUTH_REQ_SASL:
                LOGGER.log(Level.FINEST, " <=BE AuthenticationSASL");

                //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
                scramAuthenticator = new ScramAuthenticator(user, password, kwStream);
                scramAuthenticator.processServerMechanismsAndInit();
                scramAuthenticator.sendScramClientFirstMessage();
                // This works as follows:
                // 1. When tests is run from IDE, it is assumed SCRAM library is on the classpath
                // 2. In regular build for Java < 8 this `if` is deactivated and the code always throws
                if (false) {
                  //#else
                  throw new KSQLException(GT.tr(
                          "SCRAM authentication is not supported by this driver. You need JDK >= 8 and kwjdbc >= 42.2.0 (not \".jre\" versions)",
                          areq), KSQLState.CONNECTION_REJECTED);
                  //#endif
                  //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
                }
                break;
                //#endif

              //#if mvn.project.property.postgresql.jdbc.spec >= "JDBC4.1"
              case AUTH_REQ_SASL_CONTINUE:
                scramAuthenticator.processServerFirstMessage(msgLen - 4 - 4);
                break;

              case AUTH_REQ_SASL_FINAL:
                scramAuthenticator.verifyServerSignature(msgLen - 4 - 4);
                break;
              //#endif

              case AUTH_REQ_OK:
                /* Cleanup after successful authentication */
                LOGGER.log(Level.FINEST, " <=BE AuthenticationOk");
                break authloop; // We're done.

              default:
                LOGGER.log(Level.FINEST, " <=BE AuthenticationReq (unsupported type {0})", areq);
                throw new KSQLException(GT.tr(
                    "The authentication type {0} is not supported. Check that you have configured the kw_hba.conf file to include the client''s IP address or subnet, and that it is using an authentication scheme supported by the driver.",
                    areq), KSQLState.CONNECTION_REJECTED);
            }

            break;

          default:
            throw new KSQLException(GT.tr("Protocol error.  Session setup failed."),
                KSQLState.PROTOCOL_VIOLATION);
        }
      }
    } finally {
      /* Cleanup after successful or failed authentication attempts */
      if (sspiClient != null) {
        try {
          sspiClient.dispose();
        } catch (RuntimeException ex) {
          LOGGER.log(Level.FINE, "Unexpected error during SSPI context disposal", ex);
        }

      }
    }

  }

  private void runInitialQueries(QueryExecutor queryExecutor, Properties info)
      throws SQLException {
    String assumeMinServerVersion = KWProperty.ASSUME_MIN_SERVER_VERSION.get(info);
    if (Utils.parseServerVersionStr(assumeMinServerVersion) >= ServerVersion.v9_0.getVersionNum()) {
      // We already sent the parameter values in the StartupMessage so skip this
      return;
    }

    final int dbVersion = queryExecutor.getServerVersionNum();

    if (dbVersion >= ServerVersion.v9_0.getVersionNum()) {
      SetupQueryRunner.run(queryExecutor, "SET extra_float_digits = 3", false);
    }

    String appName = KWProperty.APPLICATION_NAME.get(info);
    if (appName != null && dbVersion >= ServerVersion.v9_0.getVersionNum()) {
      StringBuilder sql = new StringBuilder();
      sql.append("SET application_name = '");
      Utils.escapeLiteral(sql, appName, queryExecutor.getStandardConformingStrings());
      sql.append("'");
      SetupQueryRunner.run(queryExecutor, sql.toString(), false);
    }

  }

  private boolean isMaster(QueryExecutor queryExecutor) throws SQLException, IOException {
    byte[][] results = SetupQueryRunner.run(queryExecutor, "show transaction_read_only", true);
    String value = queryExecutor.getEncoding().decode(results[0]);
    return value.equalsIgnoreCase("off");
  }
}
