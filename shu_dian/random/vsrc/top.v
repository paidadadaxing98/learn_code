module  top (
    input clk,rst,
    input bot,
    output reg [7:0] result,
    output reg [7:0] led1,led2
);
    wire [7:0] init;
    reg pos;
    assign init = 8'b00000001;
    always @(init)begin
        result = init;
    end
    always @(posedge bot)begin
        pos = result[4] ^ result[3] ^ result[2] ^ result[0];
        result = {pos,result[7:1]};
    end
    led my_led(
        .in(result),
        .out1(led1),
        .out2(led2)
    );

endmodule

module led(
    input [7:0] in,
    output reg [7:0] out1,out2
);
    trans my_trans1(
        .in(in[3:0]),
        .out(out1)
    );
    trans my_trans2(
        .in(in[7:4]),
        .out(out2)
    );
    
endmodule

module trans(
    input [3:0] in,
    output reg [7:0] out
);
    always @(*) begin
        case(in)
            1:out = {4'b0000,4'b0010};
            2:out = {4'b1001,4'b1110};
            3:out = {4'b0010,4'b0100};
            4:out = {4'b1001,4'b1000};
            5:out = {4'b0100,4'b1000};
            6:out = {4'b0100,4'b0000};
            7:out = {4'b0001,4'b1110};
            8:out = {4'b0000,4'b0000};
            9:out = {4'b0000,4'b1000};
            10:out = {4'b0001,4'b0000};
            11:out = {4'b1100,4'b0000};
            12:out = {4'b0110,4'b0010};
            13:out = {4'b1000,4'b0100};
            14:out = {4'b0110,4'b0000};
            15:out = {4'b0111,4'b0000};
            default:
                out = 8'b00000000;
        endcase
    end


endmodule
