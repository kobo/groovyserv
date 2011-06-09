/*
 * Copyright 2009-2011 the original author or authors.
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
#include <ctype.h>
#include <assert.h>

#include <sys/param.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>

#include "config.h"

#ifdef WINDOWS
#include <windows.h>
#include <winsock2.h>
#include <process.h>
#include <sys/fcntl.h>
#else
#include <sys/socket.h> // AF_INET
#include <netinet/in.h> // sockaddr_in
#include <netdb.h>      // gethostbyname
#include <sys/uio.h>
#include <sys/errno.h>
#endif

#include "buf.h"
#include "option.h"
#include "bool.h"
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
    //fprintf(stderr, "DEBUG: scriptdir: %s, %d\n", result_dir, strlen(result_dir));
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
    //fprintf(stderr, "DEBUG: basedir_path: %s, %d\n", basedir_path, strlen(basedir_path));

    // make command line to invoke groovyserver
    static char cmdline[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(cmdline, "\"%sgroovyserver.bat\" -p %d %s", basedir_path, port, arg);
#else
    sprintf(cmdline, "'%sgroovyserver' -p %d %s", basedir_path, port, arg);
#endif
    //fprintf(stderr, "DEBUG: cmdline: %s, %d\n", cmdline, strlen(cmdline));
    return cmdline;
}

void invoke_server(char* script_path, int port, char* main_opt)
{
    char* opt = strdup(main_opt);
    char* quiet_option = " -q";

    if (client_option.quiet) {
        opt = realloc(opt, strlen(opt)+strlen(quiet_option)+1); 
        strcat(opt, quiet_option);
    }
    char* cmdline = groovyserver_cmdline(script_path, opt, port);
    if (!client_option.quiet) {
        fprintf(stderr, "Invoking server: %s\n", cmdline);
        fflush(stderr);
    }
    system(cmdline);
    free(opt);
}

void start_server(char* script_path, int port)
{
    invoke_server(script_path, port, "");
}

void kill_server(char* script_path, int port)
{
    invoke_server(script_path, port, "-k");
}

void restart_server(char* script_path, int port)
{
    invoke_server(script_path, port, "-r");
}

/*
 * read authentication cookie.
 */
static void read_cookie(char* cookie, int size, int port)
{
    char path[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(path, "%s\\.groovy\\groovyserv\\cookie-%d", getenv("USERPROFILE"), port);
#else
    sprintf(path, "%s/.groovy/groovyserv/cookie-%d", getenv("HOME"), port);
#endif
    FILE* fp = fopen(path, "r");
    if (fp != NULL) {
        if (fgets(cookie, size, fp) == NULL) {
            perror("ERROR: fgets");
            exit(1);
        }
        fclose(fp);
    }
    else {
        fprintf(stderr, "ERROR: cannot open cookie file\n");
        exit(1);
    }
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

static int connect_server(char* argv0, int port)
{
    int fd;

    int failCount = 0;

    while ((fd = open_socket(DESTSERV, port)) == -1) {
        if (client_option.without_invocation_server == TRUE) {
            fprintf(stderr, "ERROR: groovyserver isn't running\n");
            exit(9);
        }
        if (failCount >= 3) {
            fprintf(stderr, "ERROR: Failed to start up groovyserver\n");
            exit(1);
        }
        start_server(argv0, port);

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
        fprintf(stderr, "ERROR: creating socket");
        exit(1);
    }

    // make standard output to binary mode.
    if (_setmode(_fileno(stdout), _O_BINARY) < 0) {
        fprintf(stderr, "ERROR: setmode stdout failed");
        exit(1);
    }
#endif

    scan_options(&client_option, argc, argv);

#ifdef DEBUG
    print_client_options(&client_option);
#endif

    int port = get_port();

    if (client_option.kill) {
        kill_server(argv[0], port);
        exit(0);
    }
    else if (client_option.restart) {
        restart_server(argv[0], port);
        exit(0);
    }

    fd_soc = connect_server(argv[0], port);
    signal(SIGINT, signal_handler); // using fd_soc in handler

    char cookie[BUFFER_SIZE];
    read_cookie(cookie, sizeof(cookie), port);

    send_header(fd_soc, argc, argv, cookie);
    int status = start_session(fd_soc);

    if (client_option.help) {
        usage();
        exit(0);
    }

#ifdef WINDOWS
    WSACleanup();
#endif

    return status;
}

