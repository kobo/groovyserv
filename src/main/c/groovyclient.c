/*
 * Copyright 2009-2010 the original author or authors.
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

#include "buf.h"

#define DESTSERV "localhost"
#define DESTPORT 1961
#define BUFFER_SIZE 512

// request headers
const char * const HEADER_KEY_CURRENT_WORKING_DIR = "Cwd";
const char * const HEADER_KEY_ARG = "Arg";
const char * const HEADER_KEY_ENV = "Env";
const char * const HEADER_KEY_CP = "Cp";
const char * const HEADER_KEY_COOKIE = "Cookie";

// response headers
const char * const HEADER_KEY_CHANNEL = "Channel";
const char * const HEADER_KEY_SIZE = "Size";
const char * const HEADER_KEY_STATUS = "Status";

const char * const CLIENT_OPTION_PREFIX = "-C";

const int CR = 0x0d;
const int CANCEL = 0x18;

#define MAX_HEADER_KEY_LEN 30
#define MAX_HEADER_VALUE_LEN 512
#define MAX_HEADER 10

struct header_t {
    char key[MAX_HEADER_KEY_LEN + 1];
    char value[MAX_HEADER_VALUE_LEN + 1];
};

#ifdef WINDOWS
extern char __declspec(dllimport) **environ;
#else
extern char **environ;
#endif

#define TRUE 1
#define FALSE 0
#define BOOL int

#define MAX_MASK 10

struct option_t {
    BOOL without_invocation_server;
    char* env_include_mask[MAX_MASK];
    char* env_exclude_mask[MAX_MASK];
} client_option = {
    FALSE,
    {}, // NULL initialized
    {}, // NULL initialized
};

struct option_param_t {
  char* name;
  char* value;
};

enum OPTION_TYPE {
  OPT_WITHOUT_INVOCATION_SERVER,
  OPT_HELP,
  OPT_ENV_INCLUDE_MASK,
  OPT_ENV_EXCLUDE_MASK,
};

struct option_info_t {
  char* name;
  enum OPTION_TYPE type;
  BOOL take_value;
} option_info[] = {
  { "without-invoking-server", OPT_WITHOUT_INVOCATION_SERVER, FALSE },
  { "envin", OPT_ENV_INCLUDE_MASK, TRUE },
  { "envex", OPT_ENV_EXCLUDE_MASK, TRUE },
  { "help", OPT_HELP, FALSE },
  { "h", OPT_HELP, FALSE },
  { "", OPT_HELP, FALSE },
};



/*
 * Make socket and connect to the server (fixed to localhost).
 */
