// Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
/*-------------------------------------------------------------------------
 *
 * pqcomm.c
 *	  Communication functions between the Frontend and the Backend
 *
 * These routines handle the low-level details of communication between
 * frontend and backend.  They just shove data across the communication
 * channel, and are ignorant of the semantics of the data --- or would be,
 * except for major brain damage in the design of the old COPY OUT protocol.
 * Unfortunately, COPY OUT was designed to commandeer the communication
 * channel (it just transfers data without wrapping it into messages).
 * No other messages can be sent while COPY OUT is in progress; and if the
 * copy is aborted by an ereport(ERROR), we need to close out the copy so that
 * the frontend gets back into sync.  Therefore, these routines have to be
 * aware of COPY OUT state.  (New COPY-OUT is message-based and does *not*
 * set the DoingCopyOut flag.)
 *
 * NOTE: generally, it's a bad idea to emit outgoing messages directly with
 * pq_putbytes(), especially if the message would require multiple calls
 * to send.  Instead, use the routines in pqformat.c to construct the message
 * in a buffer and then emit it in one call to pq_putmessage.  This ensures
 * that the channel will not be clogged by an incomplete message if execution
 * is aborted by ereport(ERROR) partway through the message.  The only
 * non-libpq code that should call pq_putbytes directly is old-style COPY OUT.
 *
 * At one time, libpq was shared between frontend and backend, but now
 * the backend's "backend/libpq" is quite separate from "interfaces/libpq".
 * All that remains is similarities of names to trap the unwary...
 *
 * Portions Copyright (c) 1996-2015, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *	src/backend/libpq/pqcomm.c
 *
 *-------------------------------------------------------------------------
 */

/*------------------------
 * INTERFACE ROUTINES
 *
 * setup/teardown:
 *		StreamServerPort	- Open postmaster's server port
 *		StreamConnection	- Create new connection with client
 *		StreamClose			- Close a client/backend connection
 *		TouchSocketFiles	- Protect socket files against /tmp cleaners
 *		pq_init			- initialize libpq at backend startup
 *		pq_comm_reset	- reset libpq during error recovery
 *		pq_close		- shutdown libpq at backend exit
 *
 * low-level I/O:
 *		pq_getbytes		- get a known number of bytes from connection
 *		pq_getstring	- get a null terminated string from connection
 *		pq_getmessage	- get a message with length word from connection
 *		pq_getbyte		- get next byte from connection
 *		pq_peekbyte		- peek at next byte from connection
 *		pq_putbytes		- send bytes to connection (not flushed until pq_flush)
 *		pq_flush		- flush pending output
 *		pq_flush_if_writable - flush pending output if writable without blocking
 *		pq_getbyte_if_available - get a byte if available without blocking
 *
 * message-level I/O (and old-style-COPY-OUT cruft):
 *		pq_putmessage	- send a normal message (suppressed in COPY OUT mode)
 *		pq_putmessage_noblock - buffer a normal message (suppressed in COPY OUT)
 *		pq_startcopyout - inform libpq that a COPY OUT transfer is beginning
 *		pq_endcopyout	- end a COPY OUT transfer
 *
 *------------------------
 */
#include "postgres.h"

#include <signal.h>
#include <fcntl.h>
#include <grp.h>
#include <unistd.h>
#include <sys/file.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <netdb.h>
#include <netinet/in.h>
#ifdef HAVE_NETINET_TCP_H
#include <netinet/tcp.h>
#endif
#include <arpa/inet.h>
#ifdef HAVE_UTIME_H
#include <utime.h>
#endif

#include "port.h"
#include "libpq/ip.h"
#include "libpq/libpq.h"

/*
 * Cope with the various platform-specific ways to spell TCP keepalive socket
 * options.  This doesn't cover Windows, which as usual does its own thing.
 */
