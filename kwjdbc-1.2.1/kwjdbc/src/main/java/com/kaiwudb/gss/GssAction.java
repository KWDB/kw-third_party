/*
 * Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package com.kaiwudb.gss;

import com.kaiwudb.core.KWStream;
import com.kaiwudb.util.GT;
import com.kaiwudb.util.KSQLException;
import com.kaiwudb.util.KSQLState;
import com.kaiwudb.util.ServerErrorMessage;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

class GssAction implements PrivilegedAction<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GssAction.class.getName());
  private final KWStream kwStream;
  private final String host;
  private final String user;
  private final String kerberosServerName;
  private final boolean useSpnego;
  private final GSSCredential clientCredentials;
  private final boolean logServerErrorDetail;

  GssAction(KWStream kwStream, GSSCredential clientCredentials, String host, String user,
            String kerberosServerName, boolean useSpnego, boolean logServerErrorDetail) {
    this.kwStream = kwStream;
    this.clientCredentials = clientCredentials;
    this.host = host;
    this.user = user;
    this.kerberosServerName = kerberosServerName;
    this.useSpnego = useSpnego;
    this.logServerErrorDetail = logServerErrorDetail;
  }

  private static boolean hasSpnegoSupport(GSSManager manager) throws GSSException {
    org.ietf.jgss.Oid spnego = new org.ietf.jgss.Oid("1.3.6.1.5.5.2");
    org.ietf.jgss.Oid[] mechs = manager.getMechs();

    for (Oid mech : mechs) {
      if (mech.equals(spnego)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Exception run() {
    try {
      GSSManager manager = GSSManager.getInstance();
      GSSCredential clientCreds = null;
      Oid[] desiredMechs = new Oid[1];
      if (clientCredentials == null) {
        if (useSpnego && hasSpnegoSupport(manager)) {
          desiredMechs[0] = new Oid("1.3.6.1.5.5.2");
        } else {
          desiredMechs[0] = new Oid("1.2.840.113554.1.2.2");
        }
        GSSName clientName = manager.createName(user, GSSName.NT_USER_NAME);
        clientCreds = manager.createCredential(clientName, 8 * 3600, desiredMechs,
            GSSCredential.INITIATE_ONLY);
      } else {
        desiredMechs[0] = new Oid("1.2.840.113554.1.2.2");
        clientCreds = clientCredentials;
      }

      GSSName serverName =
          manager.createName(kerberosServerName + "@" + host, GSSName.NT_HOSTBASED_SERVICE);

      GSSContext secContext = manager.createContext(serverName, desiredMechs[0], clientCreds,
          GSSContext.DEFAULT_LIFETIME);
      secContext.requestMutualAuth(true);

      byte[] inToken = new byte[0];
      byte[] outToken = null;

      boolean established = false;
      while (!established) {
        outToken = secContext.initSecContext(inToken, 0, inToken.length);


        if (outToken != null) {
          LOGGER.log(Level.FINEST, " FE=> Password(GSS Authentication Token)");

          kwStream.sendChar('p');
          kwStream.sendInteger4(4 + outToken.length);
          kwStream.send(outToken);
          kwStream.flush();
        }

        if (!secContext.isEstablished()) {
          int response = kwStream.receiveChar();
          // Error
          switch (response) {
            case 'E':
              int elen = kwStream.receiveInteger4();
              ServerErrorMessage errorMsg
                  = new ServerErrorMessage(kwStream.receiveErrorString(elen - 4));

              LOGGER.log(Level.FINEST, " <=BE ErrorMessage({0})", errorMsg);

              return new KSQLException(errorMsg, logServerErrorDetail);
            case 'R':
              LOGGER.log(Level.FINEST, " <=BE AuthenticationGSSContinue");
              int len = kwStream.receiveInteger4();
              int type = kwStream.receiveInteger4();
              // should check type = 8
              inToken = kwStream.receive(len - 8);
              break;
            default:
              // Unknown/unexpected message type.
              return new KSQLException(GT.tr("Protocol error.  Session setup failed."),
                  KSQLState.CONNECTION_UNABLE_TO_CONNECT);
          }
        } else {
          established = true;
        }
      }

    } catch (IOException e) {
      return e;
    } catch (GSSException gsse) {
      return new KSQLException(GT.tr("GSS Authentication failed"), KSQLState.CONNECTION_FAILURE,
          gsse);
    }

    return null;
  }
}
