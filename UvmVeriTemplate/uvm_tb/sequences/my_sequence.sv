`ifndef MY_SEQUENCE__SV
`define MY_SEQUENCE__SV

class my_sequence extends uvm_sequence #(my_transaction);
   my_transaction m_trans;

   function new(string name= "my_sequence");
      super.new(name);
   endfunction

   virtual task body();
      if(starting_phase != null) 
         starting_phase.raise_objection(this);
      else
         `uvm_fatal("my_sequence", "null starting phase!!!")
         
      //#1000
      `uvm_info("my_sequence", "Starting my_sequence body", UVM_LOW)
      #50
      repeat (2) begin
         `uvm_do(m_trans)
      end
      #1000
      `uvm_info("my_sequence", "End my_sequence body", UVM_LOW)
      if(starting_phase != null) 
         starting_phase.drop_objection(this);
      else 
         `uvm_fatal("my_sequence", "null starting phase!!!")
   endtask

   `uvm_object_utils(my_sequence)
endclass
`endif
