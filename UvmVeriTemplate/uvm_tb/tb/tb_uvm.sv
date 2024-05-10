// ---------------------------------------------------------------------------------
//                 Copyright (c) 2022 
//                 ALL RIGHTS RESERVED
// ---------------------------------------------------------------------------------
// Filename      : tb.sv
// Author        : AiF
// Created On    : 2022-05-15 19:17
// Last Modified : 2022-05-19 11:36
// ---------------------------------------------------------------------------------
// Description   : 
//
//
// ---------------------------------------------------------------------------------
module tb_uvm;
    import uvm_pkg::*;
    `include "uvm_macros.svh"

    logic clock;
    logic reset;

    real      clock_period = 10;
    real      half_clock_period = clock_period / 2;
//    fifo_if FIFO(.*);
    //例化N组读写接口
    write_interface w_if_0(.clock(clock),.reset(reset));
    read_interface r_if_0(.clock(clock),.reset(reset));

    write_interface w_if_1(.clock(clock),.reset(reset));
    read_interface r_if_1(.clock(clock),.reset(reset));

    initial begin
      uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env.i_agt.drv", "wif0", w_if_0);
      uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env.i_agt.drv", "wif1", w_if_1);
      //uvm_config_db#(virtual my_if)::set(null, "uvm_test_top.env.i_agt.mon", "vif", input_if);
      uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env.o_agt.mon", "rif0", r_if_0);
      uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env.o_agt.mon", "rif1", r_if_1);
    end

    initial begin
        string testname;
        if($value$plusargs("UVM_TESTNAME=%s",testname))
            `uvm_info("UVM_TOP_TB",$sformatf("RUNNING TEST {%0s} ...",testname),UVM_NONE)
        $fsdbDumpfile({testname,".fsdb"});
        $fsdbDumpvars;
        $fsdbDumpMDA();
		    $fsdbDumpSVA;
        run_test("base_test");
    end
 
	// Configurable wclk
	  initial begin
        clock = 0;
        #10
        //if(uvm_config_db#(real)::get(uvm_root::get(),"uvm_test_top","wclk_half_period",wclk_half_period))  //In test config
        //if(uvm_config_db#(int)::get(uvm_root::get(),"uvm_test_top.m_env.m_wf_agent.m_sequencer","clock_period",clock_period))
        //    `uvm_info("CLK",$sformatf("Configure the clock_period = [%0d]",clock_period),UVM_NONE)
        //else begin
        //    `uvm_info("CLK","Can't configure clock period with config_db correctly,will use default value:10",UVM_MEDIUM)
        //    clock_period = 10;
        //end
        forever begin 
            #half_clock_period clock = ~clock;
        end
    end

    
    // read reset
    
	initial begin
        //给Interface指定默认值
        w_if_0.valid = 0;
        w_if_0.data = 0;
        w_if_0.sop = 0;
        w_if_0.eop = 0;

        w_if_1.valid = 0;
        w_if_1.data = 0;
        w_if_1.sop = 0;
        w_if_1.eop = 0;

        r_if_0.ready = 1;
        r_if_1.ready = 1;
      reset = 1;
      #30
      reset = 0;
    end
     
     //Sram 读写顶层
     SramTop  SramTop_inst (
      .clock(clock),
      .reset(reset),
      .io_Wr_1_valid(w_if_1.valid),
      .io_Wr_1_data(w_if_1.data),
      .io_Wr_1_ready(w_if_1.ready),
      .io_Wr_1_sop(w_if_1.sop),
      .io_Wr_1_eop(w_if_1.eop),
      .io_Wr_0_valid(w_if_0.valid),
      .io_Wr_0_data(w_if_0.data),
      .io_Wr_0_ready(w_if_0.ready),
      .io_Wr_0_sop(w_if_0.sop),
      .io_Wr_0_eop(w_if_0.eop),
      .io_Rd_1_valid(r_if_1.valid),
      .io_Rd_1_data(r_if_1.data),
      .io_Rd_1_ready(r_if_1.ready),
      .io_Rd_1_sop(r_if_1.sop),
      .io_Rd_1_eop(r_if_1.eop),
      .io_Rd_0_valid(r_if_0.valid),
      .io_Rd_0_data(r_if_0.data),
      .io_Rd_0_ready(r_if_0.ready),
      .io_Rd_0_sop(r_if_0.sop),
      .io_Rd_0_eop(r_if_0.eop)
    );
endmodule