#if defined(TCP_KEEPIDLE)
/* TCP_KEEPIDLE is the name of this option on Linux and *BSD */
#define PG_TCP_KEEPALIVE_IDLE TCP_KEEPIDLE
#define PG_TCP_KEEPALIVE_IDLE_STR "TCP_KEEPIDLE"
#elif defined(TCP_KEEPALIVE_THRESHOLD)
/* TCP_KEEPALIVE_THRESHOLD is the name of this option on Solaris >= 11 */
#define PG_TCP_KEEPALIVE_IDLE TCP_KEEPALIVE_THRESHOLD
#define PG_TCP_KEEPALIVE_IDLE_STR "TCP_KEEPALIVE_THRESHOLD"
#elif defined(TCP_KEEPALIVE) && defined(__darwin__)
/* TCP_KEEPALIVE is the name of this option on macOS */
/* Caution: Solaris has this symbol but it means something different */
#define PG_TCP_KEEPALIVE_IDLE TCP_KEEPALIVE
#define PG_TCP_KEEPALIVE_IDLE_STR "TCP_KEEPALIVE"
#endif

/* Internal functions */
static void socket_comm_reset(Port *port);
static void socket_close(Port *port, int code);
static void socket_set_nonblocking(Port *port, bool nonblocking);
static int	socket_flush(Port *port);
static int	socket_flush_if_writable(Port *port);
static bool socket_is_send_pending(Port *port);
static int	socket_putmessage(Port *port, char msgtype, const char *s, size_t len);
static void socket_putmessage_noblock(Port *port, char msgtype, const char *s, size_t len);
static void socket_startcopyout(Port *port);
static void socket_endcopyout(Port *port, bool errorAbort);
static int	internal_putbytes(Port *port, const char *s, size_t len);
static int	internal_flush(Port *port);
static void socket_set_nonblocking(Port *port, bool nonblocking);

#ifdef HAVE_UNIX_SOCKETS
static int	Lock_AF_UNIX(char *unixSocketDir, char *unixSocketPath);
static int	Setup_AF_UNIX(char *sock_path);
#endif   /* HAVE_UNIX_SOCKETS */

static PQcommMethods PqCommSocketMethods = {
	socket_comm_reset,
	socket_flush,
	socket_flush_if_writable,
	socket_is_send_pending,
	socket_putmessage,
	socket_putmessage_noblock,
    socket_startcopyout,
    socket_endcopyout
};

PQcommMethods *PqCommMethods = &PqCommSocketMethods;



/* --------------------------------
 *		pq_init - initialize libpq at backend startup
 * --------------------------------
 */
int pq_init(Port *port)
{
	/* initialize state variables */
	if (port->PqSendBuffer == NULL) {
		return STATUS_ERROR;
	}
	port->PqSendPointer = port->PqSendStart = port->PqRecvPointer = port->PqRecvLength = 0;
	port->PqCommBusy = false;
	port->PqCommReadingMsg = false;
	port->DoingCopyOut = false;
	if (!pg_set_noblock(port->sock)) {
		return STATUS_ERROR;
	}
	return STATUS_OK;
}

/* --------------------------------
 *		socket_comm_reset - reset libpq during error recovery
 *
 * This is called from error recovery at the outer idle loop.  It's
 * just to get us out of trouble if we somehow manage to elog() from
 * inside a pqcomm.c routine (which ideally will never happen, but...)
 * --------------------------------
 */
static void
socket_comm_reset(Port *port)
{
	/* Do not throw away pending data, but do reset the busy flag */
	port->PqCommBusy = false;

}

/* --------------------------------
 *		socket_close - shutdown libpq at backend exit
 *
 * This is the one pg_on_exit_callback in place during BackendInitialize().
 * That function's unusual signal handling constrains that this callback be
 * safe to run at any instant.
 * --------------------------------
 */
static void
socket_close(Port *port, int code)
{
	/* Nothing to do in a standalone backend, where MyProcPort is NULL. */
	if (port != NULL)
	{
		/*
		 * Formerly we did an explicit close() here, but it seems better to
		 * leave the socket open until the process dies.  This allows clients
		 * to perform a "synchronous close" if they care --- wait till the
		 * transport layer reports connection closure, and you can be sure the
		 * backend has exited.
		 *
		 * We do set sock to PGINVALID_SOCKET to prevent any further I/O,
		 * though.
		 */
		port->sock = PGINVALID_SOCKET;
	}
}


/* --------------------------------
 * Low-level I/O routines begin here.
 *
 * These routines communicate with a frontend client across a connection
 * already established by the preceding routines.
 * --------------------------------
 */

/* --------------------------------
 *			  socket_set_nonblocking - set socket blocking/non-blocking
 *
 * Sets the socket non-blocking if nonblocking is TRUE, or sets it
 * blocking otherwise.
 * --------------------------------
 */
