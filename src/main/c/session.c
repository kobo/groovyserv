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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <assert.h>

#include "config.h"

#include <sys/types.h>  // netinet/in.h
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

#include <sys/param.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>

#include "base64.h"
#include "buf.h"
#include "option.h"
#include "bool.h"
#include "session.h"

// request headers
const char * const HEADER_KEY_CURRENT_WORKING_DIR = "Cwd";
const char * const HEADER_KEY_ARG = "Arg";
const char * const HEADER_KEY_ENV = "Env";
const char * const HEADER_KEY_CP = "Cp";
const char * const HEADER_KEY_AUTHTOKEN = "AuthToken";

// response headers
const char * const HEADER_KEY_CHANNEL = "Channel";
const char * const HEADER_KEY_SIZE = "Size";
const char * const HEADER_KEY_STATUS = "Status";

const int CR = 0x0d;
const int CANCEL = 0x18;

#ifdef WINDOWS
extern char __declspec(dllimport) **environ;
#else
extern char **environ;
#endif

/*
 * Make socket and connect to the server.
 */
int open_socket(char* server_host, int server_port)
{
    struct hostent *hostent;
    struct sockaddr_in server;

    // FIXME to use getaddressinfo or getnameinfo
    hostent = gethostbyname(server_host); // lookup IP
    if (hostent == NULL) {
        printf("ERROR: cannot resolve host address: %s\n", server_host);
        exit(1);
    }

    memset(&server, 0, sizeof(server)); // zero clear struct

    server.sin_family = PF_INET;        // server.sin_addr = hostent->h_addr
    memcpy(&server.sin_addr, hostent->h_addr, hostent->h_length);
    server.sin_port = htons(server_port);

    int fd;
#ifdef WINDOWS
    if ((fd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) == INVALID_SOCKET) {
#else
    if ((fd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0) {
#endif
        perror("ERROR: socket");
        exit(1);
    }

#ifdef WINDOWS
    if (connect(fd, (struct sockaddr *)&server, sizeof(server)) == SOCKET_ERROR) {
        return -1;
    }
#else
    if (connect(fd, (struct sockaddr *)&server, sizeof(server)) < 0) {
        if (errno == ECONNREFUSED) {
            return -1;
        }
        perror("ERROR: connect");
        exit(1);
    }
#endif
    return fd;
}

/*
 * return TRUE if the NAME part of str("NAME=VALUE") matches the pattern.
 */
static BOOL mask_match(char* pattern, const char* str)
{
    char* pos = strchr(str, '=');

    if (pos == NULL) {
        printf("ERROR: environment variable %s format invalid\n", str);
        exit(1);
    }
    *pos = '\0';
    BOOL result = strstr(str, pattern) != NULL;
    *pos = '='; // resume terminted NAME.
    return result;
}

static BOOL masks_match(char** masks, char* str)
{
    char** p;
    for (p = masks; p-masks < MAX_MASK && *p != NULL; p++) {
        if (mask_match(*p, str)) {
            return TRUE;
        }
    }
    return FALSE;
}

static void make_env_headers(buf* read_buf, char** env, char** inc_mask, char** exc_mask)
{
    int i;
    for (i = 0; env[i] != NULL; i++) {
        if (client_option.env_all || masks_match(client_option.env_include_mask, env[i])) {
            if (!masks_match(client_option.env_exclude_mask, env[i])) {
                buf_printf(read_buf, "%s: %s\n", HEADER_KEY_ENV, env[i]);
            }
        }
    }
}

/*
 * Send header information which includes current working direcotry,
 * command line arguments, and CLASSPATH environment variable
 * to the server.
 */
void send_header(int fd, int argc, char** argv, char* authtoken)
{
    char path_buffer[MAXPATHLEN];
    buf read_buf = buf_new(BUFFER_SIZE, NULL);
    int i;

    // send current working directory.
    buf_printf(&read_buf, "%s: ", HEADER_KEY_CURRENT_WORKING_DIR);
    char* cwd = getcwd(path_buffer, MAXPATHLEN);
    if (cwd == NULL) {
        perror("ERROR: getcwd");
        exit(1);
    }

    buf_add(&read_buf, cwd);
    buf_add(&read_buf, "\n");

    buf_printf(&read_buf, "%s: %s\n", HEADER_KEY_AUTHTOKEN, authtoken);

    // send command line arguments.
    char *encoded_ptr, *encoded_work;
    for (i = 1; i < argc; i++) {
        if (argv[i] != NULL) {
            // base64 encoded data is less "(original size) * 1.5 + 5" as much as raw data
            // "+5" is a extra space for '=' padding and NULL as the end of string
            encoded_ptr = malloc(sizeof(char) * strlen(argv[i]) * 1.5 + 5);
            if (encoded_ptr == NULL) {
                perror("ERROR: failed to malloc");
                exit(1);
            }
            encoded_work = encoded_ptr; // copy for free
            base64_encode(encoded_work, (unsigned char*) argv[i]);
            buf_printf(&read_buf, "%s: %s\n", HEADER_KEY_ARG, encoded_work);
            free(encoded_ptr);
        }
    }

    // send envvars.
    if (client_option.env_include_mask != NULL) {
        make_env_headers(&read_buf,
                         environ,
                         client_option.env_include_mask,
                         client_option.env_exclude_mask);
    }

    char* cp = getenv("CLASSPATH");
    if (cp != NULL) {
        buf_printf(&read_buf, "%s: %s\n", HEADER_KEY_CP, cp);
    }

    buf_printf(&read_buf, "\n");
    read_buf.size--; /* remove trailing '\0' */

#ifdef WINDOWS
    send(fd, read_buf.buffer, read_buf.size, 0);
#else
    write(fd, read_buf.buffer, read_buf.size);
#endif
    buf_delete(&read_buf);
}

/*
 * parse server response header.
 */
static void read_header(char* buf, struct header_t* header)
{
    //fprintf(stderr, "DEBUG: read_header: line: %s<LF> (size:%d)\n", buf, strlen(buf));

    // key
    char* p = strtok(buf, " :");
    if (p == NULL) {
        fprintf(stderr, "ERROR: key is NULL\n");
        exit(1);
    }
    if (strlen(p) > MAX_HEADER_KEY_LEN) {
        fprintf(stderr, "ERROR: key \"%s\" is too long\n", p);
        exit(1);
    }
    int i;
    for (i = 0; i < strlen(p); i++) {
        if (p[i] == CANCEL) {
            exit(0);
        }
        if (isspace((unsigned char) p[i])) {
            // if invoked "groovyclient" without arguments, it should works
            // as error message command and print usage by delegated groovy command
            return;
        }
        if (!isalnum((unsigned char) p[i])) {
            fprintf(stderr, "ERROR: key \"%s\" is invalid: %x\n", p, p[i]);
            exit(1);
        }
    }
    strncpy(header->key, p, MAX_HEADER_KEY_LEN);

    // value
    p = strtok(NULL, "\n");
    if (p == NULL) {
        fprintf(stderr, "ERROR: value of key \"%s\" is NULL: %s\n", header->key, p);
        exit(1);
    }
    while (isspace((unsigned char) *p)) { // ignore spaces
        p++;
    }
    if (strlen(p) > MAX_HEADER_VALUE_LEN) {
        fprintf(stderr, "ERROR: value of key \"%s\" is too long: %s\n", header->key, p);
        exit(1);
    }
    strncpy(header->value, p, MAX_HEADER_VALUE_LEN);

    //fprintf(stderr, "DEBUG: read_header: parsed: \"%s\"(size:%d) => \"%s\"(size:%d)\n", header->key, strlen(header->key), header->value, strlen(header->value));
}

static char* read_line(int fd, char* buf, int size)
{
     int i;
     for (i = 0; i < size; i++) {
#ifdef WINDOWS
         int ret = recv(fd, buf + i, 1, 0);
         if (ret == -1) {
             fprintf(stderr, "ERROR: failed to read line: %d\n", WSAGetLastError());
             exit(1);
         }
         if (ret != 1) {
             // signal handler output breaks stream.
             //fprintf(stderr, "DEBUG: read_line (maybe by signal handler): %d\n", ret);
             return NULL;
         }
#else
         read(fd, buf + i, 1);
#endif
         if (buf[i] == '\n') {
             //fprintf(stderr, "DEBUG: read_line (until LF): %s<LF> (size:%d)\n", buf, strlen(buf));
             return buf;
         }
     }
     //fprintf(stderr, "DEBUG: read_line (size over): %s<END> (size:%d)\n", buf, strlen(buf));
     return buf;
 }

/*
 * read server response headers.
 */
static int read_headers(int fd, struct header_t headers[])
{
    char read_buf[BUFFER_SIZE];
    char *p;
    int pos = 0;
    while (1) {
        memset(read_buf, 0, sizeof(read_buf));
        p = read_line(fd, read_buf, BUFFER_SIZE);
        if (p == NULL) {
          return 0;
        }
        if (*p == '\0') {
            return 0;
        }
        if (*p == CR) { // FIXME For what is it worth?
            p++;
        }
        if (*p == '\n') { // if empty line
            break;
        }
        read_header(read_buf, headers + pos);
        if (pos > MAX_HEADER) {
            fprintf(stderr, "ERROR: too many headers\n");
            exit(1);
        }
        pos++;
    }
    return pos;
}

/*
 * find header value.
 */
static char* find_header(struct header_t headers[], const char* key, int header_size)
{
    int i;
    for (i = 0; i < header_size; i++) {
        if (strcmp(headers[i].key, key) == 0) {
            return headers[i].value;
        }
    }
    return NULL;
}

static int min_int(int a,int b) {
    return (a < b) ? a : b;
}

/*
 * Receive a chunk, and write it to stdout or stderr.
 */
static int receive_from_server(int socket, char* stream_identifier, int size)
{
    // select output stream
    int output_fd;
    if (strcmp(stream_identifier, "out") == 0) {
        output_fd = fileno(stdout);
    }
    else if (strcmp(stream_identifier, "err") == 0) {
        output_fd = fileno(stderr);
    }
    else {
        fprintf(stderr, "ERROR: unrecognizable stream identifier: %s\n", stream_identifier);
        exit(1);
    }

    // copy to stream
    char read_buf[BUFFER_SIZE];
    int remained_size = size;
    int ret;
    while ((ret = recv(socket, read_buf, min_int(remained_size, BUFFER_SIZE), 0)) > 0) {
        write(output_fd, read_buf, ret);
        remained_size -= ret;
    }
    return 0;
}

/*
 * Copy data from stdin and send it to the server.
 */
static int send_to_server(int fd)
{
    char read_buf[BUFFER_SIZE+1];
    int ret;

    if ((ret = read(fileno(stdin), read_buf, BUFFER_SIZE)) == -1){ // TODO buffering
        perror("ERROR: failed to read from stdin");
        exit(1);
    }
    read_buf[ret] = '\0';

    char write_buf[BUFFER_SIZE];
    sprintf(write_buf, "Size: %d\n\n", ret); // TODO: check size

#ifdef WINDOWS
    send(fd, write_buf, strlen(write_buf), 0);
    send(fd, read_buf, ret, 0);
#else
    write(fd, write_buf, strlen(write_buf));
    write(fd, read_buf, ret);
#endif

    if (ret == 0) {
        return 1;
    }
    return 0;
}

#ifdef WINDOWS
static void copy_stdin_to_socket(int fd)
{
    while (1) {
        send_to_server(fd);
    }
}

static void invoke_thread(int fd)
{
    DWORD id = 1;
    CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) copy_stdin_to_socket, (LPVOID)fd, 0, &id);
}
#endif

