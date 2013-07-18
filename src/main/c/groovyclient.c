/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if defined(_MSC_VER) || defined(__MINGW32__)
#define WINDOWS
#else
#define UNIX
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/param.h>
#include <unistd.h>
#include <signal.h>

#include "config.h"

#ifdef WINDOWS
#include <windows.h>
#include <winsock2.h>
#include <process.h>
#include <sys/fcntl.h>
#endif

#include "option.h"
#include "session.h"

static void scriptdir(char* result_dir, char* script_path)
{
    // prepare work variable of script path
    int script_path_length = strlen(script_path);
    char work_path[script_path_length];
    strcpy(work_path, script_path);

    // cut off a part of script name
    char* work_pt = work_path + script_path_length;
    while (work_pt > work_path && (*work_pt != '/' && *work_pt != '\\')) {
        work_pt--;
    }
    if (*work_pt == '/' || *work_pt == '\\') {
        work_pt++;
    }
    if (*work_pt != '\0') { // cut
        *work_pt = '\0';
    }

    // set result
    strcpy(result_dir, work_path);

#ifdef DEBUG
    fprintf(stderr, "DEBUG: scriptdir: %s, %zu\n", result_dir, strlen(result_dir));
#endif
}

static char* groovyserver_cmdline(char* script_path, char* arg, int port)
{
    // resolve base directory
    char basedir_path[MAXPATHLEN];
    char* groovyserv_home = getenv("GROOVYSERV_HOME");
    if (groovyserv_home == NULL) {
        scriptdir(basedir_path, script_path);
    } else {
#ifdef WINDOWS
        // If the path is on cygwin, scriptdir is used instead of GROOVYSERV_HOME.
        if (*groovyserv_home == '/') {
            if (!client_option.quiet) {
                fprintf(stderr, "WARN: Using the directory path of groovyclient instead of the invalid GROOVYSERV_HOME which isn't a path format on Windows: %s\n", groovyserv_home);
                fflush(stderr);
            }
            scriptdir(basedir_path, script_path);
        } else {
            sprintf(basedir_path, "%s\\bin\\", groovyserv_home);
        }
#else
        sprintf(basedir_path, "%s/bin/", groovyserv_home);
#endif
    }

#ifdef DEBUG
    fprintf(stderr, "DEBUG: basedir_path: %s, %zu\n", basedir_path, strlen(basedir_path));
#endif

    // make command line to invoke groovyserver
    static char cmdline[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(cmdline, "\"%sgroovyserver.bat\" -p %d %s", basedir_path, port, arg);
#else
    sprintf(cmdline, "'%sgroovyserver' -p %d %s", basedir_path, port, arg);
#endif

#ifdef DEBUG
    fprintf(stderr, "DEBUG: cmdline: %s, %zu\n", cmdline, strlen(cmdline));
#endif

    return cmdline;
}

void invoke_server(char* script_path, int port, char* main_opt, char* authtoken)
{
    char* opt = strdup(main_opt);
    char* quiet_option = " -q";

    if (client_option.quiet) {
        opt = realloc(opt, strlen(opt) + strlen(quiet_option) + 1);
        strcat(opt, quiet_option);
    }

    char authtoken_option[BUFFER_SIZE];
    if (authtoken != NULL) {
        sprintf(authtoken_option, " --authtoken %s", authtoken);
        opt = realloc(opt, strlen(opt) + strlen(authtoken_option) + 1);
        strcat(opt, authtoken_option);
    }

    char* cmdline = groovyserver_cmdline(script_path, opt, port);
    if (!client_option.quiet) {
        fprintf(stderr, "Invoking server: %s\n", cmdline);
        fflush(stderr);
    }
    system(cmdline);
    free(opt);
}

void start_server(char* script_path, int port, char* authtoken)
{
    invoke_server(script_path, port, "", authtoken);
}

void kill_server(char* script_path, int port)
{
    invoke_server(script_path, port, "-k", NULL);
}

void restart_server(char* script_path, int port, char* authtoken)
{
    invoke_server(script_path, port, "-r", authtoken);
}

/*
 * read authentication authtoken.
 */
static void read_authtoken(char* authtoken, int size, int port)
{
    char path[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(path, "%s\\.groovy\\groovyserv\\authtoken-%d", getenv("USERPROFILE"), port);
#else
    sprintf(path, "%s/.groovy/groovyserv/authtoken-%d", getenv("HOME"), port);
#endif
    FILE* fp = fopen(path, "r");
    if (fp != NULL) {
        if (fgets(authtoken, size, fp) == NULL) {
            perror("ERROR: fgets");
            exit(1);
        }
        fclose(fp);
    }
    else {
        fprintf(stderr, "ERROR: cannot open authtoken file\n");
        exit(1);
    }
}

static char* get_host()
{
    if (client_option.host != NULL) {
        return client_option.host;
    }

    static char* host_env;
    host_env = getenv("GROOVYSERVER_HOST");
    if (host_env != NULL) {
        return host_env;
    }

    return DESTHOST;
}

static int get_port()
{
    if (client_option.port != PORT_NOT_SPECIFIED) {
        return client_option.port;
    }

    char* port_str = getenv("GROOVYSERVER_PORT");
    if (port_str != NULL) {
        int port;
        if (sscanf(port_str, "%d", &port) != 1) {
            fprintf(stderr, "ERROR: port number %s of GROOVYSERV_PORT error\n", port_str);
            exit(1);
        }
        return port;
    }

    return DESTPORT;
}

static char* get_authtoken_specified_by_client(int port)
{
    if (client_option.authtoken != NULL) {
        return client_option.authtoken;
    }
    return NULL;
}

static char* get_authtoken_generated_by_server(int port)
{
    static char authtoken[BUFFER_SIZE];
    read_authtoken(authtoken, sizeof(authtoken), port);
    return authtoken;
}

static int connect_server(char* argv0, char* host, int port, char* authtoken)
{
    int fd;

    int failCount = 0;

    while ((fd = open_socket(host, port)) == -1) {
        if (failCount >= 3) {
            fprintf(stderr, "ERROR: failed to start up groovyserver\n");
            exit(1);
        }
        start_server(argv0, port, authtoken);

#ifdef WINDOWS
        Sleep(3000);
#else
        sleep(3);
#endif
        failCount++;
    }
    return fd;
}

static int fd_soc;

static void signal_handler(int sig) {
#ifdef WINDOWS
    send(fd_soc, "Size: -1\n\n", 10, 0);
    closesocket(fd_soc);
#else
    write(fd_soc, "Size: -1\n\n", 10);
    close(fd_soc);
#endif
    exit(1);
}

/*
 * open socket and initiate session.
 */
int main(int argc, char** argv)
{
#ifdef WINDOWS
    WSADATA wsadata;
    if (WSAStartup(MAKEWORD(1,1), &wsadata) == SOCKET_ERROR) {
        fprintf(stderr, "ERROR: creating socket\n");
        exit(1);
    }

    // make standard output to binary mode.
    if (_setmode(_fileno(stdout), _O_BINARY) < 0) {
        fprintf(stderr, "ERROR: setmode stdout failed\n");
        exit(1);
    }
#endif

    scan_options(&client_option, argc, argv);

    char* host = get_host();
    int port = get_port();

    // authtoken sprintf by client can use for both control to server and connection of server.
    // when authtoken is not specified by client, it means that a user want to use remote
    // groovyserver, groovyserver must generate new authtoken by server side
    char* authtoken = get_authtoken_specified_by_client(port);

    // control server
    if (client_option.kill) {
        kill_server(argv[0], port);
        exit(0);
    }
    else if (client_option.restart) {
        restart_server(argv[0], port, authtoken);
    }

    // connect to server
    fd_soc = connect_server(argv[0], host, port, authtoken);
    signal(SIGINT, signal_handler); // using fd_soc in handler

    // ~/.groovy/groovserv/authtoken-NNNN file data can use only for connection to server.
    // but if authtoken is already specified by client, it must be used to the connection
    // instead of the file data, because it means that a user want to use a remote groovyserver.
    if (authtoken == NULL) {
        authtoken = get_authtoken_generated_by_server(port);
    }

    // invoke a script on server
    send_header(fd_soc, argc, argv, authtoken);
    int status = start_session(fd_soc);

    // print particular error status message
    // FIXME it's strongly bound to exit code of ExitStatus on groovyserver.
    //       and it will easily conflict with exit code which is specified at user script...
    if (status == ERROR_INVALID_AUTHTOKEN) {
        fprintf(stderr, "ERROR: rejected by groovyserv because of invalid authtoken\n");
    } else if (status == ERROR_CLIENT_NOT_ALLOWED) {
        fprintf(stderr, "ERROR: rejected by groovyserv because of not allowed client address\n");
    }

#ifdef DEBUG
    fprintf(stderr, "DEBUG: exit code: %d\n", status);
#endif

    // an additional text after invoking
    if (client_option.help) {
        usage();
        exit(status);
    }
    if (client_option.version) {
        version();
        exit(status);
    }

#ifdef WINDOWS
    WSACleanup();
#endif

    return status;
}

