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
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"

static int is_batch_mode = false;

int wp_main();

bool wp_print();
word_t vaddr_read(vaddr_t, int);
void init_regex();
void init_wp_pool();
void cpu_exec(uint64_t);
void isa_reg_display();
void file_test(void);
WP* new_wp();
void free_wp();


/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  return -1;
}

static int cmd_help(char *args);

// the following features were added later

static int cmd_si(char *args) {
  int n = 0;
  if(args == NULL){
    cpu_exec(1);
  }
  else{
    sscanf(args,"%d ",&n);
    cpu_exec(n);
  }

  return 0;
}


static int cmd_info(char *args){
  if(args == NULL){
    printf("you can choose r or w \n");
  }
  else{
      if(strcmp(args,"r") == 0){
        isa_reg_display();    
      }
      else if(strcmp(args,"w") == 0){
        wp_print();
      }
      else{
        printf("unknown argument!\n");
    }
  }

  return 0;
}

static int cmd_x(char *args){
  int i;
  int n,x;
  int addr;
  char expression[512] = {0};bool success = false;
  int32_t val;
  if(args == NULL){
    printf("please enter number!\n");
    return 0;
  }
  else{
      sscanf(args,"%d %s",&n,expression);
      if(expression[0] == '\0'){
        x = 0x80000000;
      }
      else{
        x = expr(expression,&success);
      }

     for(i=0;i<n;i++){
      addr = x + 4*i;
      val = vaddr_read(x,4);
      printf("address:0x%-8x : 0x%08x\n",addr,val);
     }
    
      return 0;
    }

  }



static int cmd_p(char *args){
  bool success[1];
  int val;
  val = expr(args,success);
  
  if(*success){
  printf("the result is %d\n",val);
  }
  else
    {
      printf("Parsing failure[cmd_p]\n");
    }
    return 0;
}

static int cmd_test(char *args){
  if(args == NULL){
    printf("please enter a loop number\n");
    return 0;
  }
  else {
    int i = 0;
    int status;   
    char command[256];  
    sscanf(args,"%d",&i);
    sprintf(command, "/home/zy/ysyx-workbench/nemu/tools/gen-expr/gen-expr %d > ./test_expression", i);
    status = system(command);
    if(status < 0){
      return 0;
    }
    file_test();
  }
  return 1;
}

void file_test(){
  FILE *f = fopen("/home/zy/ysyx-workbench/nemu/test_expression","r");
  if(f == NULL){
    printf("memory fail!\n");
    exit(1);
  }
  char express[1024];
  int expect_result = 0;
  bool success = false;
  int error_num = 0;
  while(fscanf(f,"%d %[^\n]",&expect_result,express) != EOF){
    unsigned result = 0;
    result = expr(express,&success);
    if(result == expect_result){
      continue;
    }
    else{
      error_num++;
      printf("%d %s\n",expect_result,express);
      printf("error!\nexpect result is %d\nbut result is %d\n",expect_result,result);
    }
  }

  if(error_num == 0){
    printf("all expression can pass!\n");
  }
  else{
    printf("error number:%d\n",error_num);
  }

  fclose(f);
}


int cmd_w(char *args){

  if(args == NULL){
    printf("enter an expression\n");
    return 0;
  }
  else{
    WP* reg;
    bool success = true;
    reg = new_wp();
    assert(reg);
    sprintf(reg->expr,"%s",args);
    reg->l_result =expr(reg->expr,&success);
  }
  return 0;
}

int cmd_d(char *args){

  if(args == NULL){
    printf("enter a NO.\n");
  }
  else{
    int n;
    sscanf(args,"%d",&n);
    free_wp(n);
  }
    
  return 0;
}

int cmd_b(char *args){
  int addr;
  char b_cmd[64];
  int status;
  if(args == NULL){
    printf("enter address\n");
  }
  else{
    sscanf(args,"%x",&addr);
    sprintf(b_cmd,"w $t0 == %x",addr);
    printf("%s\n",b_cmd);
    status = system(b_cmd);
    if(status < 0){
      printf("unkonwn address\n");
    }

    cpu_exec(1);
  }
  return 0;
}




//finish adding
static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  {"si","Single step", cmd_si},
  {"info","print status",cmd_info},
  {"x","scan memory",cmd_x},
  {"p","find the value of expression",cmd_p},
  {"test","test the expression",cmd_test},
  {"w","add a watchpoint",cmd_w},
  {"d","delete a watchpoint",cmd_d},
  {"b","set a breakpoint",cmd_b},
  /* TODO: Add more commands */

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return ;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { exit(0); }    
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