/*
 * asynchronus input (select) with the stdin and the socket connection
 * to the server. copy input data from stdin to server, and
 * copy received data from the server to stdout/stderr.
 * destination of output is stdout or stderr are distinguished by
 * stream identifier(sid) header is 'out' or 'err'.
 */
int start_session(int fd)
{
    struct header_t headers[MAX_HEADER];

#ifdef WINDOWS
    invoke_thread(fd);
#else
    fd_set read_set;
    int ret;
    int stdin_closed = 0;
#endif

    while (1) {
#ifdef UNIX
        // initialize the set of file descriptor
        FD_ZERO(&read_set);

        // watch stdin of client and socket.
        FD_SET(0, &read_set);
        FD_SET(fd, &read_set);

        if ((ret = select(FD_SETSIZE, &read_set, (fd_set*)NULL, (fd_set*)NULL, NULL)) == -1) {
            perror("ERROR: select failure");
            exit(1);
        }
        if (ret == 0) {
            fprintf(stderr, "ERROR: timeout?\n");
            continue;
        }

        // detect changed descriptor
        if (!stdin_closed && FD_ISSET(0, &read_set)) { // stdin
            stdin_closed = send_to_server(fd);
        }
        if (FD_ISSET(fd, &read_set) == FALSE){ // socket
            continue;
        }
#endif

        int size = read_headers(fd, headers);
        if (size == 0) {
            return 0; // as normal exit if header size 0
        }

        // Process exit
        char* status = find_header(headers, HEADER_KEY_STATUS, size);
        if (status != NULL) {
            int stat = atoi(status);
            return stat;
        }

        // Dispatch data from server to stdout/err.
        char* sid = find_header(headers, HEADER_KEY_CHANNEL, size);
        if (sid == NULL) {
            fprintf(stderr, "ERROR: required header %s not found\n", HEADER_KEY_CHANNEL);
            return 1;
        }
        char* chunk_size = find_header(headers, HEADER_KEY_SIZE, size);
        if (chunk_size == NULL) {
            fprintf(stderr, "ERROR: required header %s not found\n", HEADER_KEY_SIZE);
            return 1;
        }
        if (receive_from_server(fd, sid, atoi(chunk_size)) == EOF) {
            return 0;
        }
    }
}
