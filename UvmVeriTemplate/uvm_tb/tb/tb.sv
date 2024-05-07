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
// module asyn_fifo_tb;
//     import uvm_pkg::*;
//     `include "uvm_macros.svh"
    
//     logic     wclk;
//     logic     rclk;

//     real      wclk_half_period;
//     real      rclk_half_period;
//     fifo_if FIFO(.*);

//     initial begin
//         uvm_config_db#(virtual fifo_if)::set(uvm_root::get(),"uvm_test_top","w_fifo_if",FIFO);
//         uvm_config_db#(virtual fifo_if)::set(uvm_root::get(),"uvm_test_top","r_fifo_if",FIFO);
//         run_test();
//     end

//     initial begin
//         string testname;
//         if($value$plusargs("UVM_TESTNAME=%s",testname))
//             `uvm_info("UVM_TOP_TB",$sformatf("RUNNING TEST {%0s} ...",testname),UVM_NONE)
//         $fsdbDumpfile({testname,".fsdb"});
//         $fsdbDumpvars;
//         $fsdbDumpMDA();
// 		$fsdbDumpSVA;
//     end
 
// 	// Configurable wclk
// 	initial begin
//         wclk = 0;
//         #10
//         //if(uvm_config_db#(real)::get(uvm_root::get(),"uvm_test_top","wclk_half_period",wclk_half_period))  //In test config
//         if(uvm_config_db#(int)::get(uvm_root::get(),"uvm_test_top.m_env.m_wf_agent.m_sequencer","wclk_half_period",wclk_half_period))
//             `uvm_info("WCLK",$sformatf("Configure the wclk_half_period = [%0d]",wclk_half_period),UVM_NONE)
//         else begin
//             `uvm_info("WCLK","Can't configure wclk with config_db correctly,will use default value:10",UVM_MEDIUM)
//             wclk_half_period = 10;
//         end
//         forever begin 
//             #wclk_half_period wclk = ~wclk;
//         end
//     end

// 	// Configurable rclk
// 	initial begin
//         rclk = 0;
//         #10
//         if(uvm_config_db#(int)::get(uvm_root::get(),"uvm_test_top.m_env.m_rf_agent.m_sequencer","rclk_half_period",rclk_half_period))
//             `uvm_info("RCLK",$sformatf("Configure the rclk_half_period = [%0d]",rclk_half_period),UVM_NONE)
//         else begin
//             `uvm_info("RCLK","Can't configure rclk with config_db correctly,will use default value:10",UVM_MEDIUM)
//             rclk_half_period = 10;
//         end
//         forever begin 
//             #rclk_half_period rclk = ~rclk;
//         end
//     end
    
//     // read reset
//     /*
// 	 *initial begin
//      *    rrst_n = 0;
//      *    #30
//      *    rrst_n = 1;
//      *end
//      */
// 	// 读写控制
// asyn_fifo U_ASYN_FIFO_0(
//     .rdata                          ( FIFO.rdata                         ),
//     .rempty                         ( FIFO.rempty                        ),
//     .rclk                           ( FIFO.rclk                          ),
//     .rinc                           ( FIFO.rinc                          ),
//     .rrst_n                         ( FIFO.rrst_n                        ),

