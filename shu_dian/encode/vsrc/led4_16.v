module led4_16(
    input en,
    input [2:0] in,
    output reg [7:0] out 
);

integer i;

always @(*) begin
    if(en)begin

        case(in[2:0])
            0:out = 8'b00000010;
            1:out = 8'b10011110;
            2:out = 8'b00100100;
            3:out = 8'b00001100;
            4:out = 8'b10011000;
            5:out = 8'b01001000;
            6:out = 8'b01000000;
            7:out = 8'b00011110;
        default:
                out = 8'b11111111;
        endcase
        

    end
end

endmodule