int open_socket(char* server_name, int server_port) {
    struct hostent *hostent;
    struct sockaddr_in server;

    hostent = gethostbyname(server_name); // lookup IP
    if (hostent == NULL) {
        perror("ERROR: gethostbyname");
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
        if (errno = ECONNREFUSED) {
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
BOOL mask_match(char* pattern, const char* str) {
    char* pos = strchr(str, '=');

	if (strcmp(pattern, "*") == 0) {
		return TRUE;
	}
    if (pos == NULL) {
        printf("ERROR: environment variable %s format invalid\n", str);
        exit(1);
    }
	*pos = '\0';
    char* p = strstr(pattern, "*");
    if (p != NULL) {
        *p = '\0';	/* treat "MASK*" as "MASK" */ // FIXME for wildcard matching.
    }
	BOOL result = strstr(str, pattern) != NULL;
	*pos = '='; // resume terminted NAME.
    return result;
}

BOOL masks_match(char** masks, char* str) {
	char** p;
	for (p = masks; p-masks < MAX_MASK && *p != NULL; p++) {
		if (mask_match(*p, str)) {
			return TRUE;
		}
	}
	return FALSE;
}

void make_env_headers(buf* read_buf, char** env, char** inc_mask, char** exc_mask) {
	int i;
	for (i = 1; env[i] != NULL; i++) {
		if (masks_match(client_option.env_include_mask, env[i])) {
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
void send_header(int fd, int argc, char** argv, char* cookie) {
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

    buf_printf(&read_buf, "%s: %s\n", HEADER_KEY_COOKIE, cookie);

    // send command line arguments.
    for (i = 1; i < argc; i++) {
        if (argv[i] != NULL) {
            buf_printf(&read_buf, "%s: %s\n", HEADER_KEY_ARG, argv[i]);
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
void read_header(char* buf, struct header_t* header) {
    //fprintf(stderr, "DEBUG: read_header: line: %s<LF> (size:%d)\n", buf, strlen(buf));

    // key
    char* p = strtok(buf, " :");
    if (p == NULL) {
        fprintf(stderr, "ERROR: key is NULL\n", p);
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
        if (isspace(p[i])) {
            // if invoked "groovyclient" without arguments, it should works
            // as error message command and print usage by delegated groovy command
            return;
        }
        if (!isalnum(p[i])) {
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
    while (isspace(*p)) { // ignore spaces
        p++;
    }
    if (strlen(p) > MAX_HEADER_VALUE_LEN) {
        fprintf(stderr, "ERROR: value of key \"%s\" is too long: %s\n", header->key, p);
        exit(1);
    }
    strncpy(header->value, p, MAX_HEADER_VALUE_LEN);

    //fprintf(stderr, "DEBUG: read_header: parsed: \"%s\"(size:%d) => \"%s\"(size:%d)\n", header->key, strlen(header->key), header->value, strlen(header->value));
}

char* read_line(int fd, char* buf, int size) {
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
int read_headers(int fd, struct header_t headers[]) {
    char read_buf[BUFFER_SIZE];
    int result = 0;
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
char* find_header(struct header_t headers[], const char* key, int nhdrs) {
    int i;
    for (i = 0; i < nhdrs; i++) {
        if (strcmp(headers[i].key, key) == 0) {
            return headers[i].value;
        }
    }
    return NULL;
}

/*
 * Receive a chunk, and write it to stdout or stderr.
 */
int split_socket_output(int soc, char* stream_identifier, int size) {
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

    static char* read_buf = NULL;
    static int read_buf_size = 0;
    if (read_buf == NULL) {
        read_buf = malloc(BUFFER_SIZE);
        read_buf_size = size;
    }
    if (read_buf_size < size) {
        while (read_buf_size < size) {
            read_buf_size *= 2;
        }
        read_buf = realloc(read_buf, read_buf_size);
    }
#ifdef WINDOWS
    int ret = recv(soc, read_buf, size, 0);
    assert(ret == size);
#else
    read(soc, read_buf, size);
#endif
    write(output_fd, read_buf, size);
    return 0;
}

/*
 * Copy data from stdin and send it to the server.
 */
int send_to_server(int fd) {
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

void copy_stdin_to_soc(int fd) {
    while (1) {
        send_to_server(fd);
    }
}

void invoke_thread(int fd) {
    DWORD id = 1;
    HANDLE hThread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) copy_stdin_to_soc, (LPVOID)fd, 0, &id);
}

int start_session(int fd) {
    struct header_t headers[MAX_HEADER];
    invoke_thread(fd);
    while (1) {
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
        if (split_socket_output(fd, sid, atoi(chunk_size)) == EOF) {
            return 0;
        }
    }
}

#else

/*
 * asynchronus input (select) with the stdin and the socket connection
 * to the server. copy input data from stdin to server, and
 * copy received data from the server to stdout/stderr.
 * destination of output is stdout or stderr are distinguished by
 * stream identifier(sid) header is 'out' or 'err'.
 */
int start_session(int fd) {
    struct header_t headers[MAX_HEADER];
    fd_set read_set;
    int ret;
    int stdin_closed = 0;
    while (1) {
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
        if (FD_ISSET(fd, &read_set)){ // socket
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
            if (split_socket_output(fd, sid, atoi(chunk_size)) == EOF) {
                return 0;
            }
        }
    }
}
#endif

char* scriptdir(char* result_dir, char* script_path) {
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

void start_server(char* script_path, int port) {
    fprintf(stderr, "starting server...\n");
    fflush(stderr);

    // resolve base directory
    char basedir_path[MAXPATHLEN];
    char* groovyserv_home = getenv("GROOVYSERV_HOME");
    if (groovyserv_home == NULL) {
        scriptdir(basedir_path, script_path);
    } else {
#ifdef WINDOWS
        sprintf(basedir_path, "%s\\bin\\", groovyserv_home);
#else
        sprintf(basedir_path, "%s/bin/", groovyserv_home);
#endif
    }
    //fprintf(stderr, "DEBUG: basedir_path: %s, %d\n", basedir_path, strlen(basedir_path));

    // make command line to invoke groovyserver
    char groovyserver_path[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(groovyserver_path, "%sgroovyserver.bat -p %d", basedir_path, port);
#else
    sprintf(groovyserver_path, "%sgroovyserver -p %d", basedir_path, port);
#endif
    //fprintf(stderr, "DEBUG: groovyserver_path: %s, %d\n", groovyserver_path, strlen(groovyserver_path));

    // start groovyserver.
    system(groovyserver_path);
}

/*
 * read authentication cookie. 
 */
void read_cookie(char* cookie, int size) {
    char path[MAXPATHLEN];
#ifdef WINDOWS
    sprintf(path, "%s\\.groovy\\groovyserv\\cookie", getenv("USERPROFILE"));
#else
    sprintf(path, "%s/.groovy/groovyserv/cookie", getenv("HOME"));
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

void remove_client_options(int argc, char** argv) {
    int i;
    for (i = 0; i < argc; i++) {
        if (strcmp(argv[i], "--without-invoking-server") == 0
            || strncmp(argv[i], CLIENT_OPTION_PREFIX,
                       strlen(CLIENT_OPTION_PREFIX)) == 0) {
            argv[i] = NULL;
        }
    }
}

int resolve_port() {
    int port = DESTPORT;
    char* port_str = getenv("GROOVYSERVER_PORT");
    if (port_str != NULL) {
        if (sscanf(port_str, "%d", &port) != 1) {
            fprintf(stderr, "ERROR: port format error\n");
            exit(1);
        }
    }
    return port;
}

int connect_server(char* argv0) {
    int fd;

    int port = resolve_port();
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

void usage() {
    printf("Usage:\n"													\
		   "groovyclient %s[options for client] [args/options for groovy command]\n" \
		   "  where [options for client] are:\n"						\
		   "    %sh        ... show help message.\n"					\
		   "    %senvin=MASK ... pass environment vars which matches with MASK.\n" \
		   "    %senvin=*    ... pass all environment vars.\n" \
		   "    %senvex=MASK ... don't pass environment vars which matches with MASK.\n" \
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   );

}

BOOL is_client_option(char* s) {
    return strncmp(s, CLIENT_OPTION_PREFIX,
				   strlen(CLIENT_OPTION_PREFIX)) == 0;
}

BOOL is_option_name_valid_char(c) {
    if (c == '\0') {
        return FALSE;
    }
    return isalnum(c) || strchr("_-", c) != NULL;
}

char* get_option_name_value(struct option_param_t* opt, char* arg) {
    char* p;
    for (p = arg; is_option_name_valid_char(*p); p++) {
        /*nothing*/
    }
    opt->name = arg;
    if (*p == '=') {
        *p = '\0';
        opt->value = p+1;
    }
    else if (*p == '\0') {
        opt->value = NULL;
    }
    else {
        fprintf(stderr, "ERROR: illeval option format %s\n", arg);
        usage();
        exit(1);
    }
}

void set_mask_option(char ** env_mask, char* value)
{
	char** p;
	for (p = env_mask; p-env_mask < MAX_MASK && *p != NULL; p++) {
		;
	}
	if (p-env_mask == MAX_MASK) {
		fprintf(stderr, "ERROR: too many mask option: %s\n", value);
		usage();
		exit(1);
	}
	*p = value;
}

void option_formal_check(struct option_info_t* opt, struct option_param_t *param) {
	if (param->value != NULL && opt->take_value==FALSE) {
		fprintf(stderr, "ERROR: option %s can't take param\n", param->name);
		exit(1);
	}
	else if (param->value == NULL && opt->take_value==TRUE) {
		fprintf(stderr, "ERROR: option %s require param\n", param->name);
		exit(1);
	}
}

struct option_info_t* what_option(struct option_param_t* param) {
	int j = 0;
	for (j=0; j<sizeof(option_info)/sizeof(struct option_info_t); j++) {
		if (strcmp(option_info[j].name, param->name) == 0) {
			option_formal_check(&option_info[j], param);
			return &option_info[j];
		}
	}
	return NULL;
}

void scan_options(struct option_t* option, int argc, char **argv) {
	int i;
	for (i = 1; i < argc; i++) {
		if (strcmp(argv[i], "--without-invoking-server") == 0) {
			option->without_invocation_server = TRUE;
			continue;
		}
		if (is_client_option(argv[i])) {
			struct option_param_t param;
			get_option_name_value(&param, argv[i]+strlen(CLIENT_OPTION_PREFIX));

			struct option_info_t* opt = what_option(&param);
			if (opt == NULL) {
				fprintf(stderr, "ERROR: unknown option %s\n", param.name);
				usage();
				exit(1);
			}
			switch (opt->type) {
			case OPT_WITHOUT_INVOCATION_SERVER:
				option->without_invocation_server = TRUE;
				break;
			case OPT_HELP:
				usage();
				exit(1);
				break;
			case OPT_ENV_INCLUDE_MASK:
				set_mask_option(option->env_include_mask, param.value);
				break;
			case OPT_ENV_EXCLUDE_MASK:
				set_mask_option(option->env_exclude_mask, param.value);
				break;
			}
		}
	}
}

void print_mask_option(char ** env_mask)
{
	char** p;
	for (p = env_mask; p-env_mask < MAX_MASK && *p != NULL; p++) {
		printf("%s ", *p);
	}
}

void print_options(struct option_t *opt) {
	printf("without_invocation_server = %d\n", opt->without_invocation_server);
	printf("env_include_mask = { ");
	print_mask_option(opt->env_include_mask);
	printf("}\n");
	printf("env_exclude_mask = { ");
	print_mask_option(opt->env_exclude_mask);
	printf("}\n");
}

/*
 * open socket and initiate session.
 */
int main(int argc, char** argv) {
#ifdef WINDOWS
    WSADATA wsadata;
    if (WSAStartup(MAKEWORD(1,1), &wsadata) == SOCKET_ERROR) {
        fprintf(stderr, "ERROR: creating socket");
        exit(1);
    }

    // make standard output to binary mode.
    if (_setmode(_fileno(stdout), _O_BINARY) < 0) {
        fprintf(stderr, "ERROR: setmode failed");
        exit(1);
    }
#endif

    scan_options(&client_option, argc, argv);

#ifdef DEBUG
	print_options(&client_option);
#endif
	
    remove_client_options(argc, argv);

    fd_soc = connect_server(argv[0]);

    signal(SIGINT, signal_handler); // using fd_soc in handler

    char cookie[BUFFER_SIZE];
    read_cookie(cookie, sizeof(cookie));

    send_header(fd_soc, argc, argv, cookie);
    int status = start_session(fd_soc);

#ifdef WINDOWS
    WSACleanup();
#endif

    return status;
}

