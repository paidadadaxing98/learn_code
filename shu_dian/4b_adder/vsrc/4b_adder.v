module top(
    input clk,rst,
    input [2:0] func,
    input  [3:0] a,b,
    output reg c,
    output reg over,
    output [3:0] result
);
    reg [3:0] a1,b1;

    always @(*) begin
            result = 0;
            c =0;
            over = 0;

            if(func == 0)begin

            if(a[3] == 1&&b[3] == 1)//转换成补码形式
            begin
                    a1 = ~a + 1;
                    b1 = ~b + 1;
            end
            else if(a[3] == 1)begin  
                a1 = ~a + 1;
                a1[3] = 1;
                b1 = b;  
            end
            else if(a[3] == 1&&b[3] == 1)
                begin
                    a1 = ~a + 1;
                    b1 = ~b + 1;
                end
            else
                begin
                    a1 = a;
                    b1 = b;
                end

                {c,result} = a1 + b1;

                if(result[3] == 1) begin        //转换成原码
                    result = result - 1;
                    result = ~result;
                    result[3] = 1; 
                end
                else
                begin
                    result = result;  
                end

             over = (a[3] == b[3]) && (result[3] != a[3]);
            end
            else if(func == 1)begin

            if(a[3] == 1&&b[3] == 1)//转换成补码形式
            begin
                    a1 = ~a + 1;
                    b1 = ~b + 1;
            end
            else if(a[3] == 1)begin  
                a1 = ~a + 1;
                a1[3] = 1;
                b1 = b;  
            end
            else if(a[3] == 1&&b[3] == 1)
                begin
                    a1 = ~a + 1;
                    b1 = ~b + 1;
                end
            else
                begin
                    a1 = a;
                    b1 = b;
                end

                {c,result} = a1 + ~{1'0,b1} + 1;

                if(result[3] == 1) begin        //转换成原码
                    result = result - 1;
                    result = ~result;
                    result[3] = 1; 
                end
                else
                begin
                    result = result;  
                end

                over = (a[3] == b[3]) && (result[3] != a[3]);
            end
            else if(func == 2)begin
                result = ~a;
                over = 0;c = 0;a1 = a;b1 = b;
            end
            else if(func == 3)begin
                result = a & b;
                over = 0;c = 0;a1 = a;b1 = b;
            end
            else if(func == 4)begin
                result = a | b;
                over = 0;c = 0;a1 = a;b1 = b;
            end            
            else if(func == 5)begin
                result = a ^ b;
                over = 0;c = 0;a1 = a;b1 = b;
            end
            else if(func == 6)begin
                result = a < b ? 1 : 0;
                over = 0;c = 0;a1 = a;b1 = b;
            end
            else if(func == 7)begin
                result = a == b ? 1 : 0;
                over = 0;c = 0;a1 = a;b1 = b;
            end
            else
                begin
                result = 0;over = 0;c = 0;
                a1 = 0; b1 = 0;
                end            

    end
    
endmodule