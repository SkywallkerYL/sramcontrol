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
module tb_uvmallport;
    import uvm_pkg::*;
    `include "uvm_macros.svh"

    logic clock;
    logic reset;

    real      clock_period = 10;
    real      half_clock_period = clock_period / 2;
//    fifo_if FIFO(.*);
    //例化16组读写接口
    write_interface w_if_0(.clock(clock),.reset(reset));
    read_interface r_if_0(.clock(clock),.reset(reset));

    write_interface w_if_1(.clock(clock),.reset(reset));
    read_interface r_if_1(.clock(clock),.reset(reset));

    write_interface w_if_2(.clock(clock),.reset(reset));
    read_interface r_if_2(.clock(clock),.reset(reset));

    write_interface w_if_3(.clock(clock),.reset(reset));
    read_interface r_if_3(.clock(clock),.reset(reset));

    write_interface w_if_4(.clock(clock),.reset(reset));
    read_interface r_if_4(.clock(clock),.reset(reset));

    write_interface w_if_5(.clock(clock),.reset(reset));
    read_interface r_if_5(.clock(clock),.reset(reset));

    write_interface w_if_6(.clock(clock),.reset(reset));
    read_interface r_if_6(.clock(clock),.reset(reset));

    write_interface w_if_7(.clock(clock),.reset(reset));
    read_interface r_if_7(.clock(clock),.reset(reset));

    write_interface w_if_8(.clock(clock),.reset(reset));
    read_interface r_if_8(.clock(clock),.reset(reset));

    write_interface w_if_9(.clock(clock),.reset(reset));
    read_interface r_if_9(.clock(clock),.reset(reset));

    write_interface w_if_10(.clock(clock),.reset(reset));
    read_interface r_if_10(.clock(clock),.reset(reset));

    write_interface w_if_11(.clock(clock),.reset(reset));
    read_interface r_if_11(.clock(clock),.reset(reset));

    write_interface w_if_12(.clock(clock),.reset(reset));
    read_interface r_if_12(.clock(clock),.reset(reset));

    write_interface w_if_13(.clock(clock),.reset(reset));
    read_interface r_if_13(.clock(clock),.reset(reset));

    write_interface w_if_14(.clock(clock),.reset(reset));
    read_interface r_if_14(.clock(clock),.reset(reset));

    write_interface w_if_15(.clock(clock),.reset(reset));
    read_interface r_if_15(.clock(clock),.reset(reset));
    //16个id 
    int id0 = 0;
    int id1 = 1;
    int id2 = 2;
    int id3 = 3;
    int id4 = 4;
    int id5 = 5;
    int id6 = 6;
    int id7 = 7;
    int id8 = 8;
    int id9 = 9;
    int id10 = 10;
    int id11 = 11;
    int id12 = 12;
    int id13 = 13;
    int id14 = 14;
    int id15 = 15;
    

    initial begin
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env0.i_agt.drv", "wif", w_if_0);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env1.i_agt.drv", "wif", w_if_1);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env2.i_agt.drv", "wif", w_if_2);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env3.i_agt.drv", "wif", w_if_3);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env4.i_agt.drv", "wif", w_if_4);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env5.i_agt.drv", "wif", w_if_5);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env6.i_agt.drv", "wif", w_if_6);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env7.i_agt.drv", "wif", w_if_7);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env8.i_agt.drv", "wif", w_if_8);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env9.i_agt.drv", "wif", w_if_9);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env10.i_agt.drv", "wif", w_if_10);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env11.i_agt.drv", "wif", w_if_11);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env12.i_agt.drv", "wif", w_if_12);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env13.i_agt.drv", "wif", w_if_13);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env14.i_agt.drv", "wif", w_if_14);
        uvm_config_db#(virtual write_interface)::set(null, "uvm_test_top.env15.i_agt.drv", "wif", w_if_15);      
        //uvm_config_db#(virtual my_if)::set(null, "uvm_test_top.env.i_agt.mon", "vif", input_if);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env0.o_agt.mon", "rif", r_if_0);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env1.o_agt.mon", "rif", r_if_1);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env2.o_agt.mon", "rif", r_if_2);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env3.o_agt.mon", "rif", r_if_3);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env4.o_agt.mon", "rif", r_if_4);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env5.o_agt.mon", "rif", r_if_5);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env6.o_agt.mon", "rif", r_if_6);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env7.o_agt.mon", "rif", r_if_7);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env8.o_agt.mon", "rif", r_if_8);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env9.o_agt.mon", "rif", r_if_9);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env10.o_agt.mon", "rif", r_if_10);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env11.o_agt.mon", "rif", r_if_11);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env12.o_agt.mon", "rif", r_if_12);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env13.o_agt.mon", "rif", r_if_13);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env14.o_agt.mon", "rif", r_if_14);
        uvm_config_db#(virtual read_interface)::set(null, "uvm_test_top.env15.o_agt.mon", "rif", r_if_15);

        //向monitor中传递id
        uvm_config_db#(int)::set(null, "uvm_test_top.env0.o_agt.mon", "id", id0);
        uvm_config_db#(int)::set(null, "uvm_test_top.env1.o_agt.mon", "id", id1);
        uvm_config_db#(int)::set(null, "uvm_test_top.env2.o_agt.mon", "id", id2);
        uvm_config_db#(int)::set(null, "uvm_test_top.env3.o_agt.mon", "id", id3);
        uvm_config_db#(int)::set(null, "uvm_test_top.env4.o_agt.mon", "id", id4);
        uvm_config_db#(int)::set(null, "uvm_test_top.env5.o_agt.mon", "id", id5);
        uvm_config_db#(int)::set(null, "uvm_test_top.env6.o_agt.mon", "id", id6);
        uvm_config_db#(int)::set(null, "uvm_test_top.env7.o_agt.mon", "id", id7);
        uvm_config_db#(int)::set(null, "uvm_test_top.env8.o_agt.mon", "id", id8);
        uvm_config_db#(int)::set(null, "uvm_test_top.env9.o_agt.mon", "id", id9);
        uvm_config_db#(int)::set(null, "uvm_test_top.env10.o_agt.mon", "id", id10);
        uvm_config_db#(int)::set(null, "uvm_test_top.env11.o_agt.mon", "id", id11);
        uvm_config_db#(int)::set(null, "uvm_test_top.env12.o_agt.mon", "id", id12);
        uvm_config_db#(int)::set(null, "uvm_test_top.env13.o_agt.mon", "id", id13);
        uvm_config_db#(int)::set(null, "uvm_test_top.env14.o_agt.mon", "id", id14);
        uvm_config_db#(int)::set(null, "uvm_test_top.env15.o_agt.mon", "id", id15);
        

    end

    initial begin
        string testname;
        if($value$plusargs("UVM_TESTNAME=%s",testname))
            `uvm_info("UVM_TOP_TB",$sformatf("RUNNING TEST {%0s} ...",testname),UVM_NONE)
        $fsdbDumpfile({testname,".fsdb"});
        $fsdbDumpvars;
        $fsdbDumpMDA();
		    $fsdbDumpSVA;
        run_test("allport_test");
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

        w_if_2.valid = 0;
        w_if_2.data = 0;
        w_if_2.sop = 0;
        w_if_2.eop = 0;

        w_if_3.valid = 0;
        w_if_3.data = 0;
        w_if_3.sop = 0;
        w_if_3.eop = 0;

        w_if_4.valid = 0;
        w_if_4.data = 0;
        w_if_4.sop = 0;
        w_if_4.eop = 0;

        w_if_5.valid = 0;
        w_if_5.data = 0;
        w_if_5.sop = 0;
        w_if_5.eop = 0;

        w_if_6.valid = 0;
        w_if_6.data = 0;
        w_if_6.sop = 0;
        w_if_6.eop = 0;

        w_if_7.valid = 0;
        w_if_7.data = 0;
        w_if_7.sop = 0;
        w_if_7.eop = 0;

        w_if_8.valid = 0;
        w_if_8.data = 0;
        w_if_8.sop = 0;
        w_if_8.eop = 0;

        w_if_9.valid = 0;
        w_if_9.data = 0;
        w_if_9.sop = 0;
        w_if_9.eop = 0;

        w_if_10.valid = 0;
        w_if_10.data = 0;
        w_if_10.sop = 0;
        w_if_10.eop = 0;

        w_if_11.valid = 0;
        w_if_11.data = 0;
        w_if_11.sop = 0;
        w_if_11.eop = 0;

        w_if_12.valid = 0;
        w_if_12.data = 0;
        w_if_12.sop = 0;
        w_if_12.eop = 0;

        w_if_13.valid = 0;
        w_if_13.data = 0;
        w_if_13.sop = 0;
        w_if_13.eop = 0;

        w_if_14.valid = 0;
        w_if_14.data = 0;
        w_if_14.sop = 0;
        w_if_14.eop = 0;

        w_if_15.valid = 0;
        w_if_15.data = 0;
        w_if_15.sop = 0;
        w_if_15.eop = 0;

        r_if_0.ready = 0;
        r_if_1.ready = 0;
        r_if_2.ready = 0;
        r_if_3.ready = 0;
        r_if_4.ready = 0;
        r_if_5.ready = 0;
        r_if_6.ready = 0;
        r_if_7.ready = 0;
        r_if_8.ready = 0;
        r_if_9.ready = 0;
        r_if_10.ready = 0;
        r_if_11.ready = 0;
        r_if_12.ready = 0;
        r_if_13.ready = 0;
        r_if_14.ready = 0;
        r_if_15.ready = 0;

      reset = 1;
      #30
      reset = 0;
      #`readTime
        r_if_0.ready = 1;
        r_if_1.ready = 1;
        r_if_2.ready = 1;
        r_if_3.ready = 1;
        r_if_4.ready = 1;
        r_if_5.ready = 1;
        r_if_6.ready = 1;
        r_if_7.ready = 1;
        r_if_8.ready = 1;
        r_if_9.ready = 1;
        r_if_10.ready = 1;
        r_if_11.ready = 1;
        r_if_12.ready = 1;
        r_if_13.ready = 1;
        r_if_14.ready = 1;
        r_if_15.ready = 1;
    end
