`ifndef BASE_TEST__SV
`define BASE_TEST__SV

class allport_test extends uvm_test;
//16 个端口的测试 分配16组测试环境
   my_env        env0;
   my_env        env1;
   my_env        env2;
   my_env        env3;
   my_env        env4;
   my_env        env5;
   my_env        env6;
   my_env        env7;
   my_env        env8;
   my_env        env9;
   my_env        env10;
   my_env        env11;
   my_env        env12;
   my_env        env13;
   my_env        env14;
   my_env        env15;

   
   function new(string name = "allport_test", uvm_component parent = null);
      super.new(name,parent);
   endfunction
   
   extern virtual function void build_phase(uvm_phase phase);
   extern virtual function void report_phase(uvm_phase phase);
   `uvm_component_utils(allport_test)
endclass


function void allport_test::build_phase(uvm_phase phase);
   //15组sequence
   my_onechannel_sequence seq0;
   my_onechannel_sequence seq1;
   my_onechannel_sequence seq2;
   my_onechannel_sequence seq3;
   my_onechannel_sequence seq4;
   my_onechannel_sequence seq5;
   my_onechannel_sequence seq6;
   my_onechannel_sequence seq7;
   my_onechannel_sequence seq8;
   my_onechannel_sequence seq9;
   my_onechannel_sequence seq10;
   my_onechannel_sequence seq11;
   my_onechannel_sequence seq12;
   my_onechannel_sequence seq13;
   my_onechannel_sequence seq14;
   my_onechannel_sequence seq15;
   super.build_phase(phase);
   env0  =  my_env::type_id::create("env0", this);
   env1  =  my_env::type_id::create("env1", this); 
   env2  =  my_env::type_id::create("env2", this);
   env3  =  my_env::type_id::create("env3", this);
   env4  =  my_env::type_id::create("env4", this);
   env5  =  my_env::type_id::create("env5", this);
   env6  =  my_env::type_id::create("env6", this);
   env7  =  my_env::type_id::create("env7", this);
   env8  =  my_env::type_id::create("env8", this);
   env9  =  my_env::type_id::create("env9", this);
   env10  =  my_env::type_id::create("env10", this);
   env11  =  my_env::type_id::create("env11", this);
   env12  =  my_env::type_id::create("env12", this);
   env13  =  my_env::type_id::create("env13", this);
   env14  =  my_env::type_id::create("env14", this);
   env15  =  my_env::type_id::create("env15", this);
   //uvm_config_db#(uvm_object_wrapper)::set(this,
   //                                        "env.i_agt.sqr.main_phase",
   //                                        "default_sequence",
   //                                         my_sequence::type_id::get());
   // 设置两个default_sequence
   //不在sequencer内部声明sequence，而是在test中声明sequence\
   //这是一种设置default_sequence的方法
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env0.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env1.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env2.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env3.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env4.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env5.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env6.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env7.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env8.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env9.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env10.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env11.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env12.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env13.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env14.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   
   //seq = my_onechannel_sequence::type_id::create("seq");
   //seq.id = 15; // 设置你想要的id
   //这里使用另一种
   seq0 = new("seq",0);
   uvm_config_db#(uvm_sequence_base)::set(this,"env0.i_agt.sqr.main_phase","default_sequence",seq0);
   seq1 = new("seq",1);
   uvm_config_db#(uvm_sequence_base)::set(this,"env1.i_agt.sqr.main_phase","default_sequence",seq1);
   seq2 = new("seq",2);
   uvm_config_db#(uvm_sequence_base)::set(this,"env2.i_agt.sqr.main_phase","default_sequence",seq2);
   seq3 = new("seq",3);
   uvm_config_db#(uvm_sequence_base)::set(this,"env3.i_agt.sqr.main_phase","default_sequence",seq3);
   seq4 = new("seq",4);
   uvm_config_db#(uvm_sequence_base)::set(this,"env4.i_agt.sqr.main_phase","default_sequence",seq4);
   seq5 = new("seq",5);
   uvm_config_db#(uvm_sequence_base)::set(this,"env5.i_agt.sqr.main_phase","default_sequence",seq5);
   seq6 = new("seq",6);
   uvm_config_db#(uvm_sequence_base)::set(this,"env6.i_agt.sqr.main_phase","default_sequence",seq6);
   seq7 = new("seq",7);
   uvm_config_db#(uvm_sequence_base)::set(this,"env7.i_agt.sqr.main_phase","default_sequence",seq7);
   seq8 = new("seq",8);
   uvm_config_db#(uvm_sequence_base)::set(this,"env8.i_agt.sqr.main_phase","default_sequence",seq8);
   seq9 = new("seq",9);
   uvm_config_db#(uvm_sequence_base)::set(this,"env9.i_agt.sqr.main_phase","default_sequence",seq9);
   seq10 = new("seq",10);
   uvm_config_db#(uvm_sequence_base)::set(this,"env10.i_agt.sqr.main_phase","default_sequence",seq10);
   seq11 = new("seq",11);
   uvm_config_db#(uvm_sequence_base)::set(this,"env11.i_agt.sqr.main_phase","default_sequence",seq11);
   seq12 = new("seq",12);
   uvm_config_db#(uvm_sequence_base)::set(this,"env12.i_agt.sqr.main_phase","default_sequence",seq12);
   seq13 = new("seq",13);
   uvm_config_db#(uvm_sequence_base)::set(this,"env13.i_agt.sqr.main_phase","default_sequence",seq13);
   seq14 = new("seq",14);
   uvm_config_db#(uvm_sequence_base)::set(this,"env14.i_agt.sqr.main_phase","default_sequence",seq14);
   seq15 = new("seq",15);
   uvm_config_db#(uvm_sequence_base)::set(this,"env15.i_agt.sqr.main_phase","default_sequence",seq15);
   //uvm_config_db#(uvm_object_wrapper)::set(this,"env15.i_agt.sqr.main_phase","default_sequence",my_onechannel_sequence::type_id::get());
   //给每一个Sequence设置一个不同的ID


endfunction

function void allport_test::report_phase(uvm_phase phase);
   uvm_report_server server;
   int err_num;
   super.report_phase(phase);

   server = get_report_server();
   err_num = server.get_severity_count(UVM_ERROR);
//
   if (err_num != 0) begin
      $display("TEST CASE FAILED and Error num is %0d",err_num);
   end
   else begin
      $display("TEST CASE PASSED");
   end
endfunction

`endif