static void
socket_set_nonblocking(Port *port, bool nonblocking)
{
	if (port == NULL)
	{
	  	// TODO(SH): error
	}

	port->noblock = nonblocking;
}

/* --------------------------------
 *		pq_recvbuf - load some bytes into the input buffer
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
static int
pq_recvbuf(Port *port)
{
	if (port->PqRecvPointer > 0)
	{
		if (port->PqRecvLength > port->PqRecvPointer)
		{
			/* still some unread data, left-justify it in the buffer */
			memmove(port->PqRecvBuffer, port->PqRecvBuffer + port->PqRecvPointer,
					port->PqRecvLength - port->PqRecvPointer);
			port->PqRecvLength -= port->PqRecvPointer;
			port->PqRecvPointer = 0;
		}
		else
			port->PqRecvLength = port->PqRecvPointer = 0;
	}

	/* Ensure that we're in blocking mode */
	socket_set_nonblocking(port, false);

	/* Can fill buffer from PqRecvLength and upwards */
	for (;;)
	{
		int			r;

		r = secure_read(port, port->PqRecvBuffer + port->PqRecvLength,
						PQ_RECV_BUFFER_SIZE - port->PqRecvLength);

		if (r < 0)
		{
			if (errno == EINTR)
				continue;		/* Ok if interrupted */

			/*
			 * Careful: an ereport() that tries to write to the client would
			 * cause recursion to here, leading to stack overflow and core
			 * dump!  This message must go *only* to the postmaster log.
			 */
			// TODO(SH): error
			return EOF;
		}
		if (r == 0)
		{
			/*
			 * EOF detected.  We used to write a log message here, but it's
			 * better to expect the ultimate caller to do that.
			 */
			return EOF;
		}
		/* r contains number of bytes read, so just incr length */
		port->PqRecvLength += r;
		return 0;
	}
}

/* --------------------------------
 *		pq_getbyte	- get a single byte from connection, or return EOF
 * --------------------------------
 */
int
pq_getbyte(Port *port)
{
	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	while (port->PqRecvPointer >= port->PqRecvLength)
	{
		if (pq_recvbuf(port))		/* If nothing in buffer, then recv some */
			return EOF;			/* Failed to recv data */
	}
	return (unsigned char) port->PqRecvBuffer[port->PqRecvPointer++];
}

/* --------------------------------
 *		pq_peekbyte		- peek at next byte from connection
 *
 *	 Same as pq_getbyte() except we don't advance the pointer.
 * --------------------------------
 */
int
pq_peekbyte(Port *port)
{
	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	while (port->PqRecvPointer >= port->PqRecvLength)
	{
		if (pq_recvbuf(port))		/* If nothing in buffer, then recv some */
			return EOF;			/* Failed to recv data */
	}
	return (unsigned char) port->PqRecvBuffer[port->PqRecvPointer];
}

/* --------------------------------
 *		pq_getbyte_if_available - get a single byte from connection,
 *			if available
 *
 * The received byte is stored in *c. Returns 1 if a byte was read,
 * 0 if no data was available, or EOF if trouble.
 * --------------------------------
 */
int
pq_getbyte_if_available(Port *port, unsigned char *c)
{
	int			r;

	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	if (port->PqRecvPointer < port->PqRecvLength)
	{
		*c = port->PqRecvBuffer[port->PqRecvPointer++];
		return 1;
	}

	/* Put the socket into non-blocking mode */
	socket_set_nonblocking(port, true);

	r = secure_read(port, c, 1);
	if (r < 0)
	{
		/*
		 * Ok if no data available without blocking or interrupted (though
		 * EINTR really shouldn't happen with a non-blocking socket). Report
		 * other errors.
		 */
		if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR)
			r = 0;
		else
		{
			/*
			 * Careful: an ereport() that tries to write to the client would
			 * cause recursion to here, leading to stack overflow and core
			 * dump!  This message must go *only* to the postmaster log.
			 */
			// TODO(SH): error
			r = EOF;
		}
	}
	else if (r == 0)
	{
		/* EOF detected */
		r = EOF;
	}

	return r;
}

