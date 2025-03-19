module key_count(
    output reg [7:0] count,
    output reg [7:0] seg4, seg5
);

    wire [7:0] count1;
    assign count1 = count;

my_seg seg_inst(
    .light(1'b1),
    .in(count1),
    .out1(seg4),
    .out2(seg5)
);

endmodule
