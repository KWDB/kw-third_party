// Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
/*-------------------------------------------------------------------------
 *
 * auth.c
 *	  Routines to handle network authentication
 *
 * Portions Copyright (c) 1996-2015, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  src/backend/libpq/auth.c
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"

#include <sys/param.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif

#include "libpq/auth.h"
#include "libpq/ip.h"
#include "libpq/libpq.h"
#include "libpq/pqformat.h"
#include "libpq/md5.h"
#include "libpq/hba.h"


/*----------------------------------------------------------------
 * Global authentication functions
 *----------------------------------------------------------------
 */
static int	recv_and_check_password_packet(Port *port);


/*
 * Maximum accepted size of GSS and SSPI authentication tokens.
 *
 * Kerberos tickets are usually quite small, but the TGTs issued by Windows
 * domain controllers include an authorization field known as the Privilege
 * Attribute Certificate (PAC), which contains the user's Windows permissions
 * (group memberships etc.). The PAC is copied into all tickets obtained on
 * the basis of this TGT (even those issued by Unix realms which the Windows
 * realm trusts), and can be several kB in size. The maximum token size
 * accepted by Windows systems is determined by the MaxAuthToken Windows
 * registry setting. Microsoft recommends that it is not set higher than
 * 65535 bytes, so that seems like a reasonable limit for us as well.
 */
#define PG_MAX_AUTH_TOKEN_LENGTH	65535


/*
 * Tell the user the authentication failed, but not (much about) why.
 *
 * There is a tradeoff here between security concerns and making life
 * unnecessarily difficult for legitimate users.  We would not, for example,
 * want to report the password we were expecting to receive...
 * But it seems useful to report the username and authorization method
 * in use, and these are items that must be presumed known to an attacker
 * anyway.
 * Note that many sorts of failure report additional information in the
 * postmaster log, which we hope is only readable by good guys.  In
 * particular, if logdetail isn't NULL, we send that string to the log.
 */
void
auth_failed(Port *port)
{
	const char *errstr;

	errstr ="password authentication failed for user %s";

	// TODO(SH): error

	/* doesn't return */
}


/*
 * Client authentication starts here.  If there is an error, this
 * function does not return and the backend process is terminated.
 */
int
ClientAuthentication(Port *port)
{
	int			status = STATUS_ERROR;

	status = sendAuthRequest(port, AUTH_REQ_PASSWORD);
	if (status != STATUS_OK) return STATUS_ERROR;
	status = recv_and_check_password_packet(port);

	if (status == STATUS_OK)
		return sendAuthRequest(port, AUTH_REQ_OK);
	else
		auth_failed(port);
	return STATUS_OK;
}


/*
 * Send an authentication request packet to the frontend.
 */
int
sendAuthRequest(Port *port, AuthRequest areq)
{
	StringInfoData buf;

	if (pq_beginmessage(&buf, 'R') != STATUS_OK) {
		return STATUS_ERROR;
	}
	if (pq_sendint(&buf, (int32) areq, sizeof(int32)) != STATUS_OK) {
		return STATUS_ERROR;
	}

	pq_endmessage(port, &buf);

	/*
	 * Flush message so client will see it, except for AUTH_REQ_OK, which need
	 * not be sent until we are ready for queries.
	 */
	if (areq != AUTH_REQ_OK)
		pq_flush(port);

	return STATUS_OK;
}

/*
 * Collect password response packet from frontend.
 *
 * Returns NULL if couldn't get password, else palloc'd string.
 */
char *
recv_password_packet(Port *port)
{
	StringInfoData buf;

	pq_startmsgread(port);
	if (PG_PROTOCOL_MAJOR(port->proto) >= 3)
	{
		/* Expect 'p' message type */
		int			mtype;

		mtype = pq_getbyte(port);
		if (mtype != 'p')
		{
			/*
			 * If the client just disconnects without offering a password,
			 * don't make a log entry.  This is legal per protocol spec and in
			 * fact commonly done by psql, so complaining just clutters the
			 * log.
			 */
			if (mtype != EOF)
			{
				// TODO(SH): error
			}
				
			printf("EOF or bad message type\n");
			return NULL;		/* EOF or bad message type */
		}
	}
	else
	{
		/* For pre-3.0 clients, avoid log entry if they just disconnect */
		if (pq_peekbyte(port) == EOF) {
			printf("EOF \n");
			return NULL;		/* EOF */
		}
	}

	if (initStringInfo(&buf) != STATUS_OK) {
		printf("EOF2 \n");
		return NULL;		/* EOF */
	}
	if (pq_getmessage(port, &buf, 1000))		/* receive password */
	{
		/* EOF - pq_getmessage already logged a suitable message */
		// free(buf.data);
		printf("EOF3 \n");
		return NULL;
	}

	/*
	 * Apply sanity check: password packet length should agree with length of
	 * contained string.  Note it is safe to use strlen here because
	 * StringInfo is guaranteed to have an appended '\0'.
	 */
	if (strlen(buf.data) + 1 != buf.len)
	{
		// TODO(SH): error
	}

	/*
	 * Don't allow an empty password. Libpq treats an empty password the same
	 * as no password at all, and won't even try to authenticate. But other
	 * clients might, so allowing it would be confusing.
	 *
	 * Note that this only catches an empty password sent by the client in
	 * plaintext. There's another check in md5_crypt_verify to prevent an
	 * empty password from being used with MD5 authentication.
	 */
	if (buf.data[0] == '\0')
	{
		// TODO(SH): error
	}

	/* Do not echo password to logs, for security. */
	// TODO(SH): log
	// elog(DEBUG5, "received password packet");

	/*
	 * Return the received string.  Note we do not attempt to do any
	 * character-set conversion on it; since we don't yet know the client's
	 * encoding, there wouldn't be much point.
	 */
	// TODO(jiayi): member outlives its object.
	char* t = buf.data;
	buf.data = NULL;
	return t;
}


/*----------------------------------------------------------------
 * MD5 authentication
 *----------------------------------------------------------------
 */

/*
 * Called when we have sent an authorization request for a password.
 * Get the response and check it.
 * On error, optionally store a detail string at *logdetail.
 */
static int
recv_and_check_password_packet(Port *port)
{
	char	   *passwd;
	int			result;

	passwd = recv_password_packet(port);

	if (passwd == NULL)
		return STATUS_EOF;		/* client wouldn't send password */

	// TODO(SH): check password
	result = STATUS_OK;

	free(passwd);

	return result;
}

/*----------------------------------------------------------------
 * SSL client certificate authentication
 *----------------------------------------------------------------
 */
#ifdef USE_SSL
int CheckCertAuth(Port *port)
{
	Assert(port->ssl);

	/* Make sure we have received a username in the certificate */
	if (port->peer_cn == NULL ||
		strlen(port->peer_cn) <= 0)
	{
		// TODO(sh): error
		// ereport(LOG,
		// 		(errmsg("certificate authentication failed for user \"%s\": client certificate contains no user name",
		// 				port->user_name)));
		return STATUS_ERROR;
	}

	/* Just pass the certificate CN to the usermap check */
	return check_usermap(NULL, port->user_name, port->peer_cn, false);
}
#endif