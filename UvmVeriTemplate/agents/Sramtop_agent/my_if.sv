`ifndef MY_IF__SV
`define MY_IF__SV

// 写端口接口
interface write_interface(input logic clock, input logic reset);
logic valid;
logic [7:0] data; // 假设数据宽度为8位
logic ready;
logic sop;
logic eop;

//modport master(input  clock, input  reset,output valid, output data, input ready, output sop, output eop);
//modport slave(input  clock, input  reset,input valid, input data, output ready, input sop, input eop);
endinterface

// 读端口接口
interface read_interface(input logic clock, input logic reset);
logic valid;
logic [7:0] data; // 假设数据宽度为8位
logic ready;
logic sop;
logic eop;

//modport master(input  clock, input  reset,output valid, output data, input ready, output sop, output eop);
//modport slave(input  clock, input  reset,input valid, input data, output ready, input sop, input eop);
endinterface


`endif
