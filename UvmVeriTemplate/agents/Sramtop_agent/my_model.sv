`ifndef MY_MODEL__SV
`define MY_MODEL__SV

class my_model extends uvm_component;
   
   uvm_blocking_get_port #(my_transaction)  port;
   uvm_analysis_port #(my_transaction)  ap;

   my_transaction queue[$];  // 用于存储和排序事务的队列

   extern function new(string name, uvm_component parent);
   extern function void build_phase(uvm_phase phase);
   extern virtual  task main_phase(uvm_phase phase);
   //string str;  // 在类的级别声明变量
//
   //function void process_transaction(my_transaction new_tr);
   //   str = new_tr.convert2string();  // 在方法中使用变量
   //   $display("%s", str);
   //endfunction
   `uvm_component_utils(my_model)

   //实现一个对队列的排序函数 
   //优先级为pri = (queue[i][0] & 8'h70) >> 4;
   //优先级越大的放在越后面 越先被取出
   function void mysort();
      my_transaction tmp;
      int i, j;
      for (i = 0; i < queue.size(); i++) begin
         for (j = i+1; j < queue.size(); j++) begin
            if(((queue[i].data_queue[0] & 8'h70) >> 4) > ((queue[j].data_queue[0]  & 8'h70) >> 4)) begin
               tmp = queue[i];
               queue[i] = queue[j];
               queue[j] = tmp;
            end
         end
      end
   endfunction
endclass 

function my_model::new(string name, uvm_component parent);
   super.new(name, parent);
endfunction 

function void my_model::build_phase(uvm_phase phase);
   super.build_phase(phase);
   port = new("port", this);
   ap = new("ap", this);
endfunction

task my_model::main_phase(uvm_phase phase);
   my_transaction tr;
   my_transaction new_tr;
   super.main_phase(phase);
   //8个二维数组，用于存储事务 8个优先级，每个优先级有一个二维数组
   
   //while(1) begin
   //   port.get(tr);
   //   new_tr = new("new_tr");
   //   new_tr.copy(tr);
   //   //实现一个Model  对事务进行单通道的处理， 
   //   //一个transaction的第一个数据是当前的优先级，将其保存下来，然后等待 读出 
   //   // 将transaction中的数据进行排序，
   //   `uvm_info("my_model", "get one transaction, copy and print it:", UVM_LOW)
   //   //new_tr.my_print();
   //   //process_transaction(new_tr);
   //   ap.write(new_tr);
   //end

   //包含对优先级的检查，但是要求所有数据写入后，再开始读出，因为这个是直接对所有数据进行排序
   while (1) begin
      port.get(tr);
      new_tr = new("new_tr");
      new_tr.copy(tr);
      queue.push_front(new_tr);  // 将新事务添加到队列的前面
      //ap.write(queue.pop_front());
      //queue.sort();  // 对队列进行排序，优先级最高的事务在前面
      mysort();
      //`uvm_info("my_model", "get one transaction, copy and sort it:", UVM_LOW)
      if (queue.size() >=`itemnum ) begin
         break;  // 将优先级最高的事务发送到分析端口
      end
   end
   while (1) begin
      if(queue.size() == 0) begin
         break;
      end
      else begin
         //把事务从队列中取出来，从后往前取
         new_tr = queue.pop_back();
         ap.write(new_tr);
      end
   end
endtask
`endif
