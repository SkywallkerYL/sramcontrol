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



class my_onechannel_sequence extends uvm_sequence #(my_transaction);
   //my_transaction m_trans;

   function new(string name= "my_onechannel_sequence",int id=0);
      super.new(name);
      this.id = id;
   endfunction

   my_transaction m_trans;
   int data_size;
   //int id;
   //这个数值是先跑了一遍确定的
   int id; 
   //((get_inst_id()-7580)/10) & 8'h0f;
     //一个随机byte
   byte unsigned data_q;
   
   //`uvm_info("my_onechannel_sequence", $sformatf("my_onechannel_sequence id is %0d",id), UVM_LOW)
   
   virtual task body();
      if(starting_phase != null) 
         starting_phase.raise_objection(this);
      else
        `uvm_fatal("my_onechannel_sequence", "null starting phase!!!")

      //等待复位 
      #50 ;
      //开始
      
      //byte unsigned data_q[];
      //还要添加一个约束,根据当前Sequence的id来约束数据包的第一个数据
      //获取当前sequence的id
      //$display("my_onechannel_sequence id is %0d",id);
      repeat(`itemnum) begin
         //约束数据包的长度
         `uvm_do_with(m_trans,{
            m_trans.data_queue.size() inside {[64:300]};
            m_trans.data_queue[0] == ((m_trans.data_queue[0]&8'hf0)|(id&8'h0f));
            
      })
         //$display("my_onechannel_sequence id is %0d and dataone is %0x",id,m_trans.data_queue[0]);
         //#20;
      end

      #`SimulationTime
      if(starting_phase != null) 
         starting_phase.drop_objection(this);
      else 
         `uvm_fatal("my_onechannel_sequence", "null starting phase!!!")
   endtask

   `uvm_object_utils(my_onechannel_sequence)
endclass

`endif
