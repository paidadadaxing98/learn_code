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
#include "local-include/reg.h"

#define NUM_REGS (sizeof(regs) / sizeof(regs[0]))

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void isa_reg_display() {
  bool success = true;
  int32_t val;
  printf("reg    reg_val\n");
  for (int i = 0; i < NUM_REGS; i++) {
    val = isa_reg_str2val(regs[i],&success);
    printf("%-7s0x%08x\n",regs[i],val);
  }
  printf("pc:0x%x\n",cpu.pc);
  printf("\n");
}


word_t isa_reg_str2val(const char *s, bool *success) {
  int i =0;
  if(strcmp(s,"pc") == 0){
    return cpu.pc;
  }
  for(i=0;i<NUM_REGS;i++){
     if(strcmp(s,regs[i]) == 0){
      *success = 1;
      return cpu.gpr[i];
     }
  }
  if(i == NUM_REGS && strcmp(s,"pc") != 0){
    *success = 0;
    printf("can't find %s reg\n",s);
    return 0;
  }

  return 0;
}
