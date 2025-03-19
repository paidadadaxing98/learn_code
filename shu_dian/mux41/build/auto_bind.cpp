#include <nvboard.h>
#include "Vmux41.h"

void nvboard_bind_all_pins(Vmux41* top) {
	nvboard_bind_pin( &top->s, 2, SW0, SW1);
	nvboard_bind_pin( &top->a, 2, SW2, SW3);
	nvboard_bind_pin( &top->b, 2, SW4, SW5);
	nvboard_bind_pin( &top->c, 2, SW6, SW7);
	nvboard_bind_pin( &top->d, 2, SW8, SW9);
	nvboard_bind_pin( &top->y, 2, LD0, LD1);
}
