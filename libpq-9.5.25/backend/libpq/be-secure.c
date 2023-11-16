// Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
/*-------------------------------------------------------------------------
 *
 * be-secure.c
 *	  functions related to setting up a secure connection to the frontend.
 *	  Secure connections are expected to provide confidentiality,
 *	  message integrity and endpoint authentication.
 *
 *
 * Portions Copyright (c) 1996-2015, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  src/backend/libpq/be-secure.c
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"

#include <sys/stat.h>
#include <signal.h>
#include <fcntl.h>
#include <ctype.h>
#include <sys/socket.h>
#include <unistd.h>
#include <time.h>
#include <netdb.h>
#include <netinet/in.h>
#ifdef HAVE_NETINET_TCP_H
#include <netinet/tcp.h>
#include <arpa/inet.h>
#endif
#ifdef HAVE_POLL_H
#include <poll.h>
#endif
#ifdef HAVE_SYS_POLL_H
#include <sys/poll.h>
#endif
#ifdef HAVE_SYS_SELECT_H
#include <sys/select.h>
#endif

#include "libpq/libpq.h"

char	   *ssl_cert_file;
char	   *ssl_key_file;
char	   *ssl_ca_file;
char	   *ssl_crl_file;

#ifdef USE_SSL
bool		ssl_loaded_verify_locations = false;
#endif

/* GUC variable controlling SSL cipher list */
char	   *SSLCipherSuites = NULL;

/* GUC variable for default ECHD curve. */
char	   *SSLECDHCurve;

/* GUC variable: if false, prefer client ciphers */
bool		SSLPreferServerCiphers;

/* ------------------------------------------------------------ */
/*			 Procedures include to all secure sessions			*/
/* ------------------------------------------------------------ */

void initSecure(const char *sslCertFile, const char *sslKeyFile, const char *sslCaFile,
               const char *sslCiphers, const char *sslEcdhCurve, bool sslPreferServerCiphers) {
  ssl_cert_file = sslCertFile;
  ssl_key_file = sslKeyFile;
  ssl_ca_file = sslCaFile;
  ssl_crl_file = "";
  SSLCipherSuites = sslCiphers;
  SSLECDHCurve = sslEcdhCurve;
  SSLPreferServerCiphers = sslPreferServerCiphers;
  secure_initialize();
}

/*
 *	Initialize global context
 */
int
secure_initialize(void)
{
#ifdef USE_SSL
  be_tls_init();
#endif

  return 0;
}

/*
 * Indicate if we have loaded the root CA store to verify certificates
 */
bool
secure_loaded_verify_locations(void)
{
#ifdef USE_SSL
  return ssl_loaded_verify_locations;
#else
  return false;
#endif
}

/*
 *	Attempt to negotiate secure session.
 */
int
secure_open_server(Port *port)
{
  int			r = 0;

#ifdef USE_SSL
  r = be_tls_open_server(port);
#endif

  return r;
}

/*
 *	Close secure session.
 */
void
secure_close(Port *port)
{
#ifdef USE_SSL
  if (port  && port->ssl_in_use)
		be_tls_close(port);
#endif
}


/*
 * Check a file descriptor for read and/or write data, possibly waiting.
 * If neither forRead nor forWrite are set, immediately return a timeout
 * condition (without waiting).  Return >0 if condition is met, 0
 * if a timeout occurred, -1 if an error or interrupt occurred.
 *
 * Timeout is infinite if timeout_ms is -1.  Timeout is immediate (no blocking)
 * if timeout_ms is 0 (or indeed, any time before now).
 */
