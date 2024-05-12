`ifndef MY_MONITOR__SV
`define MY_MONITOR__SV
class my_monitor extends uvm_monitor;

   //monitor 例化2组读端口
   virtual read_interface r_if_0;
   //virtual read_interface r_if_1;

   uvm_analysis_port #(my_transaction)  ap;
   
   `uvm_component_utils(my_monitor)
   function new(string name = "my_monitor", uvm_component parent = null);
      super.new(name, parent);
   endfunction

   virtual function void build_phase(uvm_phase phase);
      super.build_phase(phase);
      if(!uvm_config_db#(virtual read_interface)::get(this, "", "rif", r_if_0))
         `uvm_fatal("my_monitor", "virtual interface must be set for rif0!!!")
      //if(!uvm_config_db#(virtual read_interface)::get(this, "", "rif1", r_if_1))
         //`uvm_fatal("my_monitor", "virtual interface must be set for rif1!!!")
      ap = new("ap", this);
   endfunction

   extern task main_phase(uvm_phase phase);
   extern task collect_one_pkt(my_transaction tr);
endclass

task my_monitor::main_phase(uvm_phase phase);
   my_transaction tr;
   //#10000
   while(1) begin
      tr = new("tr");
      collect_one_pkt(tr);
      //tr.my_print();
      //tr中的数据非空，就把数据写到ap端口
      //if (tr. ) begin
      //   
      //end
      ap.write(tr);
   end
endtask

task my_monitor::collect_one_pkt(my_transaction tr);
   byte unsigned data_q[$];
   byte unsigned data_array[];
   logic [7:0] data;
   logic readen = 0;
   logic empty = 0;
   int data_size;   
   `uvm_info("my_monitor", "begin to collect one pkt", UVM_LOW);
   //Monitor直接从dut读取数据，和model的数据进行比较，如果不一致
   //从端口0读取数据
   //等待端口0的数据拉高
   while (1) begin
      @(posedge r_if_0.clock);
      if (r_if_0.valid && r_if_0.ready) begin
         data = r_if_0.data;
         data_q.push_back(data);
         //$display("data = %0h",data);
         break;
      end
   end
   while (1) begin
      @(posedge r_if_0.clock);
      if (r_if_0.valid && r_if_0.ready && !r_if_0.eop) begin
         data = r_if_0.data;
         data_q.push_back(data);
         //$display("data = %0h",data);
      end
      else if(r_if_0.eop && r_if_0.valid && r_if_0.ready) begin
         break;
      end
   end

   //r_if_0.ready <= 1'b0;
   data_size  = data_q.size();   
   data_array = new[data_size];
   for ( int i = 0; i < data_size; i++ ) begin
      data_array[i] = data_q[i];
      //tr.print(data);
   end
   //$display("datasize = %d",data_size);
   `uvm_info("my_monitor", $sformatf("data_size = %0d",data_size), UVM_LOW);
   //从端口1读取数据
   //r_if_1.ready <= 1'b1;
   //while (1) begin
   //   @(posedge r_if_1.clock);
   //   if (r_if_1.valid) begin
   //      data = r_if_1.data;
   //      data_q.push_back(data);
   //      if(r_if_1.eop) begin
   //         break;
   //      end
   //   end
   //   else begin
   //      break;
   //   end
   //end
   
   //这里使用unpack_bytes函数将data_q中的byte流转换成tr中的各个字段。unpack_bytes函数的输入参数必须是一个动态数组，所
//以需要先把收集到的、放在data_q中的数据复制到一个动态数组中。由于tr中的pload是一个动态数组，所以需要在调用
//unpack_bytes之前指定其大小，这样unpack_bytes函数才能正常工作。
   //tr.pload = new[data_size - 18]; //da sa, e_type, crc
   //data_array非空，把data_array中的数据转换成tr中的各个字段
   if(data_array.size() != 0) begin
      //tr.unpack_bytes(data_array);
      //直接用这个unpack_bytes函数,好像不行
      //给tr中的data_queue分配空间
      tr.data_queue = new[data_size];
      for(int i = 0; i < data_size; i++) begin
         tr.data_queue[i] = (data_array[i]);
      end
      `uvm_info("my_monitor", "unpack_bytes", UVM_LOW);
   end
   //data_size = tr.unpack_bytes(data_array) / 8; 
   ///tr.my_print();
   `uvm_info("my_monitor", "end collect one pkt", UVM_LOW);
endtask


`endif
