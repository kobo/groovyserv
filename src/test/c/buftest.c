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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>

#include "buf.h"

void test_buf_new() {
  buf b = buf_new(0, NULL);
  assert(b.buffer_size == DEFAULT_BUFFER_SIZE);
  buf_delete(&b);
}

void test_offs_ncopy() {
  buf b = buf_new(4, NULL);
  buf_offs_ncopy(&b, 0, "abc", 4);
  assert(memcmp(b.buffer, "abc\0", 4) == 0);
  assert(b.buffer_size == 4);
  assert(b.size == 4); /* a,b,c + \0 */
  buf_delete(&b);
}

void test_offs_ncopy2() {
  buf b = buf_new(10, NULL);
  buf_offs_ncopy(&b, 0, "abc", 3);
  assert(memcmp(b.buffer, "abc\0", 4) == 0); /* extra \0 added */
  assert(b.buffer_size == 10);
  assert(b.size == 3);  /* still size == 3 (a,b,c) */
  buf_delete(&b);
}

void test_offs_ncopy3() {
  buf b = buf_new(3, NULL);
  buf_offs_ncopy(&b, 0, "abc", 3);
  assert(memcmp(b.buffer, "abc\0", 4) == 0);
  assert(b.buffer_size == 3); /* auto extended */
  assert(b.size == 3);  /* still size == 3 (a,b,c) */
  buf_delete(&b);
}

void test_offs_ncopy4() {
  buf b = buf_new(10, NULL);
  buf_offs_ncopy(&b, 0, "abcd", 3);
  assert(memcmp(b.buffer, "abc\0", 3) == 0); /* extra \0 added */
  assert(b.buffer_size == 10);
  assert(b.size == 3);
  buf_delete(&b);
}

/*
 * 0 1 2 3 4
 * ? a b c \0
 */
void test_offs_ncopy5() {
  buf b = buf_new(10, NULL);
  buf_offs_ncopy(&b, 1, "abcd", 3);
  assert(memcmp(b.buffer+1, "abc\0", 3) == 0); /* extra \0 added */
  assert(b.buffer_size == 10);
  assert(b.size == 4);
  buf_delete(&b);
}

/*
 * 0 1 2 3 4 5 6
 * ? ? a b c d \0
 */
void test_offs_ncopy6() {
  buf b = buf_new(10, NULL);
  buf_offs_ncopy(&b, 2, "abcd", 5);
  assert(memcmp(b.buffer+2, "abcd\0", 5) == 0);
  assert(b.buffer_size == 10);
  assert(b.size == 7);
  buf_delete(&b);
}

void test_offs_ncopy_extend() {
  buf b = buf_new(3, NULL);
  buf_offs_ncopy(&b, 0, "abcd", 4);
  assert(memcmp(b.buffer, "abcd", 4) == 0);
  assert(b.size == 4);
  assert(b.buffer_size == 6);
  buf_delete(&b);
}

void test_offs_ncopy_extend2() {
  buf b = buf_new(3, NULL);
  buf_offs_ncopy(&b, 0, "abcdefg", 7);
  assert(memcmp(b.buffer, "abcdefg\0", 8) == 0);
  assert(b.size == 7);
  assert(b.buffer_size == 12);
  buf_delete(&b);
}

void test_offs_ncopy_extend3() {
  buf b = buf_new(4, NULL);
  buf_offs_ncopy(&b, 0, "abcdefg", 7);
  assert(memcmp(b.buffer, "abcdefg\0", 8) == 0);
  assert(b.buffer_size == 8);
  assert(b.size == 7);
  buf_delete(&b);
}

/*
 * 0 1 2 3 4 5 6 7 8 9
 * ? ? ? ? ? a b c d \0
 */
void test_offs_ncopy_extend4() {
  buf b = buf_new(2, NULL);
  buf_offs_ncopy(&b, 5, "abcd", 5);
  assert(memcmp(b.buffer+5, "abcd\0", 5) == 0);
  assert(b.buffer_size == 16);
  assert(b.size == 10);
  buf_delete(&b);
}

void test_copy() {
  buf b = buf_new(0, NULL);
  buf_strcopy(&b, "abcde");
  assert(memcmp(b.buffer, "abcde\0", 6) == 0);
  assert(b.buffer_size == DEFAULT_BUFFER_SIZE);
  assert(b.size == 6);
  buf_delete(&b);
}

void test_copy2() {
  buf b = buf_new(2, NULL);
  buf_strcopy(&b, "abcde");
  assert(memcmp(b.buffer, "abcde", 5) == 0);
  assert(b.buffer_size == 8);
  assert(b.size == 6);
  buf_delete(&b);
}

