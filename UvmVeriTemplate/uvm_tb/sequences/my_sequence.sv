`ifndef MY_SEQUENCE__SV
`define MY_SEQUENCE__SV

class my_sequence extends uvm_sequence #(my_transaction);
   //my_transaction m_trans;

   function new(string name= "my_sequence");
      super.new(name);
   endfunction

   //virtual task body();
   //   if(starting_phase != null) 
   //      starting_phase.raise_objection(this);
   //   else
   //      `uvm_fatal("my_sequence", "null starting phase!!!")
   //      
   //   //#1000
   //   `uvm_info("my_sequence", "Starting my_sequence body", UVM_LOW)
   //   #50
   //   repeat (2) begin
   //      m_trans = my_transaction::type_id::create("m_trans");
// //        `uvm_do(m_trans)
   //      if(!m_trans.randomize())
   //         `uvm_fatal("my_sequence", "randomize failed")
   //      `uvm_send(m_trans)
   //   end
//
   //   #1000
   //   `uvm_info("my_sequence", "End my_sequence body", UVM_LOW)
   //   if(starting_phase != null) 
   //      starting_phase.drop_objection(this);
   //   else 
   //      `uvm_fatal("my_sequence", "null starting phase!!!")
   //endtask
   //不使用UVM宏 ，可以更自由的操作transaction
   my_transaction m_trans;
   int data_size;
   virtual task body();
      if(starting_phase != null) 
         starting_phase.raise_objection(this);
      else
        `uvm_fatal("my_sequence", "null starting phase!!!")

      //等待复位 
      #50 ;
      //开始
      
      //byte unsigned data_q[];
      
      repeat(`itemnum) begin
         //约束数据包的长度
         `uvm_do_with(m_trans,{
            m_trans.data_queue.size() inside {[64:300]};
      })
         //#300;
      end

      #`SimulationTime
      if(starting_phase != null) 
         starting_phase.drop_objection(this);
      else 
         `uvm_fatal("my_sequence", "null starting phase!!!")
   endtask

   `uvm_object_utils(my_sequence)
endclass
`endif
