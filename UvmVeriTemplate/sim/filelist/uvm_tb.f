// // ---------------------------------------------------------------------------------
// //                 Copyright (c) 2022 
// //                 ALL RIGHTS RESERVED
// // ---------------------------------------------------------------------------------
// // Filename      : uvm_tb.f
// // Author        : AiF
// // Created On    : 2022-05-15 21:12
// // Last Modified : 
// // ---------------------------------------------------------------------------------
// // Description   : 
// //
// //
// // ---------------------------------------------------------------------------------

//+incdir+../uvm_tb/env
+incdir+../uvm_tb/sequences
../agents/Sramtop_agent/my_macro.sv
//+incdir+../uvm_tb/virtual_sequences
//+incdir+../uvm_tb/tests

//../uvm_tb/env/my_env.sv
../uvm_tb/sequences/my_transaction.sv
../uvm_tb/sequences/my_sequence.sv
../uvm_tb/sequences/my_sequencer.sv


// //../uvm_tb/virtual_sequences/asyn_fifo_vseq_pkg.sv

//../uvm_tb/tests/base_test.sv
// ../uvm_tb/**/*.sv