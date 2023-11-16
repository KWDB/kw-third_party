<img height="90" alt="Slonik Duke" align="right" src="docs/media/img/slonik_duke.png" />

# KaiwuDB JDBC Driver

KaiwuDB JDBC Driver (KwJDBC for short) allows Java programs to connect to a KaiwuDB database using standard, database independent Java code. Is an open source JDBC driver written in Pure Java (Type 4).

## Supported KaiwuDB and Java versions
The current version of the driver should be compatible with **KaiwuDB 8.2 and higher** using the version 3.0 of the protocol, and **Java 6** (JDBC 4.0), **Java 7** (JDBC 4.1) and **Java 8** (JDBC 4.2). Unless you have unusual requirements (running old applications or JVMs), this is the driver you should be using.

KwJDBC regression tests are run against all KaiwuDB versions since 8.2, including "build KaiwuDB from git master" version. There are other derived forks of KaiwuDB but have not been certified to run with KwJDBC. If you find a bug or regression on supported versions, please fill an [Issue](https://github.com/pgjdbc/pgjdbc/issues).

## Get the Driver
Most people do not need to compile KwJDBC. You can download the precompiled driver (jar) from the KaiwuDB JDBC site.

----------------------------------------------------

### Driver and DataSource class

| Implements                          | Class                                          |
| ----------------------------------- | ---------------------------------------------- |
| java.sql.Driver                     | **com.kaiwudb.Driver**                      |
| javax.sql.DataSource                | com.kaiwudb.ds.KWSimpleDataSource           |
| javax.sql.ConnectionPoolDataSource  | com.kaiwudb.ds.KWConnectionPoolDataSource   |
| javax.sql.XADataSource              | com.kaiwudb.xa.KWXADataSource               |

### Building the Connection URL
The driver recognises JDBC URLs of the form:
```
jdbc:kaiwudb:database
jdbc:kaiwudb:
jdbc:kaiwudb://host/database
jdbc:kaiwudb://host/
jdbc:kaiwudb://host:port/database
jdbc:kaiwudb://host:port/
```
The general format for a JDBC URL for connecting to a KaiwuDB server is as follows, with items in square brackets ([ ]) being optional:
```
jdbc:kaiwudb:[//host[:port]/][database][?property1=value1[&property2=value2]...]
```
where:
 * **jdbc:kaiwudb:** (Required) is known as the sub-protocol and is constant.
 * **host** (Optional) is the server address to connect. This could be a DNS or IP address, or it could be *localhost* or *127.0.0.1* for the local computer. Defaults to `localhost`.
 * **port** (Optional) is the port number listening on the host. Defaults to `26257`.
 * **database** (Optional) is the database name. Defaults to the same name as the *user name* used in the connection.
 * **propertyX** (Optional) is one or more option connection properties. For more information see *Connection properties*.

#### Connection Properties
In addition to the standard connection parameters the driver supports a number of additional properties which can be used to specify additional driver behaviour specific to KaiwuDB™. These properties may be specified in either the connection URL or an additional Properties object parameter to DriverManager.getConnection.

| Property                      | Type    | Default | Description   |
| ----------------------------- | ------- | :-----: | ------------- |
| user                          | String  | null    | The database user on whose behalf the connection is being made. |
| password                      | String  | null    | The database user's password. |
| options                       | String  | null    | Specify 'options' connection initialization parameter. |
| ssl                           | Boolean | false   | Control use of SSL (true value causes SSL to be required) |
| sslfactory                    | String  | null    | Provide a SSLSocketFactory class when using SSL. |
| sslfactoryarg (deprecated)    | String  | null    | Argument forwarded to constructor of SSLSocketFactory class. |
| sslmode                       | String  | null    | Parameter governing the use of SSL. |
| sslcert                       | String  | null    | The location of the client's SSL certificate |
| sslkey                        | String  | null    | The location of the client's PKCS#8 SSL key |
| sslrootcert                   | String  | null    | The location of the root certificate for authenticating the server. |
| sslhostnameverifier           | String  | null    | The name of a class (for use in [Class.forName(String)](https://docs.oracle.com/javase/6/docs/api/java/lang/Class.html#forName%28java.lang.String%29)) that implements javax.net.ssl.HostnameVerifier and can verify the server hostname. |
| sslpasswordcallback           | String  | null    | The name of a class (for use in [Class.forName(String)](https://docs.oracle.com/javase/6/docs/api/java/lang/Class.html#forName%28java.lang.String%29)) that implements javax.security.auth.callback.CallbackHandler and can handle PasswordCallback for the ssl password. |
| sslpassword                   | String  | null    | The password for the client's ssl key (ignored if sslpasswordcallback is set) |
| sendBufferSize                | Integer | -1      | Socket write buffer size |
| recvBufferSize                | Integer | -1      | Socket read buffer size  |
| loggerLevel                   | String  | null    | Logger level of the driver using java.util.logging. Allowed values: OFF, DEBUG or TRACE. |
| loggerFile                    | String  | null    | File name output of the Logger, if set, the Logger will use a FileHandler to write to a specified file. If the parameter is not set or the file can't be created the ConsoleHandler will be used instead. |
| allowEncodingChanges          | Boolean | false   | Allow for changes in client_encoding |
| logUnclosedConnections        | Boolean | false   | When connections that are not explicitly closed are garbage collected, log the stacktrace from the opening of the connection to trace the leak source |
| binaryTransferEnable          | String  | ""      | Comma separated list of types to enable binary transfer. Either OID numbers or names |
| binaryTransferDisable         | String  | ""      | Comma separated list of types to disable binary transfer. Either OID numbers or names. Overrides values in the driver default set and values set with binaryTransferEnable. |
| prepareThreshold              | Integer | 5       | Statement prepare threshold. A value of -1 stands for forceBinary |
| preparedStatementCacheQueries | Integer | 256     | Specifies the maximum number of entries in per-connection cache of prepared statements. A value of 0 disables the cache. |
| preparedStatementCacheSizeMiB | Integer | 5       | Specifies the maximum size (in megabytes) of a per-connection prepared statement cache. A value of 0 disables the cache. |
| defaultRowFetchSize           | Integer | 0       | Positive number of rows that should be fetched from the database when more rows are needed for ResultSet by each fetch iteration |
| loginTimeout                  | Integer | 0       | Specify how long to wait for establishment of a database connection.|
| connectTimeout                | Integer | 10      | The timeout value used for socket connect operations. |
| socketTimeout                 | Integer | 0       | The timeout value used for socket read operations. |
| tcpKeepAlive                  | Boolean | false   | Enable or disable TCP keep-alive. |
| ApplicationName               | String  | null    | The application name (require server version >= 9.0) |
| readOnly                      | Boolean | true    | Puts this connection in read-only mode |
| disableColumnSanitiser        | Boolean | false   | Enable optimization that disables column name sanitiser |
| assumeMinServerVersion        | String  | null    | Assume the server is at least that version |
| currentSchema                 | String  | null    | Specify the schema (or several schema separated by commas) to be set in the search-path |
| targetServerType              | String  | any     | Specifies what kind of server to connect, possible values: any, master, slave (deprecated), secondary, preferSlave (deprecated), preferSecondary |
| hostRecheckSeconds            | Integer | 10      | Specifies period (seconds) after which the host status is checked again in case it has changed |
| loadBalanceHosts              | Boolean | false   | If disabled hosts are connected in the given order. If enabled hosts are chosen randomly from the set of suitable candidates |
| socketFactory                 | String  | null    | Specify a socket factory for socket creation |
| socketFactoryArg (deprecated) | String  | null    | Argument forwarded to constructor of SocketFactory class. |
| autosave                      | String  | never   | Specifies what the driver should do if a query fails, possible values: always, never, conservative |
| cleanupSavepoints             | Boolean | false   | In Autosave mode the driver sets a SAVEPOINT for every query. It is possible to exhaust the server shared buffers. Setting this to true will release each SAVEPOINT at the cost of an additional round trip. |
| preferQueryMode               | String  | extended | Specifies which mode is used to execute queries to database, possible values: extended, extendedForPrepared, extendedCacheEverything, simple |
| reWriteBatchedInserts         | Boolean | false  | Enable optimization to rewrite and collapse compatible INSERT statements that are batched. |
| escapeSyntaxCallMode          | String  | select   | Specifies how JDBC escape call syntax is transformed into underlying SQL (CALL/SELECT), for invoking procedures or functions (requires server version >= 11), possible values: select, callIfNoReturn, call |
