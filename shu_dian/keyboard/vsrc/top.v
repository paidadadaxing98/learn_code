module top(
    input clk, 
    input resetn, 
    input ps2_clk, 
    input ps2_data,

    output reg [7:0] seg0, 
    output reg [7:0] seg1, 
    output reg [7:0] seg2, 
    output reg [7:0] seg3, 
    output reg [7:0] seg4, 
    output reg [7:0] seg5
);
reg [7:0] ascii,scancode;
reg [7:0] count,num;
wire [7:0] scancode1,num1;
reg light;
wire light1;

    ps2_keyboard keyboard(
        .clk(clk),
        .resetn(~resetn),
        .ps2_clk(ps2_clk),
        .ps2_data(ps2_data),
        .scancode(scancode),
        .num(num),
        .light(light)
    );

        assign scancode1 = scancode;
        assign num1 = num;
        assign light1 = light;
        
    keycode keycode1(
        .light(light1),
        .clk(clk),
        .resetn(~resetn),
        .scancode(scancode1),
        .seg1(seg1),
        .seg0(seg0)
    );

    asciicode asciicode1(
        .light(light1),
        .clk(clk),
        .resetn(~resetn),
        .scancode(scancode1),
        .ascii(ascii),
        .seg2(seg2),
        .seg3(seg3)
    );

    key_count key_count1(
        .count(num1),
        .seg4(seg4),
        .seg5(seg5)
    );



endmodule


    