/* --------------------------------
 *		pq_getbytes		- get a known number of bytes from connection
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
int
pq_getbytes(Port *port, char *s, size_t len)
{
	size_t		amount;

	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	while (len > 0)
	{
		while (port->PqRecvPointer >= port->PqRecvLength)
		{
			if (pq_recvbuf(port))	/* If nothing in buffer, then recv some */
				return EOF;		/* Failed to recv data */
		}
		amount = port->PqRecvLength - port->PqRecvPointer;
		if (amount > len)
			amount = len;
		memcpy(s, port->PqRecvBuffer + port->PqRecvPointer, amount);
		port->PqRecvPointer += amount;
		s += amount;
		len -= amount;
	}
	return 0;
}

/* --------------------------------
 *		pq_discardbytes		- throw away a known number of bytes
 *
 *		same as pq_getbytes except we do not copy the data to anyplace.
 *		this is used for resynchronizing after read errors.
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
static int
pq_discardbytes(Port *port, size_t len)
{
	size_t		amount;

	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	while (len > 0)
	{
		while (port->PqRecvPointer >= port->PqRecvLength)
		{
			if (pq_recvbuf(port))	/* If nothing in buffer, then recv some */
				return EOF;		/* Failed to recv data */
		}
		amount = port->PqRecvLength - port->PqRecvPointer;
		if (amount > len)
			amount = len;
		port->PqRecvPointer += amount;
		len -= amount;
	}
	return 0;
}

/* --------------------------------
 *		pq_getstring	- get a null terminated string from connection
 *
 *		The return value is placed in an expansible StringInfo, which has
 *		already been initialized by the caller.
 *
 *		This is used only for dealing with old-protocol clients.  The idea
 *		is to produce a StringInfo that looks the same as we would get from
 *		pq_getmessage() with a newer client; we will then process it with
 *		pq_getmsgstring.  Therefore, no character set conversion is done here,
 *		even though this is presumably useful only for text.
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
int
pq_getstring(Port *port, StringInfo s)
{
	int			i;

	if (port->PqCommReadingMsg == false)
	{
	  	// TODO(SH): error
	}

	resetStringInfo(s);

	/* Read until we get the terminating '\0' */
	for (;;)
	{
		while (port->PqRecvPointer >= port->PqRecvLength)
		{
			if (pq_recvbuf(port))	/* If nothing in buffer, then recv some */
				return EOF;		/* Failed to recv data */
		}

		for (i = port->PqRecvPointer; i < port->PqRecvLength; i++)
		{
			if (port->PqRecvBuffer[i] == '\0')
			{
				/* include the '\0' in the copy */
				if (appendBinaryStringInfo(s, port->PqRecvBuffer + port->PqRecvPointer,
									   i - port->PqRecvPointer + 1) != 0) {
					return STATUS_ERROR;
				}
				port->PqRecvPointer = i + 1;	/* advance past \0 */
				return 0;
			}
		}

		/* If we're here we haven't got the \0 in the buffer yet. */
		if (appendBinaryStringInfo(s, port->PqRecvBuffer + port->PqRecvPointer,
							   port->PqRecvLength - port->PqRecvPointer) != 0) {
			return STATUS_ERROR;
		}
		port->PqRecvPointer = port->PqRecvLength;
	}
}


/* --------------------------------
 *		pq_startmsgread - begin reading a message from the client.
 *
 *		This must be called before any of the pq_get* functions.
 * --------------------------------
 */
void
pq_startmsgread(Port *port)
{
	/*
	 * There shouldn't be a read active already, but let's check just to be
	 * sure.
	 */
	if (port->PqCommReadingMsg)
	{
	  	// TODO(SH): error
	}

	port->PqCommReadingMsg = true;
}


/* --------------------------------
 *		pq_endmsgread	- finish reading message.
 *
 *		This must be called after reading a V2 protocol message with
 *		pq_getstring() and friends, to indicate that we have read the whole
 *		message. In V3 protocol, pq_getmessage() does this implicitly.
 * --------------------------------
 */
void
pq_endmsgread(Port *port)
{
	if (port->PqCommReadingMsg)
	{
	  	// TODO(SH): error
	}

	port->PqCommReadingMsg = false;
}

/* --------------------------------
 *		pq_is_reading_msg - are we currently reading a message?
 *
 * This is used in error recovery at the outer idle loop to detect if we have
 * lost protocol sync, and need to terminate the connection. pq_startmsgread()
 * will check for that too, but it's nicer to detect it earlier.
 * --------------------------------
 */
