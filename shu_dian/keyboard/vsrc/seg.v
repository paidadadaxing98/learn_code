module my_seg(
    input light,     
    input [7:0] in,
    output reg [7:0] out1,out2
);  //将两个16进制数可视化

    reg [15:0] in_r; 
    always @(*) begin

    if(!light)  begin
    in_r[7:0] = {4'b0001, in[3:0]};  //本来想拼接一位作为显示位
    in_r[15:8] = {4'b0001, in[7:4]};  
    end
    else begin
    in_r[7:0] = {4'b0000, in[3:0]};  
    in_r[15:8] = {4'b0000, in[7:4]};  
    end

    end

    trans my_trans1(
        .in(in_r[7:0]),
        .out(out1)
    );
    trans my_trans2(
        .in(in_r[15:8]),
        .out(out2)
    );
    
endmodule

module trans(
    input [7:0] in, //防止一一对应
    output reg [7:0] out
);
    always @(*) begin
        case(in)
            0:out = {4'b0000,4'b0010};
            1:out = {4'b1001,4'b1110};
            2:out = {4'b0010,4'b0100};
            3:out = {4'b0000,4'b1100};
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
                out = 8'b11111111;
        endcase
    end


endmodule