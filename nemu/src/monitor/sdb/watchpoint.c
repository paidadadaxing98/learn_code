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

#include "sdb.h"
#include <readline/readline.h>
#include <readline/history.h>



#define NR_WP 33

void wp_mainloop(void);


WP wp_pool[NR_WP] = {};
WP *head = NULL, *free_ = NULL;
//记得最后要free(head)
void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].status = 0;
    wp_pool[i].NO = 33 - i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = (WP*)malloc(sizeof(WP));
  head->next = NULL;
  free_ = wp_pool;
}

WP *new_wp(){
  WP* pool_p = wp_pool;
  WP* head_p = head;
  if(pool_p->next == NULL){//第一个不用
    printf("there are not free watchpoints\n");
    assert(0);
  }
  else{
      while(pool_p->next->next != NULL){
      pool_p = pool_p->next;
  }//take the last WP
    while(head_p->next != NULL){
      head_p = head_p->next;
    }
    head_p->next = pool_p->next;
    pool_p->next->status = 1;
    pool_p->next = NULL; 
    return head_p->next;
  }

 
}//pool的结尾拼接到head的结尾

void free_wp(int n){
  WP* pool_p = wp_pool;
  WP* head_p = head;
  WP* wp = NULL;
  

  while (head_p->next != NULL && head_p->next->NO != n) {
      head_p = head_p->next;
  }

  if (head_p->next == NULL) {
      printf("the watchpoint is free\n");
      return;  
  }
  wp = head_p->next;  
  head_p->next = wp->next;  
  wp->next = NULL;  

  while (pool_p->next != NULL) {
      pool_p = pool_p->next;
  }

  pool_p->next = wp;  
  wp->status = 0;  
  strcpy(wp->expr, "");  
}


  static char *line_read = NULL;
/* TODO: Implement the functionality of watchpoint */
static char* rl_gets() {


  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(WP) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}


bool wp_change(){
  bool change = false;
  WP* head_p = head->next;
  bool success = false;
  while(head_p != NULL){
  if(expr(head_p->expr,&success) != head_p->l_result){
    head_p->l_result = expr(head_p->expr,&success);
    printf("NO.%d watchpoint changed!\n",head_p->NO);
    head_p = head_p->next;
    change = true;    
    continue;
  }
}
  return change;
}

void wp_print(){  
  WP* head_p = head->next;

    if(head_p == NULL){
      printf("there are no watchpoints\n");
    }
    else{    
      printf("NO    expr\n");
      while(head_p != NULL){
      printf("%-6d%-16s\n",head_p->NO,head_p->expr);
      head_p = head_p->next;
    }

  }
}



//由于目前不能解决第二次进入时突然什么也不显示的问题，放弃程序上套程序的想法
int wp_main(){

  init_wp_pool();

  wp_mainloop();

  return 0;
}

void wp_mainloop(){
  char* cmd;
  char* args;
  bool success[1];
  WP* reg;
  WP* head_end = head;
  while(head_end->next != NULL){
    head_end = head->next;
  }
  for(char* str;(str = rl_gets()) != NULL;){
    char *str_end = str + strlen(str);

    cmd = strtok(str," ");
    if(cmd == NULL) {continue;}

    args = cmd + strlen(cmd) + 1;
    if(args >= str_end){
      args = NULL;
      }
    
    if(strcmp(cmd,"w") == 0){
      if(args == NULL){
        printf("enter an expression\n");
      }
      else{
        reg = new_wp();
        assert(reg);
        sprintf(reg->expr,"%s",args);
        reg->l_result =expr(reg->expr,success);
      }


    }

    else if(strcmp(cmd,"d") == 0){
      int n;
      if(args == NULL){
        printf("enter a NO.\n");
      }
      else{
        sscanf(args,"%d",&n);
        free_wp(n);
      }
    }

    else if(strcmp(cmd,"ls") == 0){
      wp_print();
    }

    else if(strcmp(cmd,"q") == 0){
      break;
    }

    else if(strcmp(cmd,"help") == 0){
      printf("w <expression> - watch the expression\n");
      printf("d <NO.> - delete one watch point\n");
      printf("q - quit\n");
      printf("ls - list watchpoints\n");
    }
    else {
      printf("unknown cmd.\ntry 'help'\n");
    }
  }

}


