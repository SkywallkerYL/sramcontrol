`ifndef MY_TRANSACTION__SV
`define MY_TRANSACTION__SV

class my_transaction extends uvm_sequence_item;
//rand bit[7:0] data;：这行代码定义了一个名为 data 的随机位字段，
//它的位宽为 8 位。在 UVM 中，rand 关键字用于声明随机变量。
//声明随机长度的8bitdata
   //rand bit[7:0] data;
   rand byte unsigned data[];
   rand int nums; 
// 添加一个约束，限制 data 数组的长度在 20 到 100
   //constraint data_len_c { data.size() inside {[20:100]}; }
   constraint cstr {
      nums      inside {[62:100]};
      data.size()   == nums;
   }
   //rand bit[7:0] data;
   //`uvm_info("my_transaction", "my_transaction build", UVM_LOW)
   `uvm_object_utils_begin(my_transaction)
      //`uvm_field_array_byte_unsigned(data, UVM_ALL_ON)
      for (int i = 0; i< nums; i ++ ) begin
         `uvm_field_int(data[i], UVM_ALL_ON)
      end
      //`uvm_field_int(data, UVM_ALL_ON)
   `uvm_object_utils_end

   function new(string name = "my_transaction");
      super.new();
   endfunction
   //extern function my_print(bit data);
   //extern function my_copy(my_transaction tr);
endclass

//function void my_print(bit data);
//   $display("data = %0h",data);
//   
//endfunction
//function void my_copy(my_transaction tr); 
//   //data = tr.data;
//endfunction
`endif
