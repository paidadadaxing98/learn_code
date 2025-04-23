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

#include <isa.h>
#include <string.h>
/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include <stdio.h>
#include <assert.h>
#include <stdbool.h>
#include <stdlib.h>

enum {
  TK_NOTYPE = 256, TK_EQ,TK_NUM,TK_16NUM,TK_ERROR,TK_MOD,TK_REG,
  TK_INEQ,TK_AND,TK_OR,TK_DEREF,TK_NEG,

  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {"\\(", '('},  
  {"\\)", ')'},  
  {"\\+", '+'},       // plus
  {"-", '-'},
  {"\\*", '*'},
  {"\\%",'%'},  
  {"/ *0",TK_ERROR},
  {"/", '/'},  
  {" +", TK_NOTYPE},     // spaces        
  {"==", TK_EQ},   
  {"0[xX][0-9a-fA-F]+",TK_16NUM},     // equal
  {"[0-9]+",TK_NUM},   //number
  {"^\\$[0-9a-zA-Z]+",TK_REG},
  {"!=",TK_INEQ},
  {"\\&\\&",TK_AND},
  {"\\|\\|",TK_OR},
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[512] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  char* s_cp;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo - pmatch.rm_so;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        switch (rules[i].token_type) {
          case '(':tokens[nr_token++].type = '(';break;
          case ')':tokens[nr_token++].type = ')';break;
          case '+':tokens[nr_token++].type = '+';break;
          case '-':if(nr_token == 0 ||strchr("+-*/(",tokens[nr_token-1].type)){
                  tokens[nr_token++].type = TK_NEG;
          }else{
                  tokens[nr_token++].type = '-';
          }break;
          case '*':if(nr_token == 0 ||strchr("+-*/(",tokens[nr_token-1].type))
                  {
                    tokens[nr_token++].type = TK_DEREF;
                  }else{
                    tokens[nr_token++].type = '*';
                  }break;
          case '/':tokens[nr_token++].type = '/';break;
          case '%':tokens[nr_token++].type = '%';break;
          case TK_NOTYPE:break;  
          case TK_EQ:tokens[nr_token++].type = TK_EQ;break;
          case TK_INEQ:tokens[nr_token++].type = TK_INEQ;break;
          case TK_16NUM:tokens[nr_token].type = TK_16NUM;
                s_cp = strndup(substr_start, substr_len);
                strcpy(tokens[nr_token++].str,s_cp);break;
          case TK_NUM:
              tokens[nr_token].type = TK_NUM;
              s_cp = strndup(substr_start, substr_len);
              strcpy(tokens[nr_token++].str,s_cp);break;
          case TK_REG:tokens[nr_token].type =TK_REG;
              s_cp = strndup(substr_start, substr_len);
              strcpy(tokens[nr_token++].str,s_cp);break; 
          case TK_AND:tokens[nr_token++].type = TK_AND;break;
          case TK_OR:tokens[nr_token++].type = TK_OR;break;
          case TK_ERROR:
              printf("/0 error!\n");return 0;
          default: 
              printf("some tokens can't be matched !\n ");
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return 0;
    }
  }

  return 1;
}

//计算表达式

//括号匹配，是否需要去最外层括号

    bool is_paired(int p, int q) {
      int banlance = 0;
      for (int i = p; i <= q; i++) {
        if (tokens[i].type == '(') {
          banlance++;
        }
        else if (tokens[i].type == ')') {
          banlance--;
          if (banlance < 0) {
            return false;
          }
        }
      }
      //printf("balance:%d,position:%d - %d\n",banlance,p+1,q+1);
      return banlance == 0;
    }

  int check_parentheses(int p, int q) {
    if (tokens[p].type != '(' || tokens[q].type != ')') {
        return 0;
    }
    int balance = 0;
    // 检查（）+（）形式
    if(is_paired(p,q)){
      for (int i = p + 1; i < q; i++) {
        if (tokens[i].type == '(') balance++;
        else if (tokens[i].type == ')') balance--;
        if (balance < 0) return 0;
      }
      //printf("qu kuo hao:%d\n",balance==0);
      return balance == 0;
    }
    else {
      printf("Failed to match the 'parentheses\n");
      assert(0);
    }
    return 0;
}
    



// 寻找操作符,其实就是找优先级最低的位置
int find_op(int p, int q) {
  int op = -1;
  int priority = 6; 
  int balance = 0;
  for (int i = p; i <= q; i++) {
    if(tokens[i].type == '('){
      balance++;
    }
    else if(tokens[i].type == ')'){
      balance--;
    }
    else if ((tokens[i].type == '+' || tokens[i].type == '-') && balance==0) {
      if (priority >= 3) {
        op = i;
        priority = 3;
      }
    } 
    else if((tokens[i].type == TK_DEREF || tokens[i].type == TK_NEG) && balance==0){
      if (priority >= 5) {
        op = i;
        priority = 5;
    }
  }
    else if((tokens[i].type == '*' || tokens[i].type == '/' || tokens[i].type == '%') && balance==0 ) {
      if (priority >= 4) {
        op = i;
        priority = 4;
      }
    }
    else if((tokens[i].type == TK_EQ || tokens[i].type == TK_INEQ) && balance==0){
      if (priority >= 2) {
        op = i;
        priority = 2;
      }
    }
      else if((tokens[i].type == TK_AND) && balance==0){
        if (priority >= 1) {
          op = i;
          priority = 1;
        }
      }  
        else if((tokens[i].type == TK_OR) && balance==0){
          if (priority >= 0) {
            op = i;
            priority = 0;
          }
    }

  
  }
  //printf("op position:%d\n",op);
  return op;
  }

word_t vaddr_read(vaddr_t, int);

int eval(int p,int  q) {
  int32_t val,val1,val2;
  int8_t op;

    if (p > q) {
      //printf("Bad expression \n");
      return 0; 
    }
    else if (p == q) {
      if(tokens[p].type == TK_NUM){
        sscanf(tokens[p].str,"%d",&val);return val;
      }
      if(tokens[p].type == TK_16NUM){
        sscanf(tokens[p].str,"%x",&val);return val;
      }
      if(tokens[p].type == TK_REG){
        bool success[1];
        return isa_reg_str2val(tokens[p].str + 1, success);//去除$
      }
    }
    else if(check_parentheses(p, q) == 1){
      return eval(p + 1, q - 1);
    }
    else {
      //printf("find_op position:%d - %d",p,q);
      op = find_op(p,q);
      val1 = eval(p, op - 1);
      val2 = eval(op + 1, q);

      switch (tokens[op].type) {
        case TK_NEG: return -val2;
        case TK_DEREF:return vaddr_read(val2, 4);
        case '+': return val1 + val2;
        case '-': return val1 - val2;
        case '*': return val1 * val2;
        case '/': if(val2 == 0){
                  printf("position %d: /0 error\n",op+1);
                  return 0;
                  }
                  else{
                    return val1 / val2;
                  }
        case '%': return val1 % val2;
        case TK_EQ:return val1 == val2;
        case TK_INEQ:return val1 != val2;
        case TK_AND:return val1 && val2;
        case TK_OR:return val1 || val2;
        default:printf("type:%c %d\n",tokens[op].type,tokens[op].type);
              assert(0);
      }
    }
  return 0;
}

int expr(char *e, bool *success) {
  int value;

  *success = make_token(e);

  if (*success) {
    value = eval(0,nr_token-1);
    return value;
  }

  /* TODO: Insert codes to evaluate the expression. */
else {
    printf("tokens mismatch\n");
    return 0;
}

}