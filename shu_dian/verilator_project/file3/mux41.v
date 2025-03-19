module mux41(
    input [3:0] a,
    input [1:0] choose,
    output reg out
);

always @(*) begin
    case(choose)
    0:out = a[0];
    1:out = a[1];
    2:out = a[2];
    3:out = a[3];
    default:out = 1'b0;

    endcase

end
endmodule