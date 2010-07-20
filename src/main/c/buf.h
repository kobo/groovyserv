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

#ifndef _BUF_H
#define _BUF_H

#include <stdarg.h>

#ifndef DEFAULT_BUFFER_SIZE
#define DEFAULT_BUFFER_SIZE 512
#else
#error  "DEFAULT_BUFFER_SIZE already defined somewhere"
#endif

typedef struct buf {
    int size;
    int buffer_size;
    char* buffer;
} buf;

int buf_strnlen(const char* const str, int n);
buf* buf_init(buf* buf, int size, const char* const initial);
buf buf_new(int size, const char* const initial);
void buf_delete(buf* buf);
buf* buf_offs_ncopy(buf* buf, int pos, const char* const str, int n);
buf* buf_strcopy(buf* buf, const char* const str);
buf* buf_nstrcopy(buf* buf, const char* const str, int n);
buf* buf_add(buf* buf, const char* const str);
buf* buf_offs_vprintf(buf* buf, int offs, const char* const fmt, va_list ap);
buf* buf_offs_printf(buf* buf, int offs, const char* const fmt, ...);
buf* buf_printf(buf* buf, const char* const fmt, ...);

#endif