static int
pqSocketPoll(int sock, int forRead, int forWrite, int timeout_ms)
{
	/* We use poll(2) if available, otherwise select(2) */
#ifdef HAVE_POLL
	struct pollfd input_fd;

	if (!forRead && !forWrite)
		return 0;

	input_fd.fd = sock;
	input_fd.events = POLLERR;
	input_fd.revents = 0;

	if (forRead)
		input_fd.events |= POLLIN;
	if (forWrite)
		input_fd.events |= POLLOUT;

	return poll(&input_fd, 1, timeout_ms);
#else							/* !HAVE_POLL */

	fd_set		input_mask;
	fd_set		output_mask;
	fd_set		except_mask;
	struct timeval timeout;
	struct timeval *ptr_timeout;

	if (!forRead && !forWrite)
		return 0;

	FD_ZERO(&input_mask);
	FD_ZERO(&output_mask);
	FD_ZERO(&except_mask);
	if (forRead)
		FD_SET(sock, &input_mask);

	if (forWrite)
		FD_SET(sock, &output_mask);
	FD_SET(sock, &except_mask);

	/* Compute appropriate timeout interval */
	if (timeout_ms == ((time_t) -1))
		ptr_timeout = NULL;
	else
	{
		timeout.tv_sec = timeout_ms / 1000L;
		timeout.tv_usec = (timeout_ms % 1000L) * 1000L;
		ptr_timeout = &timeout;
	}

	return select(sock + 1, &input_mask, &output_mask,
				  &except_mask, ptr_timeout);
#endif   /* HAVE_POLL */
}

/*
 * Checks a socket, using poll or select, for data to be read, written,
 * or both.  Returns >0 if one or more conditions are met, 0 if it timed
 * out, -1 if an error occurred.
 *
 * If SSL is in use, the SSL buffer is checked prior to checking the socket
 * for read data directly.
 */
int pq_readready(Port *port, int timeout_ms) {
	if (!port) {
		return -1;
  }
	if (port->sock == PGINVALID_SOCKET) {
		return -1;
	}
#ifdef USE_SSL
	/* Check for SSL library buffering read bytes */
	if (port->ssl_in_use && be_tls_read_pending(port)) {
		/* short-circuit the select */
		return 1;
	}
#endif
  return pqSocketPoll(port->sock, 1, 0, timeout_ms);
}

/*
 *	Read data from a secure connection.
 */
ssize_t
secure_read(Port *port, void *ptr, size_t len)
{
  ssize_t		n;
  int			waitfor;

retry:
#ifdef USE_SSL
  waitfor = 0;
  if (port && port->ssl_in_use)
  {
    n = be_tls_read(port, ptr, len, &waitfor);
  }
  else
#endif
  {
    n = secure_raw_read(port, ptr, len);
  }

  /* In blocking mode, wait until the socket is ready */
  if (n < 0 && !port->noblock && (errno == EWOULDBLOCK || errno == EAGAIN))
  {
    int ret = pqSocketPoll(port->sock, 1, 0, SOCKET_CHECK_TIMEOUT_MS);
    /*
      * We'll retry the read. Most likely it will return immediately
      * because there's still no data available, and we'll wait for the
      * socket to become ready again.
      */
    goto retry;
  }

  return n;
}

ssize_t
secure_raw_read(Port *port, void *ptr, size_t len)
{
	ssize_t		n;

	/*
	 * Try to read from the socket without blocking. If it succeeds we're
	 * done, otherwise we'll wait for the socket using the latch mechanism.
	 */
	n = recv(port->sock, ptr, len, 0);

	return n;
}


/*
 *	Write data to a secure connection.
 */
ssize_t
secure_write(Port *port, void *ptr, size_t len)
{
  ssize_t		n;
  int			waitfor;

retry:
  waitfor = 0;
#ifdef USE_SSL
  if (port->ssl_in_use)
  {
    n = be_tls_write(port, ptr, len, &waitfor);
  }
  else
#endif
  {
    n = secure_raw_write(port, ptr, len);
  }

  if (n < 0 && !port->noblock && (errno == EWOULDBLOCK || errno == EAGAIN))
  {
    int ret = pqSocketPoll(port->sock, 0, 1, SOCKET_CHECK_TIMEOUT_MS);
    /*
      * We'll retry the write. Most likely it will return immediately
      * because there's still no buffer space available, and we'll wait
      * for the socket to become ready again.
      */
    goto retry;
  }

  return n;
}

ssize_t
secure_raw_write(Port *port, const void *ptr, size_t len)
{
	ssize_t		n;

	n = send(port->sock, ptr, len, MSG_NOSIGNAL);

	return n;
}
