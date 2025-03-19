module top(
    input [7:0] in,
    input en,clk,rst,
    output y,
    output reg [7:0] hex,
    output reg [2:0] p
);
    encode8_3 my_encode(
        .en(en),
        .in(in),
        .out(p),
        .y(y)
    );

    led4_16 my_led4_16(
        .en(y),
        .in(p),
        .out(hex)
    );

endmodule