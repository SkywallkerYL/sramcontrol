// // ---------------------------------------------------------------------------------
// //                 Copyright (c) 2022 
// //                 ALL RIGHTS RESERVED
// // ---------------------------------------------------------------------------------
// // Filename      : tb.sv
// // Author        : AiF
// // Created On    : 2022-05-15 19:17
// // Last Modified : 2022-05-19 11:36
// // ---------------------------------------------------------------------------------
// // Description   : 
// //
// //
// // ---------------------------------------------------------------------------------

module SramTop_tb;

  // Parameters
        initial begin
            string testname;
            //if($value$plusargs("UVM_TESTNAME=%s",testname))
            //    `uvm_info("UVM_TOP_TB",$sformatf("RUNNING TEST {%0s} ...",testname),UVM_NONE)
            $fsdbDumpfile({"Test.fsdb"});
            $fsdbDumpvars;
            $fsdbDumpMDA();
    		$fsdbDumpSVA;
        end
//Ports
reg  clock;
reg  reset;
reg  io_Wr_1_valid;
reg [7:0] io_Wr_1_data;
wire  io_Wr_1_ready;
reg  io_Wr_1_sop;
reg  io_Wr_1_eop;
reg  io_Wr_0_valid;
reg [7:0] io_Wr_0_data;
wire  io_Wr_0_ready;
reg  io_Wr_0_sop;
reg  io_Wr_0_eop;
wire  io_Rd_1_valid;
wire [7:0] io_Rd_1_data;
reg  io_Rd_1_ready;
wire  io_Rd_1_sop;
wire  io_Rd_1_eop;
wire  io_Rd_0_valid;
wire [7:0] io_Rd_0_data;
reg  io_Rd_0_ready;
wire  io_Rd_0_sop;
wire  io_Rd_0_eop;

SramTop  SramTop_inst (
  .clock(clock),
  .reset(reset),
  .io_Wr_1_valid(io_Wr_1_valid),
  .io_Wr_1_data(io_Wr_1_data),
  .io_Wr_1_ready(io_Wr_1_ready),
  .io_Wr_1_sop(io_Wr_1_sop),
  .io_Wr_1_eop(io_Wr_1_eop),
  .io_Wr_0_valid(io_Wr_0_valid),
  .io_Wr_0_data(io_Wr_0_data),
  .io_Wr_0_ready(io_Wr_0_ready),
  .io_Wr_0_sop(io_Wr_0_sop),
  .io_Wr_0_eop(io_Wr_0_eop),
  .io_Rd_1_valid(io_Rd_1_valid),
  .io_Rd_1_data(io_Rd_1_data),
  .io_Rd_1_ready(io_Rd_1_ready),
  .io_Rd_1_sop(io_Rd_1_sop),
  .io_Rd_1_eop(io_Rd_1_eop),
  .io_Rd_0_valid(io_Rd_0_valid),
  .io_Rd_0_data(io_Rd_0_data),
  .io_Rd_0_ready(io_Rd_0_ready),
  .io_Rd_0_sop(io_Rd_0_sop),
  .io_Rd_0_eop(io_Rd_0_eop)
);
always #5  clock = ~ clock ;

initial begin
    clock = 0;
    #0 reset = 1;
    #20 reset = 0;
    // Write port 0 
    fork
    // Write port 0
      begin
        io_Rd_0_ready = 0; 
        io_Wr_0_sop = 1;
        io_Wr_0_eop = 0;
        io_Wr_0_data = 8'h00;
        io_Wr_0_valid = 1;
      end
      begin
        io_Rd_1_ready = 0; 
        io_Wr_1_sop = 1;
        io_Wr_1_eop = 0;
        io_Wr_1_data = 8'h00;
        io_Wr_1_valid = 0;
      end
    // Write port 1
    join
    #10
    io_Wr_0_sop = 0;
    io_Wr_0_data = 8'h01;
    #10
    io_Wr_0_data = 8'h02;
    #10
    io_Wr_0_data = 8'h03;
    #10
    io_Wr_0_data = 8'h04;
    #10
    io_Wr_0_data = 8'h05;
    #100
    io_Wr_0_eop = 1;
    #10
    io_Wr_0_valid = 0;
    io_Wr_0_eop = 0;
    # 10
    #200
    //read port 0
    io_Rd_0_ready = 1;  
    #150 
    //port0 write again
    io_Wr_0_valid = 1;
    io_Wr_0_sop = 1;
    #10
    io_Wr_0_data = 8'h06;
    io_Wr_0_sop = 0;
    #100
    io_Wr_0_data = 8'h07;
    io_Wr_0_eop = 1;
    #10
    io_Wr_0_valid = 0;

    // Write port 1
    io_Wr_1_valid = 1; 
    io_Wr_1_data = 8'h00;
    #10
    io_Wr_1_sop = 0;
    io_Wr_1_data = 8'h01;
    #10
    io_Wr_1_data = 8'h02;
    #10
    io_Wr_1_data = 8'h03;
    #200
    io_Wr_1_eop = 1;
    #10
    io_Wr_1_valid = 0;
    # 10
    io_Rd_1_ready = 1;
    #1000 $finish;
end


endmodule