#include "verilated.h"
#include "verilated_vcd_c.h"
#include "obj_dir/Vmux41.h"

VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;

static Vmux41* top;     //change 

void step_and_dump_wave(){
  top->eval();
  contextp->timeInc(1);
  tfp->dump(contextp->time());
}
void sim_init(){
  contextp = new VerilatedContext;
  tfp = new VerilatedVcdC;
  top = new Vmux41;   //change
  contextp->traceEverOn(true);
  top->trace(tfp, 0);
  tfp->open("dump.vcd");    //wave file'name
}

void sim_exit(){
  step_and_dump_wave();
  tfp->close();
}

int main() {
  sim_init();

    top->a=0b0101; top->choose=0b00; step_and_dump_wave();
                    top->choose=0b01;step_and_dump_wave();
                    top->choose=0b10;step_and_dump_wave();
                    top->choose=0b11;step_and_dump_wave();
    top->a=0b1010; top->choose=0b00; step_and_dump_wave();
                    top->choose=0b01;step_and_dump_wave();
                    top->choose=0b10;step_and_dump_wave();
                    top->choose=0b11;step_and_dump_wave();

  sim_exit();
}