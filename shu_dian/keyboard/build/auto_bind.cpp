#include <nvboard.h>
#include "Vtop.h"

void nvboard_bind_all_pins(Vtop* top) {
	nvboard_bind_pin( &top->ps2_clk, 1, PS2_CLK);
	nvboard_bind_pin( &top->ps2_data, 1, PS2_DAT);
	nvboard_bind_pin( &top->seg0, 8, SEG0A, SEG0B, SEG0C, SEG0D, SEG0E, SEG0F, SEG0G, DEC0P);
	nvboard_bind_pin( &top->seg1, 8, SEG1A, SEG1B, SEG1C, SEG1D, SEG1E, SEG1F, SEG0G, DEC1P);
	nvboard_bind_pin( &top->seg2, 8, SEG2A, SEG2B, SEG2C, SEG2D, SEG2E, SEG2F, SEG0G, DEC2P);
	nvboard_bind_pin( &top->seg3, 8, SEG3A, SEG3B, SEG3C, SEG3D, SEG3E, SEG3F, SEG0G, DEC3P);
	nvboard_bind_pin( &top->seg4, 8, SEG4A, SEG4B, SEG4C, SEG4D, SEG4E, SEG4F, SEG0G, DEC4P);
	nvboard_bind_pin( &top->seg5, 8, SEG5A, SEG5B, SEG5C, SEG5D, SEG5E, SEG5F, SEG0G, DEC5P);
}
