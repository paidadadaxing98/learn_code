#include <nvboard.h>
#include <Vtop.h>

static Vtop dut;

void nvboard_bind_all_pins(Vtop* top);

static void single_cycle() {
  dut.clk = 0; dut.eval();
  dut.clk = 1; dut.eval();
}

static void reset(int n) {
  dut.resetn = 1;
  while (n -- > 0) single_cycle();
  dut.resetn = 0;
}

int main() {
  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset(10);

  while(1) {
    nvboard_update();
    single_cycle();
  }
}
