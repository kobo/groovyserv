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
#define WINDOWS_WITHOUT_CYGWIN
#else
#define CYGWIN_OR_UNIX
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <sys/types.h> /* netinet/in.h */
#ifdef WINDOWS_WITHOUT_CYGWIN
#include <windows.h>
#include <winsock2.h>
#include <process.h>
#include <sys/fcntl.h>
#else
#include <sys/socket.h> /* AF_INET */
#include <netinet/in.h> /* sockaddr_in */
#include <netdb.h> /* gethostbyname */
#include <sys/uio.h>
#include <sys/errno.h>
#endif

#include <sys/param.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>

#if defined(__CYGWIN__)
#include <sys/cygwin.h>
#include <w32api/windef.h>
#endif

#define DESTSERV "localhost"
#define DESTPORT 1961
#define MAX_INT_WIDTH 10 /* assume int size is 32bit */
#define BUFFER_SIZE 512

/* request headers */
const char * const HEADER_KEY_CURRENT_WORKING_DIR = "Cwd";
const char * const HEADER_KEY_ARG = "Arg";
const char * const HEADER_KEY_CP = "Cp";
const char * const HEADER_KEY_COOKIE = "Cookie";

/* response headers */
const char * const HEADER_KEY_CHANNEL = "Channel";
const char * const HEADER_KEY_SIZE = "Size";
const char * const HEADER_KEY_STATUS = "Status";

const int CR = 0x0d;
const int LF = 0x0a;

#define MAX_HEADER_KEY_LEN 30
#define MAX_HEADER_VALUE_LEN 512
#define MAX_HEADER 10

#define SERVER_NOT_RUNNING 15

struct header_t {
  char key[MAX_HEADER_KEY_LEN+1];
  char value[MAX_HEADER_VALUE_LEN+1];
};

/*
 * open_socket.
 * Make socket and connect to the server (fixed to localhost).
 */
