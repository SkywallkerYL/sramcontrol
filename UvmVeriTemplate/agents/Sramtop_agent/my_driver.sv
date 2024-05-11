`ifndef MY_DRIVER__SV
`define MY_DRIVER__SV
class my_driver extends uvm_driver#(my_transaction);

   //driver ,例化2组写端口 
   virtual write_interface w_if_0 ;
   //virtual write_interface w_if_1 ;

   uvm_analysis_port #(my_transaction)  ap;
   `uvm_component_utils(my_driver)
   function new(string name = "my_driver", uvm_component parent = null);
      super.new(name, parent);
   endfunction

   virtual function void build_phase(uvm_phase phase);
      super.build_phase(phase);
      //依次连接两个写端口
      if(!uvm_config_db#(virtual write_interface)::get(this, "", "wif", w_if_0))
         `uvm_fatal("my_driver", "virtual interface must be set for wif0!!!")
      //if(!uvm_config_db#(virtual write_interface)::get(this, "", "wif1", w_if_1))
      //   `uvm_fatal("my_driver", "virtual interface must be set for wif1!!!")
      ap = new("ap", this);
   endfunction

   extern task main_phase(uvm_phase phase);
   extern task drive_one_pkt(my_transaction tr);
endclass

task my_driver::main_phase(uvm_phase phase);
   my_transaction tr;
   //phase.raise_objection(this);
   `uvm_info("my_driver", "main phase is called", UVM_LOW);
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
   //#100
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
   //这里先简单一点，给两个端口都传一样的数据

   //repeat(3) @(posedge wif.wclk);

   for ( int i = 0; i < data_size; i++ ) begin
      @(posedge w_if_0.clock);
      w_if_0.valid <= 1'b1;
      w_if_0.data <= data_q[i]; 
      if(i == 0) begin
         w_if_0.sop <= 1'b1;
      end
      else begin
         w_if_0.sop <= 1'b0;
      end
      if(i == data_size - 1) begin
         w_if_0.eop <= 1'b1;
      end
      else begin
         w_if_0.eop <= 1'b0;
      end
      if(!w_if_0.ready) begin
         i = i - 1;
      end
      //$display("port 0 write data: %d",data_q[i]);
   end
   @(posedge w_if_0.clock);
   w_if_0.valid <= 1'b0;
   w_if_0.eop <= 1'b0;
   `uvm_info("my_driver", "end drive one pkt", UVM_LOW);
endtask


`endif
