module ps2_keyboard(
input clk,resetn,ps2_clk,ps2_data,
output reg [7:0] scancode,
output reg [7:0] num,
output reg light
);//获取当前扫描吗

reg [9:0] buffer;        // ps2_data bits
reg [3:0] count;  // count ps2_data bits
reg [2:0] ps2_clk_sync;
reg pressed;

always @(posedge clk) begin
    ps2_clk_sync <=  {ps2_clk_sync[1:0],ps2_clk};
end

wire sampling = ps2_clk_sync[2] & ~ps2_clk_sync[1];

always @(posedge clk) begin
    if (resetn == 0) begin // reset
        count <= 0;
        num = 0;
        pressed = 0;
        scancode <= 8'hff;
        light = 0;
    end
    else begin
        if (sampling) begin
          if (count == 4'd10) begin
            if ((buffer[0] == 0) &&  // start bit
                (ps2_data)       &&  // stop bit
                (^buffer[9:1])) 
                begin      // odd  parity
                scancode <= buffer[8:1];

                if(scancode != 8'hf0)begin
                  pressed = 0;
                  light = 1;
                end
                else  begin
                  pressed = 1;
                  light = 0;
                end

                if(pressed) begin
                  num = num +1;
                end

                $display("receive %x", buffer[8:1]);
                $display(" num is %d",num);
            end
            count <= 0;    // for next
            pressed = 0;
          end 
          else begin
            buffer[count] <= ps2_data;  // store ps2_data
            count <= count + 3'b1;
          end
        end
    end
end

endmodule