bool
pq_is_reading_msg(Port *port)
{
	return port->PqCommReadingMsg;
}

/* --------------------------------
 *		pq_getmessage	- get a message with length word from connection
 *
 *		The return value is placed in an expansible StringInfo, which has
 *		already been initialized by the caller.
 *		Only the message body is placed in the StringInfo; the length word
 *		is removed.  Also, s->cursor is initialized to zero for convenience
 *		in scanning the message contents.
 *
 *		If maxlen is not zero, it is an upper limit on the length of the
 *		message we are willing to accept.  We abort the connection (by
 *		returning EOF) if client tries to send more than that.
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
int
pq_getmessage(Port *port, StringInfo s, int maxlen)
{
	int32		len;

	if (port->PqCommReadingMsg)
	{
	  	// TODO(SH): error
	}

	resetStringInfo(s);

	/* Read message length word */
	if (pq_getbytes(port, (char *) &len, 4) == EOF)
	{
	  	// TODO(SH): error
		return EOF;
	}

	len = ntohl(len);

	if (len < 4 ||
		(maxlen > 0 && len > maxlen))
	{
	  	// TODO(SH): error
		return EOF;
	}

	len -= 4;					/* discount length itself */

	if (len > 0)
	{
		/*
		 * Allocate space for message.  If we run out of room (ridiculously
		 * large message), we will elog(ERROR), but we want to discard the
		 * message body so as not to lose communication sync.
		 */
		if (enlargeStringInfo(s, len) != 0) {
			return EOF;
		}

		/* And grab the message */
		if (pq_getbytes(port, s->data, len) == EOF)
		{
		  	// TODO(SH): error
			return EOF;
		}
		s->len = len;
		/* Place a trailing null per StringInfo convention */
		s->data[len] = '\0';
	}

	/* finished reading the message. */
	port->PqCommReadingMsg = false;

	return 0;
}


/* --------------------------------
 *		pq_putbytes		- send bytes to connection (not flushed until pq_flush)
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
int
pq_putbytes(Port *port, const char *s, size_t len)
{
	int			res;

	/* Should only be called by old-style COPY OUT */
	if (port->DoingCopyOut == false)
	{
	  	// TODO(SH): error
	}
	/* No-op if reentrant call */
	if (port->PqCommBusy)
		return 0;
	port->PqCommBusy = true;
	res = internal_putbytes(port, s, len);
	port->PqCommBusy = false;
	return res;
}

static int
internal_putbytes(Port *port, const char *s, size_t len)
{
	size_t		amount;

	while (len > 0)
	{
		/* If buffer is full, then flush it out */
		if (port->PqSendPointer >= port->PqSendBufferSize)
		{
			socket_set_nonblocking(port, false);
			if (internal_flush(port))
				return EOF;
		}
		amount = port->PqSendBufferSize - port->PqSendPointer;
		if (amount > len)
			amount = len;
		memcpy(port->PqSendBuffer + port->PqSendPointer, s, amount);
		port->PqSendPointer += amount;
		s += amount;
		len -= amount;
	}
	return 0;
}

/* --------------------------------
 *		socket_flush		- flush pending output
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
static int
socket_flush(Port *port)
{
	int			res;

	/* No-op if reentrant call */
	if (port->PqCommBusy)
		return 0;
	port->PqCommBusy = true;
	socket_set_nonblocking(port, false);
	res = internal_flush(port);
	port->PqCommBusy = false;
	return res;
}

/* --------------------------------
 *		internal_flush - flush pending output
 *
 * Returns 0 if OK (meaning everything was sent, or operation would block
 * and the socket is in non-blocking mode), or EOF if trouble.
 * --------------------------------
 */
