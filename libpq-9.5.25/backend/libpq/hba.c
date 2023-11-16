// Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.
/*-------------------------------------------------------------------------
 *
 * hba.c
 *	  Routines to handle host based authentication (that's the scheme
 *	  wherein you authenticate a user by seeing what IP address the system
 *	  says he comes from and choosing authentication method based on it).
 *
 * Portions Copyright (c) 1996-2015, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  src/backend/libpq/hba.c
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"

#include <ctype.h>
#include <pwd.h>
#include <fcntl.h>
#include <sys/param.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>


#include "libpq/hba.h"
#include "libpq/ip.h"
#include "libpq/libpq.h"
#include "nodes/pg_list.h"
#include "port.h"


/*
 *	Scan the (pre-parsed) ident usermap file line by line, looking for a match
 *
 *	See if the user with ident username "auth_user" is allowed to act
 *	as Postgres user "pg_role" according to usermap "usermap_name".
 *
 *	Special case: Usermap NULL, equivalent to what was previously called
 *	"sameuser" or "samerole", means don't look in the usermap file.
 *	That's an implied map wherein "pg_role" must be identical to
 *	"auth_user" in order to be authorized.
 *
 *	Iff authorized, return STATUS_OK, otherwise return STATUS_ERROR.
 */
int
check_usermap(const char *usermap_name,
			  const char *pg_role,
			  const char *auth_user,
			  bool case_insensitive)
{
	bool		found_entry = false,
				error = false;

	if (usermap_name == NULL || usermap_name[0] == '\0')
	{
		if (case_insensitive)
		{
			if (pg_strcasecmp(pg_role, auth_user) == 0)
				return STATUS_OK;
		}
		else
		{
			if (strcmp(pg_role, auth_user) == 0)
				return STATUS_OK;
		}
        // TODO(sh): error
		// ereport(LOG,
		// 		(errmsg("provided user name (%s) and authenticated user name (%s) do not match",
		// 				pg_role, auth_user)));
		return STATUS_ERROR;
	}
	else
	{
		// ListCell   *line_cell;

		// foreach(line_cell, parsed_ident_lines)
		// {
		// 	check_ident_usermap(lfirst(line_cell), usermap_name,
		// 						pg_role, auth_user, case_insensitive,
		// 						&found_entry, &error);
		// 	if (found_entry || error)
		// 		break;
		// }
	}
	if (!found_entry && !error)
	{
        // TODO(sh): error
		// ereport(LOG,
		// 		(errmsg("no match in usermap \"%s\" for user \"%s\" authenticated as \"%s\"",
		// 				usermap_name, pg_role, auth_user)));
	}
	return found_entry ? STATUS_OK : STATUS_ERROR;
}