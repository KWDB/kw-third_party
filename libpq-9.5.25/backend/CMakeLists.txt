
set(TARGET_NAME "pqs")
add_definitions(-fpermissive -w ${OPENSSL_FLAG})
aux_source_directory(. DIR_LIB_SRCS)
set(DIR_LIB_SRCS ${DIR_LIB_SRCS}
        lib/stringinfo.c
        )

set(DIR_LIB_SRCS ${DIR_LIB_SRCS}
        libpq/auth.c
        libpq/be-secure.c
        libpq/hba.c
        libpq/ip.c
        libpq/md5.c
        libpq/pqcomm.c
        libpq/pqformat.c
        )

if (WITH_OPENSSL)
    set(DIR_LIB_SRCS ${DIR_LIB_SRCS} libpq/be-secure-openssl.c)
endif()

set(DIR_LIB_SRCS ${DIR_LIB_SRCS}
        ../port/strlcpy.c
        ../port/pgstrcasecmp.c
        ../port/noblock.c
        )

set_source_files_properties(PROPERTY ${DIR_LIB_SRCS} PROPERTIES LANGUAGE CXX)
add_library(${TARGET_NAME} ${DIR_LIB_SRCS})

target_link_libraries(${TARGET_NAME} ${SSL_LIB} pthread crypto)

install(TARGETS ${TARGET_NAME}
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/bin
        LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
        ARCHIVE DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)

