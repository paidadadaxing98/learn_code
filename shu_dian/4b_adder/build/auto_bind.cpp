#include <nvboard.h>
#include "Vtop.h"

void nvboard_bind_all_pins(Vtop* top) {
	nvboard_bind_pin( &top->a, 4, SW15, SW14, SW13, SW12);
	nvboard_bind_pin( &top->b, 4, SW11, SW10, SW9, SW8);
	nvboard_bind_pin( &top->func, 3, SW7, SW6, SW5);
	nvboard_bind_pin( &top->c, 1, LD4);
	nvboard_bind_pin( &top->result, 4, LD3, LD2, LD1, LD0);
	nvboard_bind_pin( &top->over, 1, LD6);
}
