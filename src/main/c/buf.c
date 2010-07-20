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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <stdarg.h>

#include "buf.h"

int buf_strnlen(const char* const str, int n) {
    int result = 0;
    const char* p;
    for (p = str; *p; p++) {
        result++;
        if (result > n) {
            return n;
        }
    }
    return result;
}

buf* buf_init(buf* buf, int size, const char* const initial) {
    assert(buf != NULL);
    if (size == 0) {
        size = DEFAULT_BUFFER_SIZE;
    }
    buf->size = 0;
    buf->buffer_size = size;
    buf->buffer = malloc(size * sizeof(char));
    if (buf->buffer == NULL) {
        fprintf(stderr, "\nERROR: malloc error\n");
        exit(1);
    }
    if (initial != NULL) {
        buf_strcopy(buf, initial);
    }
    return buf;
}

buf buf_new(int size, const char* const initial) {
    buf result;
    buf_init(&result, size, initial);
    return result;
}

void buf_delete(buf* buf) {
    assert(buf != NULL);
    free(buf->buffer);
    buf->size = 0;
    buf->buffer = NULL;
    buf->buffer_size = 0;
}

buf* buf_offs_ncopy(buf* buf, int offs, const char* const str, int n) {
    assert(buf != NULL);
    assert(str != NULL);
    assert(offs >= 0);
    assert(n >= 0);
    while (offs + n > buf->buffer_size) {
        buf->buffer_size *= 2;
    }
    buf->buffer = realloc(buf->buffer, buf->buffer_size);
    strncpy(buf->buffer + offs, str, n);
    buf->size = offs + n;
    return buf;
}

buf* buf_strcopy(buf* buf, const char* const str) {
    assert(str != NULL);
    return buf_offs_ncopy(buf, 0, str, strlen(str) + 1);
}


buf* buf_nstrcopy(buf* buf, const char* const str, int n) {
    assert(str != NULL);
    return buf_offs_ncopy(buf, 0, str, n);
}

buf* buf_add(buf* buf, const char* const str) {
    assert(str != NULL);
    return buf_offs_ncopy(buf, buf_strnlen(buf->buffer, buf->size), str, strlen(str) + 1);
}

buf* buf_offs_vprintf(buf* buf, int offs, const char* const fmt, va_list vlist) {
    va_list ap;
    int rest;
    int printf_output_size;
    rest = buf->buffer_size - offs;
    va_copy(ap, vlist);
    while ((printf_output_size = vsnprintf(buf->buffer + offs, rest, fmt, ap)) >= rest) {
        buf->buffer_size *= 2;
        buf->buffer = realloc(buf->buffer, buf->buffer_size);
        rest = buf->buffer_size - offs;
        va_copy(ap, vlist);
    }
    buf->size = offs + printf_output_size + 1;
    return buf;
}

buf* buf_offs_printf(buf* b, int offs, const char* const fmt, ...) {
    buf* result;
    va_list ap;
    va_start(ap, fmt);
    result = buf_offs_vprintf(b, offs, fmt, ap);
    va_end(ap);
    return result;
}

buf* buf_printf(buf* b, const char* const fmt, ...) {
    buf* result;
    va_list ap;
    va_start(ap, fmt);
    result = buf_offs_vprintf(b, buf_strnlen(b->buffer, b->size), fmt, ap);
    va_end(ap);
    return result;
}

