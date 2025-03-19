module keycode(
    input light,
    input clk, resetn,
    input [7:0] scancode,
    output reg [7:0] seg1, seg0
);
wire [7:0] scancode2;
assign scancode2 = scancode;


my_seg seg_inst(
    .light(light),
    .in(scancode2),
    .out1(seg0),
    .out2(seg1)
);

endmodule
