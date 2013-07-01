/*
 * Copyright 2009-2013 the original author or authors.
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

const int MASK_6BITS = 0x3f;
const char * const BASE64_DICT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

void base64_encode(char* encoded, unsigned char* original)
{
    int encoded_idx = 0;
    int work_buff = 0, wb_available_length = 0; // working buffer to handle characters as bit
    int dict_index;

    // encoding
    while (*original != 0) {
        // append a next character to working buffer
        work_buff <<= 8;
        work_buff |= *original;
        wb_available_length += 8;

        // consuming working buffer
        for (; wb_available_length >= 6; wb_available_length -= 6) {
            // upper 6 bits of working buffer
            dict_index = (work_buff >> (wb_available_length - 6)) & MASK_6BITS;

            // conv to a character
            encoded[encoded_idx++] = BASE64_DICT[dict_index];
        }
        original++;
    }
    if (wb_available_length > 0) {
        work_buff <<= (6 - wb_available_length);
        encoded[encoded_idx++] = BASE64_DICT[work_buff & MASK_6BITS];
    }

    // padding as a couple of 4 digits
    while (encoded_idx % 4 > 0) {
        encoded[encoded_idx++] = '=';
    }

    // terminate
    encoded[encoded_idx++] = '\0';
}

