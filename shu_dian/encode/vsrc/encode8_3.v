module encode8_3(
    input en,
    input [7:0] in,
    output reg [2:0] out,
    output reg y
);
    integer i; 
    always @(*) begin
    if(en) 
        begin
            if(in == 0)begin
                out = 3'b000;
                y = 0;
            end
            else
                begin
                    y = 1;
                    out = 3'b000; // 默认值为 0
                    for(i=0;i<8;i=i+1)begin
                        if(in[i] == 1)begin
                            out = i[2:0]; // 找到最后一个 1，更新输出
                        end
                    end
                end
        end
    else 
        begin
            out = 3'b000;
            y = 0;
        end
    end

endmodule