SramTop  SramTop_inst (
  .clock(clock),
  .reset(reset),
  .io_Wr_15_valid(w_if_15.valid),
  .io_Wr_15_data(w_if_15.data),
  .io_Wr_15_ready(w_if_15.ready),
  .io_Wr_15_sop(w_if_15.sop),
  .io_Wr_15_eop(w_if_15.eop),
  .io_Wr_14_valid(w_if_14.valid),
  .io_Wr_14_data(w_if_14.data),
  .io_Wr_14_ready(w_if_14.ready),
  .io_Wr_14_sop(w_if_14.sop),
  .io_Wr_14_eop(w_if_14.eop),
  .io_Wr_13_valid(w_if_13.valid),
  .io_Wr_13_data(w_if_13.data),
  .io_Wr_13_ready(w_if_13.ready),
  .io_Wr_13_sop(w_if_13.sop),
  .io_Wr_13_eop(w_if_13.eop),
  .io_Wr_12_valid(w_if_12.valid),
  .io_Wr_12_data(w_if_12.data),
  .io_Wr_12_ready(w_if_12.ready),
  .io_Wr_12_sop(w_if_12.sop),
  .io_Wr_12_eop(w_if_12.eop),
  .io_Wr_11_valid(w_if_11.valid),
  .io_Wr_11_data(w_if_11.data),
  .io_Wr_11_ready(w_if_11.ready),
  .io_Wr_11_sop(w_if_11.sop),
  .io_Wr_11_eop(w_if_11.eop),
  .io_Wr_10_valid(w_if_10.valid),
  .io_Wr_10_data(w_if_10.data),
  .io_Wr_10_ready(w_if_10.ready),
  .io_Wr_10_sop(w_if_10.sop),
  .io_Wr_10_eop(w_if_10.eop),
  .io_Wr_9_valid(w_if_9.valid),
  .io_Wr_9_data(w_if_9.data),
  .io_Wr_9_ready(w_if_9.ready),
  .io_Wr_9_sop(w_if_9.sop),
  .io_Wr_9_eop(w_if_9.eop),
  .io_Wr_8_valid(w_if_8.valid),
  .io_Wr_8_data(w_if_8.data),
  .io_Wr_8_ready(w_if_8.ready),
  .io_Wr_8_sop(w_if_8.sop),
  .io_Wr_8_eop(w_if_8.eop),
  .io_Wr_7_valid(w_if_7.valid),
  .io_Wr_7_data(w_if_7.data),
  .io_Wr_7_ready(w_if_7.ready),
  .io_Wr_7_sop(w_if_7.sop),
  .io_Wr_7_eop(w_if_7.eop),
  .io_Wr_6_valid(w_if_6.valid),
  .io_Wr_6_data(w_if_6.data),
  .io_Wr_6_ready(w_if_6.ready),
  .io_Wr_6_sop(w_if_6.sop),
  .io_Wr_6_eop(w_if_6.eop),
  .io_Wr_5_valid(w_if_5.valid),
  .io_Wr_5_data(w_if_5.data),
  .io_Wr_5_ready(w_if_5.ready),
  .io_Wr_5_sop(w_if_5.sop),
  .io_Wr_5_eop(w_if_5.eop),
  .io_Wr_4_valid(w_if_4.valid),
  .io_Wr_4_data(w_if_4.data),
  .io_Wr_4_ready(w_if_4.ready),
  .io_Wr_4_sop(w_if_4.sop),
  .io_Wr_4_eop(w_if_4.eop),
  .io_Wr_3_valid(w_if_3.valid),
  .io_Wr_3_data(w_if_3.data),
  .io_Wr_3_ready(w_if_3.ready),
  .io_Wr_3_sop(w_if_3.sop),
  .io_Wr_3_eop(w_if_3.eop),
  .io_Wr_2_valid(w_if_2.valid),
  .io_Wr_2_data(w_if_2.data),
  .io_Wr_2_ready(w_if_2.ready),
  .io_Wr_2_sop(w_if_2.sop),
  .io_Wr_2_eop(w_if_2.eop),
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
  .io_Rd_15_valid(r_if_15.valid),
  .io_Rd_15_data(r_if_15.data),
  .io_Rd_15_ready(r_if_15.ready),
  .io_Rd_15_sop(r_if_15.sop),
  .io_Rd_15_eop(r_if_15.eop),
  .io_Rd_14_valid(r_if_14.valid),
  .io_Rd_14_data(r_if_14.data),
  .io_Rd_14_ready(r_if_14.ready),
  .io_Rd_14_sop(r_if_14.sop),
  .io_Rd_14_eop(r_if_14.eop),
  .io_Rd_13_valid(r_if_13.valid),
    .io_Rd_13_data(r_if_13.data),
    .io_Rd_13_ready(r_if_13.ready),
    .io_Rd_13_sop(r_if_13.sop),
    .io_Rd_13_eop(r_if_13.eop),
    .io_Rd_12_valid(r_if_12.valid),
    .io_Rd_12_data(r_if_12.data),
    .io_Rd_12_ready(r_if_12.ready),
    .io_Rd_12_sop(r_if_12.sop),
    .io_Rd_12_eop(r_if_12.eop),
    .io_Rd_11_valid(r_if_11.valid),
    .io_Rd_11_data(r_if_11.data),
    .io_Rd_11_ready(r_if_11.ready),
    .io_Rd_11_sop(r_if_11.sop),
    .io_Rd_11_eop(r_if_11.eop),
    .io_Rd_10_valid(r_if_10.valid),
    .io_Rd_10_data(r_if_10.data),
    .io_Rd_10_ready(r_if_10.ready),
    .io_Rd_10_sop(r_if_10.sop),
    .io_Rd_10_eop(r_if_10.eop),
    .io_Rd_9_valid(r_if_9.valid),
    .io_Rd_9_data(r_if_9.data),
    .io_Rd_9_ready(r_if_9.ready),
    .io_Rd_9_sop(r_if_9.sop),
    .io_Rd_9_eop(r_if_9.eop),
    .io_Rd_8_valid(r_if_8.valid),
    .io_Rd_8_data(r_if_8.data),
    .io_Rd_8_ready(r_if_8.ready),
    .io_Rd_8_sop(r_if_8.sop),
    .io_Rd_8_eop(r_if_8.eop),
    .io_Rd_7_valid(r_if_7.valid),
    .io_Rd_7_data(r_if_7.data),
    .io_Rd_7_ready(r_if_7.ready),
    .io_Rd_7_sop(r_if_7.sop),
    .io_Rd_7_eop(r_if_7.eop),
    .io_Rd_6_valid(r_if_6.valid),
    .io_Rd_6_data(r_if_6.data),
    .io_Rd_6_ready(r_if_6.ready),
    .io_Rd_6_sop(r_if_6.sop),
    .io_Rd_6_eop(r_if_6.eop),
    .io_Rd_5_valid(r_if_5.valid),
    .io_Rd_5_data(r_if_5.data),
    .io_Rd_5_ready(r_if_5.ready),
    .io_Rd_5_sop(r_if_5.sop),
    .io_Rd_5_eop(r_if_5.eop),
    .io_Rd_4_valid(r_if_4.valid),
    .io_Rd_4_data(r_if_4.data),
    .io_Rd_4_ready(r_if_4.ready),
    .io_Rd_4_sop(r_if_4.sop),
    .io_Rd_4_eop(r_if_4.eop),
    .io_Rd_3_valid(r_if_3.valid),
    .io_Rd_3_data(r_if_3.data),
    .io_Rd_3_ready(r_if_3.ready),
    .io_Rd_3_sop(r_if_3.sop),
    .io_Rd_3_eop(r_if_3.eop),
    .io_Rd_2_valid(r_if_2.valid),
    .io_Rd_2_data(r_if_2.data),
    .io_Rd_2_ready(r_if_2.ready),
    .io_Rd_2_sop(r_if_2.sop),
    .io_Rd_2_eop(r_if_2.eop),
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


