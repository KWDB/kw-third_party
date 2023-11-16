set(TARGET_NAME "pq")
set(SO_MAJOR_VERSION 5)
set(SO_MINOR_VERSION 8)
add_definitions(-DFRONTEND -DUNSAFE_STAT_OK -fPIC -Wno-format-truncation -D_GNU_SOURCE -w ${OPENSSL_FLAG})
set(SHLIB_EXPORTS "libpq/exports.txt")
#aux_source_directory(wchar.c DIR_LIB_SRCS)


set(DIR_LIB_SRCS
		libpq/aeparameter.c
		libpq/chklocale.c
		libpq/encnames.c
		libpq/fe-auth.c
		libpq/fe-connect.c
		libpq/fe-exec.c
		libpq/fe-lobj.c
		libpq/fe-misc.c
		libpq/fe-print.c
		libpq/fe-protocol2.c
		libpq/fe-protocol3.c
		libpq/fe-secure.c
		libpq/getpeerid.c
		libpq/inet_net_ntop.c
		libpq/ip.c
		libpq/libpq-events.c
		libpq/md5.c
		libpq/noblock.c
		libpq/pgstrcasecmp.c
		libpq/pqexpbuffer.c
		libpq/strlcpy.c
		libpq/thread.c
		libpq/wchar.c
		)
if (WITH_OPENSSL)
	set(DIR_LIB_SRCS ${DIR_LIB_SRCS} libpq/fe-secure-openssl.c)
endif()

add_library(${TARGET_NAME} SHARED ${DIR_LIB_SRCS})
SET_TARGET_PROPERTIES(${TARGET_NAME} PROPERTIES 
    VERSION ${SO_MAJOR_VERSION}
    SOVERSION ${SO_MAJOR_VERSION}.${SO_MINOR_VERSION})

add_library(${TARGET_NAME}_static STATIC ${DIR_LIB_SRCS})

target_link_libraries(${TARGET_NAME} ${SSL_LIB} pthread crypto)

install(TARGETS ${TARGET_NAME} ${TARGET_NAME}_static
		RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
		LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
		ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)