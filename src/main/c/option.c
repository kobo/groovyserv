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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <assert.h>

#include "option.h"
#include "bool.h"

struct option_t client_option = {
    FALSE,
    FALSE,
    {}, // each array elements are expected to be filled with NULLs
    {}, // each array elements are expected to be filled with NULLs
};

struct option_info_t option_info[] = {
    { "without-invoking-server", OPT_WITHOUT_INVOCATION_SERVER, FALSE },
    { "env", OPT_ENV_INCLUDE, TRUE },
    { "env-include", OPT_ENV_INCLUDE, TRUE },
    { "env-all", OPT_ENV_ALL, FALSE },
    { "env-exclude", OPT_ENV_EXCLUDE, TRUE },
    { "help", OPT_HELP, FALSE },
    { "h", OPT_HELP, FALSE },
    { "", OPT_HELP, FALSE },
};

static char *groovy_help_options[] = {
    "--help",
    "-help",
    "-h"
};

void usage()
{
    printf("\n"	
           "usage: groovyclient %s[option for groovyclient] [args/options for groovy]\n" \
           "options:\n"	\
		   "  %sh,%shelp                       Usage information of groovyclient options\n" \

		   "  %senv,%senv-include=<pattern>    Pass the environment variables which name\n" \
           "                                   matches with the specified pattern. The values\n" \
           "                                   of matched variables on the client process are\n" \
           "                                   sent to the server process, and the values of\n"
           "                                   same name environment variable on the server\n" \
           "                                   are set to or overwitten by the passed values. \n" \
		   "  %senv-all                        Pass all environment variables on client process\n" \
		   "  %senv-exclude=<pattern>          Don't pass the environment variables which\n" \
		   "                                   name matches with pattern\n" \
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   , CLIENT_OPTION_PREFIX
		   );

}

BOOL is_client_option(char* s)
{
    return strncmp(s, CLIENT_OPTION_PREFIX,
				   strlen(CLIENT_OPTION_PREFIX)) == 0;
}

BOOL is_groovy_help_option(char* s)
{
    char** p;
    for (p = groovy_help_options;
         (p-groovy_help_options) < sizeof(groovy_help_options)/sizeof(groovy_help_options[0]); p++) {
        if (strcmp(*p, s) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}

BOOL is_option_name_valid_char(char c)
{
    if (c == '\0') {
        return FALSE;
    }
    return isalnum(c) || strchr("_-", c) != NULL;
}

char* get_option_name_value(struct option_param_t* opt, char* arg)
{
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

void option_formal_check(struct option_info_t* opt, struct option_param_t *param)
{
	if (param->value != NULL && opt->take_value==FALSE) {
		fprintf(stderr, "ERROR: option %s can't take param\n", param->name);
		exit(1);
	}
	else if (param->value == NULL && opt->take_value==TRUE) {
		fprintf(stderr, "ERROR: option %s require param\n", param->name);
		exit(1);
	}
}

struct option_info_t* what_option(struct option_param_t* param)
{
	int j = 0;
	for (j=0; j<sizeof(option_info)/sizeof(struct option_info_t); j++) {
        if (strcmp(option_info[j].name, param->name) == 0) {
            option_formal_check(&option_info[j], param);
			return &option_info[j];
		}
	}
	return NULL;
}

void scan_options(struct option_t* option, int argc, char **argv)
{
	int i;
    if (argc <= 1) {
            option->help = TRUE;
            return;
    }
	for (i = 1; i < argc; i++) {
		if (strcmp(argv[i], "--without-invoking-server") == 0) {
			option->without_invocation_server = TRUE;
			continue;
		}

        if (is_groovy_help_option(argv[i])) {
            option->help = TRUE;
            return;
        }
		if (is_client_option(argv[i])) {

            struct option_param_t param;
            char* name_value = argv[i]+strlen(CLIENT_OPTION_PREFIX);
            
            get_option_name_value(&param, name_value);

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
			case OPT_ENV_INCLUDE:
				set_mask_option(option->env_include_mask, param.value);
				break;
			case OPT_ENV_ALL:
				set_mask_option(option->env_include_mask, MATCH_ALL_PATTERN);
				break;
			case OPT_ENV_EXCLUDE:
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

void print_options(struct option_t *opt)
{
	printf("without_invocation_server = %d\n", opt->without_invocation_server);
	printf("env_include_mask = { ");
	print_mask_option(opt->env_include_mask);
	printf("}\n");
	printf("env_exclude_mask = { ");
	print_mask_option(opt->env_exclude_mask);
	printf("}\n");
}

void remove_client_options(int argc, char** argv)
{
    int i;
    for (i = 0; i < argc; i++) {
        if (strcmp(argv[i], "--without-invoking-server") == 0
            || strncmp(argv[i], CLIENT_OPTION_PREFIX,
                       strlen(CLIENT_OPTION_PREFIX)) == 0) {
            argv[i] = NULL;
        }
    }
}