int open_socket(char* server_name, int server_port) {
  struct hostent *hostent;
  struct sockaddr_in server;

  hostent = gethostbyname(server_name); /* lookup IP */
  if (hostent == NULL ) {
    perror("gethostbyname");
    exit(1);
  }

  memset(&server, 0, sizeof(server)); /* zero clear struct */

  server.sin_family = PF_INET;  /* server.sin_addr = hostent->h_addr */
  memcpy(&server.sin_addr, hostent->h_addr, hostent->h_length);
  server.sin_port = htons(server_port);

  int fd;
#ifdef WINDOWS_WITHOUT_CYGWIN
  if ((fd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP))  == INVALID_SOCKET) {
#else
  if ((fd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0) {
#endif
    perror("socket");
    exit(1);
  }

#ifdef WINDOWS_WITHOUT_CYGWIN
  if (connect(fd, (struct sockaddr *)&server, sizeof(server)) == SOCKET_ERROR) {
    perror("connect");
    exit(1);
  }
#else
  if (connect(fd, (struct sockaddr *)&server, sizeof(server)) < 0) {
    if (errno = ECONNREFUSED) {
      return -1;
    }
    perror("connect");
    exit(1);
  }
#endif
  return fd;
}

/*
 * send_header.
 * Send header information which includes current working direcotry,
 * command line arguments, and CLASSPATH environment variable
 * to the server.
 */
void send_header(int fd, int argn, char** argv, char* cookie) {
  char read_buf[BUFFER_SIZE];
  char* p = read_buf;
  int i;

  // send current working directory.
  p += sprintf(p, "%s: ", HEADER_KEY_CURRENT_WORKING_DIR);
  char* cwd = getcwd(p, MAXPATHLEN);
  if (cwd == NULL) {
    perror("getcwd");
    exit(1);
  }

#if defined(__CYGWIN__)
  cygwin_conv_to_win32_path(cwd, p);
#endif

  p += strlen(cwd);
  *p++ = '\n';

  p += sprintf(p, "%s: %s\n", HEADER_KEY_COOKIE, cookie);

  // send command line arguments.
  for (i=1; i<argn; i++) {
	p+= sprintf(p, "%s: %s\n", HEADER_KEY_ARG, argv[i]);
    // TODO: check buffer overrrun
  }

  char* cp = getenv("CLASSPATH");
  if (cp != NULL && *cp != '\0') {
    p += sprintf(p, "%s: %s\n", HEADER_KEY_CP, cp);
    // TODO: check buffer overrrun
  }

  *p++ = '\n';
  if (p - read_buf > BUFFER_SIZE) {
    fprintf(stderr, "\nheader size too big\n");
    exit(1);
  }
#ifdef WINDOWS_WITHOUT_CYGWIN
  send(fd, read_buf, p-read_buf, 0);
#else
  write(fd, read_buf, p-read_buf);
#endif
}

/*
 * parse server response header.
 */
void read_header(char* buf, struct header_t* header) {
  char* p = strtok(buf, " :");
  if (strlen(p) > MAX_HEADER_KEY_LEN) {
    fprintf(stderr, "\nkey %s too long\n", p);
    exit(1);
  }
  strncpy(header->key, p, MAX_HEADER_KEY_LEN);

  p = strtok(NULL, " :\n");
  while (isspace(*p)) {
    p++;
  }
  if (*p == '\n' || *p == '\0') {
    fprintf(stderr, "\nformat error\n");
    exit(1);
  }
  if (strlen(p) > MAX_HEADER_VALUE_LEN) {
    fprintf(stderr, "\nkey %s too long\n", p);
    exit(1);
  }

  strncpy(header->value, p, MAX_HEADER_VALUE_LEN);
}

char* read_line(int fd, char* buf, int size) {
   int i;
   for (i=0; i<size; i++) {
#ifdef WINDOWS_WITHOUT_CYGWIN
	 int ret = recv(fd, buf+i, 1, 0);
	 //	 int ret = recv(fd, buf+i, 100, 0);
	 if (ret == -1) {
	   printf("error : %d\n", WSAGetLastError());
	   exit(1);
	 }
	 assert(ret == 1);
#else
	 read(fd, buf+i, 1);
#endif
	 if (buf[i] == '\n') {
	   return buf;
	 }
   }
   return buf;
 }

/*
 * read server response headers.
 */
int read_headers(int fd, struct header_t headers[], int header_buf_size) {
  char read_buf[BUFFER_SIZE];
  int result = 0;
  char *p;
  int pos = 0;
  while (1) {
    p = read_line(fd, read_buf, BUFFER_SIZE);
    if (p == NULL) {
      return 0;
    }
    if (*p == CR) {
      p++;
    }
    if (*p == '\n') {
      break;
    }
    read_header(read_buf, headers+pos);
    if (++pos >= header_buf_size) {
      fprintf(stderr, "\ntoo many headers\n");
      exit(1);
    }
  }

  return pos;
}

/*
 * find header value.
 */
char* find_header(struct header_t headers[], const char* key, int nhdrs) {
  int i;
  for (i = 0; i<nhdrs; i++) {
    if (strcmp(headers[i].key, key) == 0) {
      return headers[i].value;
    }
  }
  return NULL;
}

/*
 * split_socket_output.
 * Receive a chunk, and write it to stdout or stderr.
 */
int split_socket_output(int soc, char* stream_identifier, int size) {
  int output_fd;
  if (strcmp(stream_identifier, "out") == 0) {
    output_fd = 1; /* stdout */
  }
  else if (strcmp(stream_identifier, "err") == 0) {
    output_fd = 2; /* stderr */
  }
  else {
    fprintf(stderr, "\nunrecognizable stream identifier: %s.\n", stream_identifier);
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
#ifdef WINDOWS_WITHOUT_CYGWIN
  int ret = recv(soc, read_buf, size, 0);
  assert(ret == size);
#else
  read(soc, read_buf, size);
#endif
  write(output_fd, read_buf, size);
  return 0;
}

/*
 * send_to_server.
 * Copy data from stdin and send it to the server.
 */
int send_to_server(int fd)
{
  char read_buf[BUFFER_SIZE];
  int ret;

  if ((ret = read(0, read_buf, BUFFER_SIZE)) == -1){ // TODO buffering
    perror("read failure from stdin");
    exit(1);
  }
  read_buf[ret] = '\0';

  char write_buf[BUFFER_SIZE];
  sprintf(write_buf, "Size: %d\n\n", ret); // TODO: check size

#ifdef WINDOWS_WITHOUT_CYGWIN
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

#ifdef WINDOWS_WITHOUT_CYGWIN

void copy_stdin_to_soc(int fd) {
  int ch;
  while (1) {
	send_to_server(fd);
  }
}

void invoke_thread(int fd) {
  DWORD id = 1;
  HANDLE hThread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)
								copy_stdin_to_soc, (LPVOID)fd, 0, &id);
}

int session(int fd)
{
  struct header_t headers[MAX_HEADER];

  invoke_thread(fd);

  while (1) {
	int size = read_headers(fd, headers, MAX_HEADER);
	if (size == 0) {
	  continue;
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
	  fprintf(stderr, "\nrequired header %s not found\n", HEADER_KEY_CHANNEL);
	  return 1;
	}
	char* chunk_size = find_header(headers, HEADER_KEY_SIZE, size);
	if (chunk_size == NULL) {
	  fprintf(stderr, "\nrequired header %s not found\n", HEADER_KEY_SIZE);
	  return 1;
	}
	if (split_socket_output(fd, sid, atoi(chunk_size)) == EOF) {
	  return 0;
	}
  }
}

#else
/*
 * session.
 * asynchronus input (select) with the stdin and the socket connection
 * to the server. copy input data from stdin to server, and
 * copy received data from the server to stdout/stderr.
 * destination of output is stdout or stderr are distinguished by
 * stream identifier(sid) header is 'out' or 'err'.
 */
int session(int fd)
{
  fd_set read_set;
  int ret;
  int stdin_closed = 0;

  while (1) {
    /* initialize the set of file descriptor */
    FD_ZERO(&read_set);

    // watch stdin of client and socket.
    FD_SET(0, &read_set);
    FD_SET(fd, &read_set);

    if ((ret = select(FD_SETSIZE, &read_set, (fd_set*)NULL,
                      (fd_set*)NULL, NULL)) == -1) {
      perror("select failure");
      exit(1);
    }

    if (ret != 0) { /* detect changed descriptor */
      if (!stdin_closed && FD_ISSET(0, &read_set)) { /* stdin */
        stdin_closed = send_to_server(fd);
      }
      if (FD_ISSET(fd, &read_set)){ /* socket */
        struct header_t headers[MAX_HEADER];
        int size = read_headers(fd, headers, MAX_HEADER);
        if (size == 0) {
          continue;
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
          fprintf(stderr, "\nrequired header %s not found\n", HEADER_KEY_CHANNEL);
          return 1;
        }

        char* chunk_size = find_header(headers, HEADER_KEY_SIZE, size);
        if (chunk_size == NULL) {
          fprintf(stderr, "\nrequired header %s not found\n", HEADER_KEY_SIZE);
          return 1;
        }
        if (split_socket_output(fd, sid, atoi(chunk_size)) == EOF) {
          return 0;
        }
      }
    }
    else {
      fprintf(stderr, "\ntimeout?\n");
    }
  }
}
#endif

static int fd_soc;

static void signal_handler(int sig) {
#ifdef WINDOWS_WITHOUT_CYGWIN
  send(fd_soc, "Size: -1\n\n", 9, 0);
  closesocket(fd_soc);
#else
  write(fd_soc, "Size: -1\n\n", 9);
  close(fd_soc);
#endif
  exit(1);
}

void mk_dir(const char* path) {
  struct stat buf;
  if (stat(path, &buf) == -1) {
#ifndef WINDOWS_WITHOUT_CYGWIN
	// TODO: create folder for mingw
    if (errno == ENOENT) {
      char cmdbuf[strlen(path) + 7];
      sprintf(cmdbuf, "mkdir %s", path);
      system(cmdbuf);
      return;
    }
#endif

    perror("stat");
    exit(1);
  }
  if (!S_ISDIR(buf.st_mode)) {
    fprintf(stderr, "\npath %s is not directory.", path);
    exit(1);
  }
}

void start_server(int argn, char** argv, int port) {
  // create directries for logging.
  /*
  char path[MAXPATHLEN];
  sprintf(path, "%s/%s", getenv("HOME"), ".groovy");
  mk_dir(path);
  sprintf(path, "%s/%s", getenv("HOME"), ".groovy/groovyserver");
  mk_dir(path);
  */

  // make command line to invoke groovyserver
  char groovyserver_path[MAXPATHLEN];
  strcpy(groovyserver_path, argv[0]);
  char* p = groovyserver_path + strlen(groovyserver_path);
  while (p > groovyserver_path && *p != '/') {
    p--;
  }
  if (*p == '/') {
    p++;
  }
  sprintf(p, "groovyserver -p %d", port);
  //  strcat(p, " >> ~/.groovy/groovyserver/groovyserver.log 2>&1");

  // start groovyserver.
  system(groovyserver_path);
#ifdef WINDOWS_WITHOUT_CYGWIN
  Sleep(3000);
#else
  sleep(3);
#endif
}

/*
 * read authentication cookie. 
 */
void read_cookie(char* cookie, int size) {
  char path[MAXPATHLEN];
  sprintf(path, "%s/%s", getenv("HOME"), ".groovy/groovyserv/cookie");
  FILE* fp = fopen(path, "r");
  if (fp != NULL) {
	if (fgets(cookie, size, fp) == NULL) {
	  perror("fgets");
	  exit(1);
	}
	fclose(fp);
  }
  else {
	fprintf(stderr, "cannot open cookie file\n");
	exit(1);
  }
}

/*
 * main.
 * open socket and initiate session.
 */
int main(int argn, char** argv) {
  signal(SIGINT, signal_handler);
  char cookie[BUFFER_SIZE];

  int port = DESTPORT;
  char* port_str = getenv("GROOVYSERVER_PORT");
  if (port_str != NULL) {
	if (sscanf(port_str, "%d", &port) != 1) {
	  fprintf(stderr, "port format error\n");
	  exit(1);
	}
  }

#ifdef WINDOWS_WITHOUT_CYGWIN
  WSADATA wsadata;
  if (WSAStartup(MAKEWORD(1,1), &wsadata) == SOCKET_ERROR) {
	printf("Error creating socket.");
	exit(1);
  }

  // make standard output to binary mode.
  if (_setmode( _fileno(stdout), _O_BINARY) < 0) {
	printf("setmode failed.");
	exit(1);
  }
#endif

  while ((fd_soc = open_socket(DESTSERV, port)) == -1) {
    fprintf(stderr, "starting server..\n");
    start_server(argn, argv, port);
  }
  read_cookie(cookie, sizeof(cookie));

  send_header(fd_soc, argn, argv, cookie);
  int status = session(fd_soc);

#ifdef WINDOWS_WITHOUT_CYGWIN
  WSACleanup();
#endif

  exit(status);
}
