`ifndef MY_DRIVER__SV
`define MY_DRIVER__SV
class my_driver extends uvm_driver#(my_transaction);

   virtual fifowrite_if wif;

   uvm_analysis_port #(my_transaction)  ap;
   `uvm_component_utils(my_driver)
   function new(string name = "my_driver", uvm_component parent = null);
      super.new(name, parent);
   endfunction

   virtual function void build_phase(uvm_phase phase);
      super.build_phase(phase);
      if(!uvm_config_db#(virtual fifowrite_if)::get(this, "", "wif", wif))
         `uvm_fatal("my_driver", "virtual interface must be set for wif!!!")
      ap = new("ap", this);
   endfunction

   extern task main_phase(uvm_phase phase);
   extern task drive_one_pkt(my_transaction tr);
endclass

task my_driver::main_phase(uvm_phase phase);
   my_transaction tr;
   //phase.raise_objection(this);
   `uvm_info("my_driver", "main phase is called", UVM_LOW);
   wif.data <= 15'b0;
   wif.writeen <= 1'b0;
   //#1000
   //for(int i = 0; i < 3; i++) begin
   //   tr = new("tr");
   //   assert(tr.randomize());
   //   drive_one_pkt(tr);
   //   ap.write(tr);
   //end
   //#10000
   
   //while(wif.rst)
   //   @(posedge wif.clk);
   while(1) begin
      seq_item_port.get_next_item(req);
      drive_one_pkt(req);
      ap.write(req);
      seq_item_port.item_done();
   end
   //phase.drop_objection(this);
endtask

task my_driver::drive_one_pkt(my_transaction tr);
   byte unsigned     data_q[];
   int  data_size;
   
   data_size = tr.pack_bytes(data_q) / 8; 
   `uvm_info("my_driver", $sformatf("begin to drive one pkt with size %0d",data_size), UVM_LOW);
   //wif.clk = 1;
   repeat(3) @(posedge wif.wclk);
   //`uvm_info("my_driver", "clkwait", UVM_LOW);
   for ( int i = 0; i < data_size; i++ ) begin
      @(posedge wif.wclk);
      wif.writeen <= 1'b1;
      wif.data <= data_q[i]; 
      $display("write data: %d",data_q[i]);
   end

   @(posedge wif.wclk);
   //wif.valid <= 1'b0;
   wif.writeen <= 1'b0;
   `uvm_info("my_driver", "end drive one pkt", UVM_LOW);
endtask


`endif