static int
internal_flush(Port *port)
{
	static int	last_reported_send_errno = 0;

	char	   *bufptr = port->PqSendBuffer + port->PqSendStart;
	char	   *bufend = port->PqSendBuffer + port->PqSendPointer;

	while (bufptr < bufend)
	{
		int			r;

		r = secure_write(port, bufptr, bufend - bufptr);

		if (r <= 0)
		{
			if (errno == EINTR)
				continue;		/* Ok if we were interrupted */

			/*
			 * Ok if no data writable without blocking, and the socket is in
			 * non-blocking mode.
			 */
			if (errno == EAGAIN ||
				errno == EWOULDBLOCK)
			{
				return 0;
			}

			/*
			 * Careful: an ereport() that tries to write to the client would
			 * cause recursion to here, leading to stack overflow and core
			 * dump!  This message must go *only* to the postmaster log.
			 *
			 * If a client disconnects while we're in the midst of output, we
			 * might write quite a bit of data before we get to a safe query
			 * abort point.  So, suppress duplicate log messages.
			 */
			if (errno != last_reported_send_errno)
			{
				last_reported_send_errno = errno;
				// TODO(SH): error
			}

			/*
			 * We drop the buffered data anyway so that processing can
			 * continue, even though we'll probably quit soon. We also set a
			 * flag that'll cause the next CHECK_FOR_INTERRUPTS to terminate
			 * the connection.
			 */
			port->PqSendStart = port->PqSendPointer = 0;
			return EOF;
		}

		last_reported_send_errno = 0;	/* reset after any successful send */
		bufptr += r;
		port->PqSendStart += r;
	}

	port->PqSendStart = port->PqSendPointer = 0;
	return 0;
}

/* --------------------------------
 *		pq_flush_if_writable - flush pending output if writable without blocking
 *
 * Returns 0 if OK, or EOF if trouble.
 * --------------------------------
 */
static int
socket_flush_if_writable(Port *port)
{
	int			res;

	/* Quick exit if nothing to do */
	if (port->PqSendPointer == port->PqSendStart)
		return 0;

	/* No-op if reentrant call */
	if (port->PqCommBusy)
		return 0;

	/* Temporarily put the socket into non-blocking mode */
	socket_set_nonblocking(port, true);

	port->PqCommBusy = true;
	res = internal_flush(port);
	port->PqCommBusy = false;
	return res;
}

/* --------------------------------
 *	socket_is_send_pending	- is there any pending data in the output buffer?
 * --------------------------------
 */
static bool
socket_is_send_pending(Port *port)
{
	return (port->PqSendStart < port->PqSendPointer);
}

/* --------------------------------
 * Message-level I/O routines begin here.
 *
 * These routines understand about the old-style COPY OUT protocol.
 * --------------------------------
 */


/* --------------------------------
 *		socket_putmessage - send a normal message (suppressed in COPY OUT mode)
 *
 *		If msgtype is not '\0', it is a message type code to place before
 *		the message body.  If msgtype is '\0', then the message has no type
 *		code (this is only valid in pre-3.0 protocols).
 *
 *		len is the length of the message body data at *s.  In protocol 3.0
 *		and later, a message length word (equal to len+4 because it counts
 *		itself too) is inserted by this routine.
 *
 *		All normal messages are suppressed while old-style COPY OUT is in
 *		progress.  (In practice only a few notice messages might get emitted
 *		then; dropping them is annoying, but at least they will still appear
 *		in the postmaster log.)
 *
 *		We also suppress messages generated while pqcomm.c is busy.  This
 *		avoids any possibility of messages being inserted within other
 *		messages.  The only known trouble case arises if SIGQUIT occurs
 *		during a pqcomm.c routine --- quickdie() will try to send a warning
 *		message, and the most reasonable approach seems to be to drop it.
 *
 *		returns 0 if OK, EOF if trouble
 * --------------------------------
 */
static int
socket_putmessage(Port *port, char msgtype, const char *s, size_t len)
{
	if (port->DoingCopyOut || port->PqCommBusy)
		return 0;
	port->PqCommBusy = true;
	if (msgtype)
		if (internal_putbytes(port,&msgtype, 1))
			goto fail;
	if (PG_PROTOCOL_MAJOR(port->proto) >= 3)
	{
		uint32		n32;

		n32 = htonl((uint32) (len + 4));
		if (internal_putbytes(port, (char *) &n32, 4))
			goto fail;
	}
	if (internal_putbytes(port, s, len))
		goto fail;
	port->PqCommBusy = false;
	return 0;

fail:
	port->PqCommBusy = false;
	return EOF;
}

/* --------------------------------
 *		pq_putmessage_noblock	- like pq_putmessage, but never blocks
 *
 *		If the output buffer is too small to hold the message, the buffer
 *		is enlarged.
 */
