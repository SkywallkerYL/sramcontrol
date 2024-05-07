`ifndef MY_TRANSACTION__SV
`define MY_TRANSACTION__SV

class my_transaction extends uvm_sequence_item;

   rand bit[15:0] data;

   `uvm_object_utils_begin(my_transaction)
      `uvm_field_int(data, UVM_ALL_ON)
   `uvm_object_utils_end

   function new(string name = "my_transaction");
      super.new();
   endfunction
   extern function my_print();
   extern function my_copy(my_transaction tr);
endclass

function void my_print();
   //$display("data = %0h",data);
   
endfunction
function void my_copy(my_transaction tr); 
   //data = tr.data;
endfunction
`endif
