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

#include <string.h>
#include "base64.h"

void base64_encode(char* encoded, char* original)
{
    char *w = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    unsigned char *buff;
    int i = 0, x = 0, l = 0;

    buff = (char *)malloc((strlen(original) * 4 / 3 + 3) & ~0x03 + 1);

    for (; *original; original++) {
        x = x << 8 | *original;
        for (l += 8; l >= 6; l -= 6) {
            buff[i++] = w[(x >> (l - 6)) & 0x3f];
        }
    }
    if (l > 0) {
        x <<= 6 - l;
        buff[i++] = w[x & 0x3f];
    }
    for (; i % 4;) {
        buff[i++] = '=';
    }

    strcpy(encoded, buff);
    free(buff);
}

