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

struct option_info_t option_info[] = {
    { "without-invoking-server", OPT_WITHOUT_INVOCATION_SERVER, FALSE },
    { "env", OPT_ENV, TRUE },
    { "env-all", OPT_ENV_ALL, FALSE },
    { "env-exclude", OPT_ENV_EXCLUDE, TRUE },
    { "help", OPT_HELP, FALSE },
    { "h", OPT_HELP, FALSE },
    { "", OPT_HELP, FALSE },
};

struct option_t client_option_values = {
    FALSE,
    FALSE,
    {}, // each array elements are expected to be filled with NULLs
    {}, // each array elements are expected to be filled with NULLs
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
           "options:\n" \
           "  %sh,%shelp                       Usage information of groovyclient options\n" \
           "  %senv <pattern>                  Pass the environment variables which name\n" \
           "                                   matches with the specified pattern. The values\n" \
           "                                   of matched variables on the client process are\n" \
           "                                   sent to the server process, and the values of\n"
           "                                   same name environment variable on the server\n" \
           "                                   are set to or overwitten by the passed values.\n" \
           "  %senv-all                        Pass all environment variables\n" \
           "  %senv-exclude <pattern>          Don't pass the environment variables which\n" \
           "                                   name matches with specified pattern\n" \
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           , CLIENT_OPTION_PREFIX
           );
}

static BOOL is_client_option(char* s)
{
    return strncmp(s, CLIENT_OPTION_PREFIX,
                   strlen(CLIENT_OPTION_PREFIX)) == 0;
}

static BOOL is_groovy_help_option(char* s)
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

static void set_mask_option(char ** env_mask, char* opt, char* value)
{
    char** p;
    for (p = env_mask; p-env_mask < MAX_MASK && *p != NULL; p++) {
        ;
    }
    if (p-env_mask == MAX_MASK) {
        fprintf(stderr, "ERROR: too many option: %s %s\n", opt, value);
        usage();
        exit(1);
    }
    *p = value;
}

static struct option_info_t* what_option(char* name)
{
    int j = 0;
    for (j=0; j<sizeof(option_info)/sizeof(struct option_info_t); j++) {
        if (strcmp(option_info[j].name, name) == 0) {
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
        if (is_groovy_help_option(argv[i])) {
            option->help = TRUE;
            return;
        }

        if (is_client_option(argv[i])) {
            char* name = argv[i]+strlen(CLIENT_OPTION_PREFIX);
            char* argvi_copy = argv[i];
            argv[i] = NULL;
            struct option_info_t* opt = what_option(name);

            if (opt == NULL) {
                fprintf(stderr, "ERROR: unknown option %s\n", argvi_copy);
                usage();
                exit(1);
            }

            char* value = NULL;
            if (opt->take_value == TRUE) {
                if (i >= argc-1) {
                    fprintf(stderr, "ERROR: option %s require param\n", argvi_copy);
                    usage();
                    exit(1);
                }
                i++;
                value = argv[i];
                argv[i] = NULL;
            }

            switch (opt->type) {
            case OPT_WITHOUT_INVOCATION_SERVER:
                option->without_invocation_server = TRUE;
                break;
            case OPT_HELP:
                usage();
                exit(1);
                break;
            case OPT_ENV:
                assert(opt->take_value == TRUE);
                set_mask_option(option->env_include_mask, name, value);
                break;
            case OPT_ENV_ALL:
                set_mask_option(option->env_include_mask, name, MATCH_ALL_PATTERN);
                break;
            case OPT_ENV_EXCLUDE:
                assert(opt->take_value == TRUE);
                set_mask_option(option->env_exclude_mask, name, value);
                break;
            }
        }
    }
}

static void print_mask_option(char ** env_mask)
{
    char** p;
    for (p = env_mask; p-env_mask < MAX_MASK && *p != NULL; p++) {
        printf("%s ", *p);
    }
}

void print_client_options(struct option_t *opt)
{
    printf("without_invocation_server = %d\n", opt->without_invocation_server);
    printf("env_include_mask = { ");
    print_mask_option(opt->env_include_mask);
    printf("}\n");
    printf("env_exclude_mask = { ");
    print_mask_option(opt->env_exclude_mask);
    printf("}\n");
}

