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
  TK_NOTYPE = 256, TK_EQ,TK_NUM,TK_16NUM,TK_ERROR 

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
  {"/ *0",TK_ERROR},
  {"/", '/'},  
  {" +", TK_NOTYPE},     // spaces        
  {"==", TK_EQ},      // equal
  {"[0-9]+",TK_NUM},   //number
  {"^0[xX][0-9a-fA-F]+$",TK_16NUM},

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

static Token tokens[32] __attribute__((used)) = {};
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
          case '-':tokens[nr_token++].type = '-';break;
          case '*':tokens[nr_token++].type = '*';break;
          case '/':tokens[nr_token++].type = '/';break;
          case TK_NOTYPE:break;  
          case TK_EQ:break; //'=='待完善
          case TK_NUM:
              tokens[nr_token].type = TK_NUM;
              s_cp = strndup(substr_start, substr_len);
              strcpy(tokens[nr_token++].str,s_cp);break;
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
#define STACK_MAX 32
  typedef struct {
    Token base[STACK_MAX];
    Token* top;
  }token_stack;
  void stack_init(token_stack *s){
    s->top = s->base;
  }
  void push(token_stack *s,Token t){
    if(s->top - s->base < STACK_MAX*sizeof(Token) ){
      *(s->top) = t;
      (s->top)++;   
    }
    else
      printf("stack full\n");
  }
  void pop(token_stack *s,Token *t){
    if(s->top > s->base){
      (s->top)--;
      *t = *(s->top);
    }
    else
      printf("stack empty!\n");
  }


  int check_parentheses(int p, int q) {
    Token popped;
    token_stack s;
    stack_init(&s);


    if (tokens[p].type != '(' || tokens[q].type != ')') {
        return 0;
    }

    for (int i = p; i <= q; i++) {
        if (tokens[i].type == '(') {
            push(&s, tokens[i]);
        } 
        else if (tokens[i].type == ')') {
            // 遇到右括号时栈为空说明不匹配
            if (s.top == s.base) {
                return 0;
            }
            pop(&s, &popped); 
        }
    }

    if (s.top != s.base) {
      printf("bracket mismatched\n");
      exit(0);
    }
    
    // 检查（）+（）形式
    int balance = 0;
    for (int i = p + 1; i < q; i++) {
        if (tokens[i].type == '(') balance++;
        else if (tokens[i].type == ')') balance--;
        if (balance < 0) break;
        }


    return (balance == 0);
}
    




// 寻找操作符,其实就是找优先级最低的位置
int find_op(int p, int q) {
  int op = -1;
  int priority = 3; 

  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '+' || tokens[i].type == '-') {
      if (priority > 1) {
        op = i;
        priority = 1;
      }
    } else if (tokens[i].type == '*' || tokens[i].type == '/') {
      if (priority > 2) {
        op = i;
        priority = 2;
      }
    }
    //跳过'()'以及里面的内容
      else if(tokens[i].type == '('){
        while(tokens[i].type != ')')
          i++;
      }
  }

  return op;
}


int eval(int p,int  q) {
  int32_t val,val1,val2;
  int8_t op;

    if (p > q) {
      printf("Bad expression \n");
      return 0; 
    }
    else if (p == q) {
      sscanf(tokens[p].str,"%d",&val);
      return val;
    }
    else if (check_parentheses(p, q) == 1) {
      return eval(p + 1, q - 1);
    }
    else {
      op = find_op(p,q);
      val1 = eval(p, op - 1);
      val2 = eval(op + 1, q);

      switch (tokens[op].type) {
        case '+': return val1 + val2;
        case '-': return val1 - val2;
        case '*': return val1 * val2;
        case '/': return val1 / val2;
        default: assert(0);
      }
    }
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
    return 0;
}

}
