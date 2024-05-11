`ifndef MY_TRANSACTION__SV
`define MY_TRANSACTION__SV
    import uvm_pkg::*;
    `include "uvm_macros.svh"
class my_transaction extends uvm_sequence_item;
//rand bit[7:0] data;：这行代码定义了一个名为 data 的随机位字段，
//它的位宽为 8 位。在 UVM 中，rand 关键字用于声明随机变量。
//声明随机长度的8bitdata
   //rand bit[7:0] data;
   rand byte unsigned data_queue[];
   //rand int nums; 
// 添加一个约束，限制 data 数组的长度在 20 到 100
   //constraint data_len_c { data_queue.size() inside {[64:100]}; }
   //constraint data_len_c { data.size() inside {[20:100]}; }
   //constraint cstr {
   //   nums      inside {[62:100]};
   //   data.size()   == nums;
   //}
   //`uvm_feild_array_int(data_queue, UVM_ALL_ON)
   //rand bit[7:0] data;
   //`uvm_info("my_transaction", "my_transaction build", UVM_LOW)
   `uvm_object_utils_begin(my_transaction)
   //   for (int i = 0; i < data_queue.size(); i++) begin
   //      `uvm_field_int(data_queue[i], UVM_ALL_ON)
   //   end
   ////// `uvm_field_int(data, UVM_ALL_ON)
      `uvm_field_array_int(data_queue, UVM_ALL_ON)
   `uvm_object_utils_end
   //virtual task body ();
   //   `uvm_info("my_transaction", "my_transaction body", UVM_LOW)
   //   for (int i = 0; i < data_queue.size(); i++) begin
   //      data_queue.push_back($urandom_range(0, 255));
   //   end
   //endtask //body ()
   function new(string name = "my_transaction");
      super.new();
   endfunction
   //function void my_print();
   //   $display("nums = %0d", 100);
   //   for (int i = 0; i< 100; i ++ ) begin
   //      $display("data[%0d] = 0x%0h", i, data[i]);
   //   end
   //      //$display("data[%0d] = 0x%0h", i, data[i]);
   //endfunction
    // 随机化函数
   // 打印函数
   function int getlen();
      
      return data_queue.size();
   endfunction
   function writepack(int data[],int len);
      for (int i = 0; i < len; i++) begin
         data_queue[i] = (data[i]);
      end
   endfunction
   function void my_print();
      int len = getlen();
      `uvm_info("my_transaction", $sformatf("data size is %0d",len), UVM_LOW)
      for (int i = 0; i< len; i ++ ) begin
         $display("data[%0d] = 0x%0h", i, data_queue[i]);
      end
   endfunction
endclass

//function void my_print(bit data);
//   $display("data = %0h",data);
//   
//endfunction
//function void my_copy(my_transaction tr); 
//   //data = tr.data;
//endfunction
`endif
