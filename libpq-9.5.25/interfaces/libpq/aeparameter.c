// Copyright (c) 2020-present,  INSPUR Co, Ltd.  All rights reserved.

#include "aeparameter.h"

#include "c.h"

void termAEParamers(aeParameters *param) {
  aeParameters *p = param, *next = NULL;

  if (p == NULL) return;

  do {
    next = p->next;
    free(p->key);
    free(p->value);
    p->oid = 0;
    p->next = NULL;
    free(p);
    p = next;
  } while (p != NULL);
}
