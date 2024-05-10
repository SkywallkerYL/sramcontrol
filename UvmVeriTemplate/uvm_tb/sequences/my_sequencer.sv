`ifndef MY_SEQUENCER__SV
`define MY_SEQUENCER__SV

class my_sequencer extends uvm_sequencer #(my_transaction);
   
   function new(string name, uvm_component parent);
      super.new(name, parent);
   endfunction 
   extern virtual task main_phase(uvm_phase phase);
   `uvm_component_utils(my_sequencer)
endclass
task my_sequencer::main_phase(uvm_phase phase);
   my_sequence seq;
   phase.raise_objection(this);
   `uvm_info("my_sequencer", "build sequence", UVM_LOW)
   seq = my_sequence::type_id::create("seq");
   seq.starting_phase = phase;
   seq.start(this);
   //#10000;
   phase.drop_objection(this);
endtask
`endif
