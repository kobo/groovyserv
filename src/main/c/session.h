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

#ifndef _SESSION_H
#define _SESSION_H

#define BUFFER_SIZE 512

#define MAX_HEADER_KEY_LEN 30
#define MAX_HEADER_VALUE_LEN 512
#define MAX_HEADER 10

struct header_t {
    char key[MAX_HEADER_KEY_LEN + 1];
    char value[MAX_HEADER_VALUE_LEN + 1];
};

int open_socket(char* server_name, int server_port);
void send_header(int fd, int argc, char** argv, char* cookie);
int read_headers(int fd, struct header_t headers[]);
int start_session(int fd);

#endif
