`ifndef MY_ENV__SV
`define MY_ENV__SV

class my_env extends uvm_env;

   my_agent   i_agt;
   my_agent   o_agt;
   my_model   mdl;
   my_scoreboard scb;
   
   uvm_tlm_analysis_fifo #(my_transaction) agt_scb_fifo;
   uvm_tlm_analysis_fifo #(my_transaction) agt_mdl_fifo;
   uvm_tlm_analysis_fifo #(my_transaction) mdl_scb_fifo;
   /***
agt_scb_fifo：
这个 FIFO 可能用于从代理（agent）传递事务到记分板（scoreboard）。
在这个过程中，代理会生成事务，然后将它们放入这个 FIFO，记分板则会从这个 FIFO 中取出事务进行处理。

agt_mdl_fifo：
这个 FIFO 可能用于从代理传递事务到模型（model）。
代理会生成事务，然后将它们放入这个 FIFO，模型则会从这个 FIFO 中取出事务进行处理。

mdl_scb_fifo：
这个 FIFO 可能用于从模型传递事务到记分板。
模型会生成事务，然后将它们放入这个 FIFO，记分板则会从这个 FIFO 中取出事务进行处理。
   ***/
   function new(string name = "my_env", uvm_component parent);
      super.new(name, parent);
   endfunction

   virtual function void build_phase(uvm_phase phase);
      super.build_phase(phase);
      i_agt = my_agent::type_id::create("i_agt", this);
      o_agt = my_agent::type_id::create("o_agt", this);
      i_agt.is_active = UVM_ACTIVE;
      o_agt.is_active = UVM_PASSIVE;
      mdl = my_model::type_id::create("mdl", this);
      scb = my_scoreboard::type_id::create("scb", this);
      agt_scb_fifo = new("agt_scb_fifo", this);
      agt_mdl_fifo = new("agt_mdl_fifo", this);
      mdl_scb_fifo = new("mdl_scb_fifo", this);

   endfunction

   extern virtual function void connect_phase(uvm_phase phase);
   
   `uvm_component_utils(my_env)
endclass

function void my_env::connect_phase(uvm_phase phase);
   super.connect_phase(phase);
   i_agt.ap.connect(agt_mdl_fifo.analysis_export);
   //这行代码将输入代理的分析端口连接到 agt_mdl_fifo 的分析导出。这意味着输入代理可以将事务放入 agt_mdl_fifo。
   mdl.port.connect(agt_mdl_fifo.blocking_get_export);
   //这行代码将模型的端口连接到 agt_mdl_fifo 的阻塞获取导出。这意味着模型可以从 agt_mdl_fifo 中获取事务。
   mdl.ap.connect(mdl_scb_fifo.analysis_export);
   //这行代码将模型的分析端口连接到 mdl_scb_fifo 的分析导出。这意味着模型可以将事务放入 mdl_scb_fifo。
   scb.exp_port.connect(mdl_scb_fifo.blocking_get_export);
   //这行代码将记分板的导出端口连接到 mdl_scb_fifo 的阻塞获取导出。这意味着记分板可以从 mdl_scb_fifo 中获取事务。
   o_agt.ap.connect(agt_scb_fifo.analysis_export);
   //这行代码将输出代理的分析端口连接到 agt_scb_fifo 的分析导出。这意味着输出代理可以将事务放入 agt_scb_fifo。
   scb.act_port.connect(agt_scb_fifo.blocking_get_export); 
   //这行代码将记分板的活动端口连接到 agt_scb_fifo 的阻塞获取导出。这意味着记分板可以从 agt_scb_fifo 中获取事务。
endfunction

`endif
