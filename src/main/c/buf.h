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