void test_ncopy() {
  buf b = buf_new(0, NULL);
  buf_nstrcopy(&b, "abcde", 6);
  assert(memcmp(b.buffer, "abcde\0", 6) == 0);
  assert(b.buffer_size == DEFAULT_BUFFER_SIZE);
  assert(b.size == 6);
  buf_delete(&b);
}

void test_ncopy2() {
  buf b = buf_new(2, NULL);
  buf_nstrcopy(&b, "abcde", 5);
  assert(memcmp(b.buffer, "abcde", 5) == 0);
  assert(b.buffer_size == 8);
  assert(b.size == 5);
  buf_delete(&b);
}

void test_add() {
  buf b = buf_new(0, NULL);
  buf_add(&b, "abcde");
  assert(memcmp(b.buffer, "abcde\0", 6) == 0);
  buf_add(&b, "fghij");
  assert(memcmp(b.buffer, "abcdefghij\0", 11) == 0);
  assert(b.buffer_size == DEFAULT_BUFFER_SIZE);
  assert(b.size == 11);
  buf_delete(&b);
}

void test_add_extend() {
  buf b = buf_new(2, NULL);
  buf_add(&b, "abcde");
  assert(memcmp(b.buffer, "abcde\0", 6) == 0);
  buf_add(&b, "fghij");
  assert(memcmp(b.buffer, "abcdefghij\0", 11) == 0);
  assert(b.buffer_size == 16);
  assert(b.size == 11);
  buf_delete(&b);
}

void test_buf_printf() {
  buf b = buf_new(2, NULL);
  buf_printf(&b, "abcde");
  assert(memcmp(b.buffer, "abcde\0", 6) == 0);
  buf_printf(&b, "fghij");
  printf("%s\n", b.buffer);
  assert(memcmp(b.buffer, "abcdefghij\0", 11) == 0);
  assert(b.buffer_size == 16);
  printf("%d\n", b.size);
  assert(b.size == 11);
  buf_delete(&b);
}

void test_buf_printf2() {
  buf b = buf_new(2, NULL);
  buf_printf(&b, "[%d, %02x]", 10, 10);
  assert(memcmp(b.buffer, "[10, 0a]\0", 9) == 0);
  buf_printf(&b, "[%s, %5s]", "abc", "def");
  assert(memcmp(b.buffer, "[10, 0a][abc,   def]\0", 21) == 0);
  assert(b.buffer_size == 32);
  assert(b.size == 21);
  buf_delete(&b);
}

void test_buf_strnlen() {
  assert(buf_strnlen("", 0) == 0);
  assert(buf_strnlen("a", 0) == 0);
  assert(buf_strnlen("ab", 0) == 0);
  assert(buf_strnlen("abc", 0) == 0);
  assert(buf_strnlen("abcd", 0) == 0);
  assert(buf_strnlen("", 1) == 0);
  assert(buf_strnlen("a", 1) == 1);
  assert(buf_strnlen("ab", 1) == 1);
  assert(buf_strnlen("abc", 1) == 1);
  assert(buf_strnlen("abcd", 1) == 1);
  assert(buf_strnlen("", 2) == 0);
  assert(buf_strnlen("a", 2) == 1);
  assert(buf_strnlen("ab", 2) == 2);
  assert(buf_strnlen("abc", 2) == 2);
  assert(buf_strnlen("abcd", 2) == 2);
  assert(buf_strnlen("", 3) == 0);
  assert(buf_strnlen("a", 3) == 1);
  assert(buf_strnlen("ab", 3) == 2);
  assert(buf_strnlen("abc", 3) == 3);
  assert(buf_strnlen("abcd", 3) == 3);
  assert(buf_strnlen("", 4) == 0);
  assert(buf_strnlen("a", 4) == 1);
  assert(buf_strnlen("ab", 4) == 2);
  assert(buf_strnlen("abc", 4) == 3);
  assert(buf_strnlen("abcd", 4) == 4);
  assert(buf_strnlen("", 5) == 0);
  assert(buf_strnlen("a", 5) == 1);
  assert(buf_strnlen("ab", 5) == 2);
  assert(buf_strnlen("abc", 5) == 3);
  assert(buf_strnlen("abcd", 5) == 4);
}

int main(int argc, char** argv) {
  test_buf_strnlen();
  test_buf_new();
  test_offs_ncopy();
  test_offs_ncopy2();
  test_offs_ncopy3();
  test_offs_ncopy4();
  test_offs_ncopy5();
  test_offs_ncopy6();
  test_offs_ncopy_extend();
  test_offs_ncopy_extend2();
  test_offs_ncopy_extend3();
  test_offs_ncopy_extend4();
  test_copy();
  test_copy2();
  test_ncopy();
  test_ncopy2();
  test_add();
  test_add_extend();
  test_buf_printf();
  test_buf_printf2();

  return 0;
}

