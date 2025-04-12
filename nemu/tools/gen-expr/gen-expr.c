/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// this should be enough
static char buf[65536] = {};
static char code_buf[65536 + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

//generate a random number
static char* buf_p = buf;
static int tk_i = 0;
static void gen(char str){
  *buf_p = str;
  buf_p++;
}
static void gen_num(){
  int num;
  num = rand() % 10; //within 10
  *buf_p = num + '0';
  buf_p++;
}

static void gen_op(){
  switch(rand()%3){
    case 0:*buf_p = '-';buf_p++;break;
    case 1:*buf_p = '*';buf_p++;break;
    case 2:*buf_p = '/';buf_p++;break;
    default:*buf_p = '+';buf_p++;break;
  }
}


 
static void gen_rand_expr() {

  switch (rand()%3)
  {
  case 0:gen_num();break;
  case 1:if(tk_i<3){
    gen('(');gen_rand_expr();gen(')');
  }
  tk_i++;break;
  default:if(tk_i<3){
    gen_rand_expr();gen_op();gen_rand_expr();
  }
  tk_i++;break;
  }
}

//finish

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;

  for (i = 0; i < loop; i ++) {
    buf_p = buf;    //reset
    gen_rand_expr();
    *buf_p = '\0';buf_p++;
    tk_i = 0;


    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}