//     .wdata                          ( FIFO.wdata                         ),
//     .wfull                          ( FIFO.wfull                         ),
//     .wclk                           ( FIFO.wclk                          ),
//     .winc                           ( FIFO.winc                          ),
//     .wrst_n                         ( FIFO.wrst_n                        )
// );
// endmodule

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
  reg  io_Wr_15_valid;
  reg [7:0] io_Wr_15_data;
  wire  io_Wr_15_ready;
  reg  io_Wr_15_sop;
  reg  io_Wr_15_eop;
  reg  io_Wr_14_valid;
  reg [7:0] io_Wr_14_data;
  wire  io_Wr_14_ready;
  reg  io_Wr_14_sop;
  reg  io_Wr_14_eop;
  reg  io_Wr_13_valid;
  reg [7:0] io_Wr_13_data;
  wire  io_Wr_13_ready;
  reg  io_Wr_13_sop;
  reg  io_Wr_13_eop;
  reg  io_Wr_12_valid;
  reg [7:0] io_Wr_12_data;
  wire  io_Wr_12_ready;
  reg  io_Wr_12_sop;
  reg  io_Wr_12_eop;
  reg  io_Wr_11_valid;
  reg [7:0] io_Wr_11_data;
  wire  io_Wr_11_ready;
  reg  io_Wr_11_sop;
  reg  io_Wr_11_eop;
  reg  io_Wr_10_valid;
  reg [7:0] io_Wr_10_data;
  wire  io_Wr_10_ready;
  reg  io_Wr_10_sop;
  reg  io_Wr_10_eop;
  reg  io_Wr_9_valid;
  reg [7:0] io_Wr_9_data;
  wire  io_Wr_9_ready;
  reg  io_Wr_9_sop;
  reg  io_Wr_9_eop;
  reg  io_Wr_8_valid;
  reg [7:0] io_Wr_8_data;
  wire  io_Wr_8_ready;
  reg  io_Wr_8_sop;
  reg  io_Wr_8_eop;
  reg  io_Wr_7_valid;
  reg [7:0] io_Wr_7_data;
  wire  io_Wr_7_ready;
  reg  io_Wr_7_sop;
  reg  io_Wr_7_eop;
  reg  io_Wr_6_valid;
  reg [7:0] io_Wr_6_data;
  wire  io_Wr_6_ready;
  reg  io_Wr_6_sop;
  reg  io_Wr_6_eop;
  reg  io_Wr_5_valid;
  reg [7:0] io_Wr_5_data;
  wire  io_Wr_5_ready;
  reg  io_Wr_5_sop;
  reg  io_Wr_5_eop;
  reg  io_Wr_4_valid;
  reg [7:0] io_Wr_4_data;
  wire  io_Wr_4_ready;
  reg  io_Wr_4_sop;
  reg  io_Wr_4_eop;
  reg  io_Wr_3_valid;
  reg [7:0] io_Wr_3_data;
  wire  io_Wr_3_ready;
  reg  io_Wr_3_sop;
  reg  io_Wr_3_eop;
  reg  io_Wr_2_valid;
  reg [7:0] io_Wr_2_data;
  wire  io_Wr_2_ready;
  reg  io_Wr_2_sop;
  reg  io_Wr_2_eop;
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
  wire  io_Rd_15_valid;
  wire [7:0] io_Rd_15_data;
  reg  io_Rd_15_ready;
  wire  io_Rd_15_sop;
  wire  io_Rd_15_eop;
  wire  io_Rd_14_valid;
  wire [7:0] io_Rd_14_data;
  reg  io_Rd_14_ready;
  wire  io_Rd_14_sop;
  wire  io_Rd_14_eop;
  wire  io_Rd_13_valid;
  wire [7:0] io_Rd_13_data;
  reg  io_Rd_13_ready;
  wire  io_Rd_13_sop;
  wire  io_Rd_13_eop;
  wire  io_Rd_12_valid;
  wire [7:0] io_Rd_12_data;
  reg  io_Rd_12_ready;
  wire  io_Rd_12_sop;
  wire  io_Rd_12_eop;
  wire  io_Rd_11_valid;
  wire [7:0] io_Rd_11_data;
  reg  io_Rd_11_ready;
  wire  io_Rd_11_sop;
  wire  io_Rd_11_eop;
  wire  io_Rd_10_valid;
  wire [7:0] io_Rd_10_data;
  reg  io_Rd_10_ready;
  wire  io_Rd_10_sop;
  wire  io_Rd_10_eop;
  wire  io_Rd_9_valid;
  wire [7:0] io_Rd_9_data;
  reg  io_Rd_9_ready;
  wire  io_Rd_9_sop;
  wire  io_Rd_9_eop;
  wire  io_Rd_8_valid;
  wire [7:0] io_Rd_8_data;
  reg  io_Rd_8_ready;
  wire  io_Rd_8_sop;
  wire  io_Rd_8_eop;
  wire  io_Rd_7_valid;
  wire [7:0] io_Rd_7_data;
  reg  io_Rd_7_ready;
  wire  io_Rd_7_sop;
  wire  io_Rd_7_eop;
  wire  io_Rd_6_valid;
  wire [7:0] io_Rd_6_data;
  reg  io_Rd_6_ready;
  wire  io_Rd_6_sop;
  wire  io_Rd_6_eop;
  wire  io_Rd_5_valid;
  wire [7:0] io_Rd_5_data;
  reg  io_Rd_5_ready;
  wire  io_Rd_5_sop;
  wire  io_Rd_5_eop;
  wire  io_Rd_4_valid;
  wire [7:0] io_Rd_4_data;
  reg  io_Rd_4_ready;
  wire  io_Rd_4_sop;
  wire  io_Rd_4_eop;
  wire  io_Rd_3_valid;
  wire [7:0] io_Rd_3_data;
  reg  io_Rd_3_ready;
  wire  io_Rd_3_sop;
  wire  io_Rd_3_eop;
  wire  io_Rd_2_valid;
  wire [7:0] io_Rd_2_data;
  reg  io_Rd_2_ready;
  wire  io_Rd_2_sop;
  wire  io_Rd_2_eop;
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
    .io_Wr_15_valid(io_Wr_15_valid),
    .io_Wr_15_data(io_Wr_15_data),
    .io_Wr_15_ready(io_Wr_15_ready),
    .io_Wr_15_sop(io_Wr_15_sop),
    .io_Wr_15_eop(io_Wr_15_eop),
    .io_Wr_14_valid(io_Wr_14_valid),
    .io_Wr_14_data(io_Wr_14_data),
    .io_Wr_14_ready(io_Wr_14_ready),
    .io_Wr_14_sop(io_Wr_14_sop),
    .io_Wr_14_eop(io_Wr_14_eop),
    .io_Wr_13_valid(io_Wr_13_valid),
    .io_Wr_13_data(io_Wr_13_data),
    .io_Wr_13_ready(io_Wr_13_ready),
    .io_Wr_13_sop(io_Wr_13_sop),
    .io_Wr_13_eop(io_Wr_13_eop),
    .io_Wr_12_valid(io_Wr_12_valid),
    .io_Wr_12_data(io_Wr_12_data),
    .io_Wr_12_ready(io_Wr_12_ready),
    .io_Wr_12_sop(io_Wr_12_sop),
    .io_Wr_12_eop(io_Wr_12_eop),
    .io_Wr_11_valid(io_Wr_11_valid),
    .io_Wr_11_data(io_Wr_11_data),
    .io_Wr_11_ready(io_Wr_11_ready),
    .io_Wr_11_sop(io_Wr_11_sop),
    .io_Wr_11_eop(io_Wr_11_eop),
    .io_Wr_10_valid(io_Wr_10_valid),
    .io_Wr_10_data(io_Wr_10_data),
    .io_Wr_10_ready(io_Wr_10_ready),
    .io_Wr_10_sop(io_Wr_10_sop),
    .io_Wr_10_eop(io_Wr_10_eop),
    .io_Wr_9_valid(io_Wr_9_valid),
    .io_Wr_9_data(io_Wr_9_data),
    .io_Wr_9_ready(io_Wr_9_ready),
    .io_Wr_9_sop(io_Wr_9_sop),
    .io_Wr_9_eop(io_Wr_9_eop),
    .io_Wr_8_valid(io_Wr_8_valid),
    .io_Wr_8_data(io_Wr_8_data),
    .io_Wr_8_ready(io_Wr_8_ready),
    .io_Wr_8_sop(io_Wr_8_sop),
    .io_Wr_8_eop(io_Wr_8_eop),
    .io_Wr_7_valid(io_Wr_7_valid),
    .io_Wr_7_data(io_Wr_7_data),
    .io_Wr_7_ready(io_Wr_7_ready),
    .io_Wr_7_sop(io_Wr_7_sop),
    .io_Wr_7_eop(io_Wr_7_eop),
    .io_Wr_6_valid(io_Wr_6_valid),
    .io_Wr_6_data(io_Wr_6_data),
    .io_Wr_6_ready(io_Wr_6_ready),
    .io_Wr_6_sop(io_Wr_6_sop),
    .io_Wr_6_eop(io_Wr_6_eop),
    .io_Wr_5_valid(io_Wr_5_valid),
    .io_Wr_5_data(io_Wr_5_data),
    .io_Wr_5_ready(io_Wr_5_ready),
    .io_Wr_5_sop(io_Wr_5_sop),
    .io_Wr_5_eop(io_Wr_5_eop),
    .io_Wr_4_valid(io_Wr_4_valid),
    .io_Wr_4_data(io_Wr_4_data),
    .io_Wr_4_ready(io_Wr_4_ready),
    .io_Wr_4_sop(io_Wr_4_sop),
    .io_Wr_4_eop(io_Wr_4_eop),
    .io_Wr_3_valid(io_Wr_3_valid),
    .io_Wr_3_data(io_Wr_3_data),
    .io_Wr_3_ready(io_Wr_3_ready),
    .io_Wr_3_sop(io_Wr_3_sop),
    .io_Wr_3_eop(io_Wr_3_eop),
    .io_Wr_2_valid(io_Wr_2_valid),
    .io_Wr_2_data(io_Wr_2_data),
    .io_Wr_2_ready(io_Wr_2_ready),
    .io_Wr_2_sop(io_Wr_2_sop),
    .io_Wr_2_eop(io_Wr_2_eop),
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
    .io_Rd_15_valid(io_Rd_15_valid),
    .io_Rd_15_data(io_Rd_15_data),
    .io_Rd_15_ready(io_Rd_15_ready),
    .io_Rd_15_sop(io_Rd_15_sop),
    .io_Rd_15_eop(io_Rd_15_eop),
    .io_Rd_14_valid(io_Rd_14_valid),
    .io_Rd_14_data(io_Rd_14_data),
    .io_Rd_14_ready(io_Rd_14_ready),
    .io_Rd_14_sop(io_Rd_14_sop),
    .io_Rd_14_eop(io_Rd_14_eop),
    .io_Rd_13_valid(io_Rd_13_valid),
    .io_Rd_13_data(io_Rd_13_data),
    .io_Rd_13_ready(io_Rd_13_ready),
    .io_Rd_13_sop(io_Rd_13_sop),
    .io_Rd_13_eop(io_Rd_13_eop),
    .io_Rd_12_valid(io_Rd_12_valid),
    .io_Rd_12_data(io_Rd_12_data),
    .io_Rd_12_ready(io_Rd_12_ready),
    .io_Rd_12_sop(io_Rd_12_sop),
    .io_Rd_12_eop(io_Rd_12_eop),
    .io_Rd_11_valid(io_Rd_11_valid),
    .io_Rd_11_data(io_Rd_11_data),
    .io_Rd_11_ready(io_Rd_11_ready),
    .io_Rd_11_sop(io_Rd_11_sop),
    .io_Rd_11_eop(io_Rd_11_eop),
    .io_Rd_10_valid(io_Rd_10_valid),
    .io_Rd_10_data(io_Rd_10_data),
    .io_Rd_10_ready(io_Rd_10_ready),
    .io_Rd_10_sop(io_Rd_10_sop),
    .io_Rd_10_eop(io_Rd_10_eop),
    .io_Rd_9_valid(io_Rd_9_valid),
    .io_Rd_9_data(io_Rd_9_data),
    .io_Rd_9_ready(io_Rd_9_ready),
    .io_Rd_9_sop(io_Rd_9_sop),
    .io_Rd_9_eop(io_Rd_9_eop),
    .io_Rd_8_valid(io_Rd_8_valid),
    .io_Rd_8_data(io_Rd_8_data),
    .io_Rd_8_ready(io_Rd_8_ready),
    .io_Rd_8_sop(io_Rd_8_sop),
    .io_Rd_8_eop(io_Rd_8_eop),
    .io_Rd_7_valid(io_Rd_7_valid),
    .io_Rd_7_data(io_Rd_7_data),
    .io_Rd_7_ready(io_Rd_7_ready),
    .io_Rd_7_sop(io_Rd_7_sop),
    .io_Rd_7_eop(io_Rd_7_eop),
    .io_Rd_6_valid(io_Rd_6_valid),
    .io_Rd_6_data(io_Rd_6_data),
    .io_Rd_6_ready(io_Rd_6_ready),
    .io_Rd_6_sop(io_Rd_6_sop),
    .io_Rd_6_eop(io_Rd_6_eop),
    .io_Rd_5_valid(io_Rd_5_valid),
    .io_Rd_5_data(io_Rd_5_data),
    .io_Rd_5_ready(io_Rd_5_ready),
    .io_Rd_5_sop(io_Rd_5_sop),
    .io_Rd_5_eop(io_Rd_5_eop),
    .io_Rd_4_valid(io_Rd_4_valid),
    .io_Rd_4_data(io_Rd_4_data),
    .io_Rd_4_ready(io_Rd_4_ready),
    .io_Rd_4_sop(io_Rd_4_sop),
    .io_Rd_4_eop(io_Rd_4_eop),
    .io_Rd_3_valid(io_Rd_3_valid),
    .io_Rd_3_data(io_Rd_3_data),
    .io_Rd_3_ready(io_Rd_3_ready),
    .io_Rd_3_sop(io_Rd_3_sop),
    .io_Rd_3_eop(io_Rd_3_eop),
    .io_Rd_2_valid(io_Rd_2_valid),
    .io_Rd_2_data(io_Rd_2_data),
    .io_Rd_2_ready(io_Rd_2_ready),
    .io_Rd_2_sop(io_Rd_2_sop),
    .io_Rd_2_eop(io_Rd_2_eop),
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

always #5  clock = ! clock ;

initial begin
    #0 reset = 1;
    #10 reset = 0;
    io_Wr_0_sop = 1;
    io_Wr_0_data = 8'h00;
    io_Wr_0_valid = 1;
    #10
    io_Wr_0_sop = 0;
    io_Wr_0_data = 8'h01;
    #10
    io_Wr_0_data = 8'h02;
    #100
    io_Wr_0_eop = 1;
    #10
    io_Wr_0_valid = 0;

    #10000 $finish;
end


endmodule