static void
socket_putmessage_noblock(Port *port, char msgtype, const char *s, size_t len)
{
	int res		PG_USED_FOR_ASSERTS_ONLY;
	int			required;

	/*
	 * Ensure we have enough space in the output buffer for the message header
	 * as well as the message itself.
	 */
	required = port->PqSendPointer + 1 + 4 + len;
	if (required > port->PqSendBufferSize)
	{
		port->PqSendBuffer = static_cast<char *>(realloc(port->PqSendBuffer, required));
		port->PqSendBufferSize = required;
	}
	res = pq_putmessage(port, msgtype, s, len);
	if (res != 0)			/* should not fail when the message fits in * buffer */
	{
	  	// TODO(SH): error
	}
}


/* --------------------------------
 *		socket_startcopyout - inform libpq that an old-style COPY OUT transfer
 *			is beginning
 * --------------------------------
 */
static void
socket_startcopyout(Port *port)
{
  port->DoingCopyOut = true;
}

/* --------------------------------
 *		socket_endcopyout	- end an old-style COPY OUT transfer
 *
 *		If errorAbort is indicated, we are aborting a COPY OUT due to an error,
 *		and must send a terminator line.  Since a partial data line might have
 *		been emitted, send a couple of newlines first (the first one could
 *		get absorbed by a backslash...)  Note that old-style COPY OUT does
 *		not allow binary transfers, so a textual terminator is always correct.
 * --------------------------------
 */
static void
socket_endcopyout(Port *port, bool errorAbort)
{
  if (!port->DoingCopyOut)
    return;
  if (errorAbort)
    pq_putbytes(port, "\n\n\\.\n", 5);
  /* in non-error case, copy.c will have emitted the terminator line */
  port->DoingCopyOut = false;
}

/*
 * Support for TCP Keepalive parameters
 */

int
pq_getkeepalivesidle(Port *port)
{
#if defined(PG_TCP_KEEPALIVE_IDLE) || defined(SIO_KEEPALIVE_VALS)
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
		return 0;

	if (port->keepalives_idle != 0)
		return port->keepalives_idle;

	if (port->default_keepalives_idle == 0)
	{
#ifndef WIN32
		ACCEPT_TYPE_ARG3 size = sizeof(port->default_keepalives_idle);

		if (getsockopt(port->sock, IPPROTO_TCP, PG_TCP_KEEPALIVE_IDLE,
					   (char *) &port->default_keepalives_idle,
					   &size) < 0)
		{
          // TODO(sh): error
//			elog(LOG, "getsockopt(%s) failed: %m", PG_TCP_KEEPALIVE_IDLE_STR);
			port->default_keepalives_idle = -1; /* don't know */
		}
#else							/* WIN32 */
		/* We can't get the defaults on Windows, so return "don't know" */
		port->default_keepalives_idle = -1;
#endif   /* WIN32 */
	}

	return port->default_keepalives_idle;
#else
  return 0;
#endif
}

int
pq_setkeepalivesidle(int idle, Port *port)
{
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
    return STATUS_OK;

/* check SIO_KEEPALIVE_VALS here, not just WIN32, as some toolchains lack it */
#if defined(PG_TCP_KEEPALIVE_IDLE) || defined(SIO_KEEPALIVE_VALS)
  if (idle == port->keepalives_idle)
		return STATUS_OK;

#ifndef WIN32
	if (port->default_keepalives_idle <= 0)
	{
		if (pq_getkeepalivesidle(port) < 0)
		{
			if (idle == 0)
				return STATUS_OK;		/* default is set but unknown */
			else
				return STATUS_ERROR;
		}
	}

	if (idle == 0)
		idle = port->default_keepalives_idle;

	if (setsockopt(port->sock, IPPROTO_TCP, PG_TCP_KEEPALIVE_IDLE,
				   (char *) &idle, sizeof(idle)) < 0)
	{
      // TODO(sh): error
//		elog(LOG, "setsockopt(%s) failed: %m", PG_TCP_KEEPALIVE_IDLE_STR);
		return STATUS_ERROR;
	}

	port->keepalives_idle = idle;
#else							/* WIN32 */
	return pq_setkeepaliveswin32(port, idle, port->keepalives_interval);
#endif
#else
  if (idle != 0)
  {
    elog(LOG, "setting the keepalive idle time is not supported");
    return STATUS_ERROR;
  }
#endif

  return STATUS_OK;
}

