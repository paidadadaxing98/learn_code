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
"  int result = %s; "
"  printf(\"%%d\", result); "
"  return 0; "
"}";

//generate a random number
static char* buf_p = buf;
static char* buf_out = buf;
static int not_0 = 0;
static int expr_len = 0;
static int last_expr = 0;
static int last_num = 0;
static void gen(char str){
  *buf_p = str;
  buf_p++;
  expr_len++;
}
static void gen_num(){
  int num;
  num = rand() % 10; //within 10
  *buf_p = num + '0';
  last_num = num;
  buf_p++;
  expr_len++;
}//0-9

static void gen_num_bigger(int n){
  int num;
  num = rand() % (10 - n); 
  *buf_p = num + '0' + n;
  buf_p++;
  expr_len++;
}//n-9

static void gen_num_small(int n){
  int num;
  num = rand() % n; 
  *buf_p = num + '0' + 1;
  buf_p++;
  expr_len++;
}//1-n

static void gen_op(){
  not_0 = 0;
  switch(rand()%8){
    case 0:gen('-');break;
    case 1:gen('-');break;
    case 2:gen('/');not_0=1;break;
    case 3:gen('*');break;
    case 4:gen('*');break;
    case 5:gen('+');break;
    default:gen('+');break;
  }
}

void gen_space(){
  if(rand() % 5 >= 3 ){
    gen(' ');
  }
}

 
static void gen_rand_expr(int deep) {

  if(deep > 4){
    if(not_0 == 0){
      gen_space();
      gen_num();
    }
    else{
      gen_space();
      gen_num_bigger(1);
    }
  }
  else{
  switch (rand()%3)
    {
    case 0:if(not_0 == 0){gen_num();}
            else{gen_num_bigger(1);}
            break;
    case 1:
      gen('(');gen_rand_expr(deep+1);gen(')');break;
    default:
      gen_rand_expr(deep+1);gen_space();gen_op();gen_space();gen_rand_expr(deep+1);break;
    }
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
    expr_len = 0;
    buf_out = buf_out + last_expr;    //reset
    gen_rand_expr(0);
    gen('\0');
    last_expr = expr_len;

    sprintf(code_buf, code_format, buf_out);

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

    printf("%d %s\n", result, buf_out);
  }
  return 0;
}