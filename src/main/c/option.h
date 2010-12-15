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

#ifndef _OPTION_H
#define _OPTION_H

#include "bool.h"

#define MAX_MASK 10
#define CLIENT_OPTION_PREFIX "-C"
#define MATCH_ALL_PATTERN "*"

struct option_t {
    BOOL without_invocation_server;
    BOOL help;
    char* env_include_mask[MAX_MASK];
    char* env_exclude_mask[MAX_MASK];
};

struct option_param_t {
  char* name;
  char* value;
};

enum OPTION_TYPE {
  OPT_WITHOUT_INVOCATION_SERVER,
  OPT_HELP,
  OPT_ENV_INCLUDE_MASK,
  OPT_ENV_ALL,
  OPT_ENV_EXCLUDE_MASK,
};

struct option_info_t {
  char* name;
  enum OPTION_TYPE type;
  BOOL take_value;
};

extern struct option_t client_option;
extern struct option_info_t option_info[];

void usage();
BOOL is_client_option(char* s);
BOOL is_groovy_help_option(char* s);
BOOL is_option_name_valid_char(char c);
char* get_option_name_value(struct option_param_t* opt, char* arg);
void set_mask_option(char ** env_mask, char* value);
void option_formal_check(struct option_info_t* opt, struct option_param_t *param);
struct option_info_t* what_option(struct option_param_t* param);
void scan_options(struct option_t* option, int argc, char **argv);
void print_mask_option(char ** env_mask);
void print_options(struct option_t *opt);
void remove_client_options(int argc, char** argv);

#endif