int
pq_getkeepalivesinterval(Port *port)
{
#if defined(TCP_KEEPINTVL) || defined(SIO_KEEPALIVE_VALS)
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
		return 0;

	if (port->keepalives_interval != 0)
		return port->keepalives_interval;

	if (port->default_keepalives_interval == 0)
	{
#ifndef WIN32
		ACCEPT_TYPE_ARG3 size = sizeof(port->default_keepalives_interval);

		if (getsockopt(port->sock, IPPROTO_TCP, TCP_KEEPINTVL,
					   (char *) &port->default_keepalives_interval,
					   &size) < 0)
		{
          // TODO(sh): error
//			elog(LOG, "getsockopt(%s) failed: %m", "TCP_KEEPINTVL");
			port->default_keepalives_interval = -1;		/* don't know */
		}
#else
		/* We can't get the defaults on Windows, so return "don't know" */
		port->default_keepalives_interval = -1;
#endif   /* WIN32 */
	}

	return port->default_keepalives_interval;
#else
  return 0;
#endif
}

int
pq_setkeepalivesinterval(int interval, Port *port)
{
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
    return STATUS_OK;

#if defined(TCP_KEEPINTVL) || defined(SIO_KEEPALIVE_VALS)
  if (interval == port->keepalives_interval)
		return STATUS_OK;

#ifndef WIN32
	if (port->default_keepalives_interval <= 0)
	{
		if (pq_getkeepalivesinterval(port) < 0)
		{
			if (interval == 0)
				return STATUS_OK;		/* default is set but unknown */
			else
				return STATUS_ERROR;
		}
	}

	if (interval == 0)
		interval = port->default_keepalives_interval;

	if (setsockopt(port->sock, IPPROTO_TCP, TCP_KEEPINTVL,
				   (char *) &interval, sizeof(interval)) < 0)
	{
      // TODO(sh): error
//		elog(LOG, "setsockopt(%s) failed: %m", "TCP_KEEPINTVL");
		return STATUS_ERROR;
	}

	port->keepalives_interval = interval;
#else							/* WIN32 */
	return pq_setkeepaliveswin32(port, port->keepalives_idle, interval);
#endif
#else
  if (interval != 0)
  {
    elog(LOG, "setsockopt(%s) not supported", "TCP_KEEPINTVL");
    return STATUS_ERROR;
  }
#endif

  return STATUS_OK;
}

int
pq_getkeepalivescount(Port *port)
{
#ifdef TCP_KEEPCNT
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
		return 0;

	if (port->keepalives_count != 0)
		return port->keepalives_count;

	if (port->default_keepalives_count == 0)
	{
		ACCEPT_TYPE_ARG3 size = sizeof(port->default_keepalives_count);

		if (getsockopt(port->sock, IPPROTO_TCP, TCP_KEEPCNT,
					   (char *) &port->default_keepalives_count,
					   &size) < 0)
		{
          // TODO(sh): error
//			elog(LOG, "getsockopt(%s) failed: %m", "TCP_KEEPCNT");
			port->default_keepalives_count = -1;		/* don't know */
		}
	}

	return port->default_keepalives_count;
#else
  return 0;
#endif
}

int
pq_setkeepalivescount(int count, Port *port)
{
  if (port == NULL || IS_AF_UNIX(port->laddr.addr.ss_family))
    return STATUS_OK;

#ifdef TCP_KEEPCNT
  if (count == port->keepalives_count)
		return STATUS_OK;

	if (port->default_keepalives_count <= 0)
	{
		if (pq_getkeepalivescount(port) < 0)
		{
			if (count == 0)
				return STATUS_OK;		/* default is set but unknown */
			else
				return STATUS_ERROR;
		}
	}

	if (count == 0)
		count = port->default_keepalives_count;

	if (setsockopt(port->sock, IPPROTO_TCP, TCP_KEEPCNT,
				   (char *) &count, sizeof(count)) < 0)
	{
      // TODO(sh): error
//		elog(LOG, "setsockopt(%s) failed: %m", "TCP_KEEPCNT");
		return STATUS_ERROR;
	}

	port->keepalives_count = count;
#else
  if (count != 0)
  {
    elog(LOG, "setsockopt(%s) not supported", "TCP_KEEPCNT");
    return STATUS_ERROR;
  }
#endif

  return STATUS_OK;
}

