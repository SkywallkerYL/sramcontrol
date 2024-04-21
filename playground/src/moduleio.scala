package FFT


import chisel3._
import chisel3.util._
class AxiStream extends Bundle with Config{
  val valid  = Output(Bool())
  val data   = Output(UInt((DataWidthIn*2).W))
  val ready  = Input(Bool())
  val last   = Output(Bool())
}
class AxiStreamSingle extends Bundle with Config{
  val valid  = Output(Bool())
  val data   = Output(UInt((DataWidthIn).W))
  val last   = Output(Bool())
}
class AxiStream256 extends Bundle with Config{
  val valid  = Output(Bool())
  val data   = Output(UInt((256).W))
  val ready  = Input(Bool())
  val last   = Output(Bool())
}

class AxiRead extends Bundle with Config{
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(AXIADDRWIDTH.W))
  val arlen   = Output(UInt(AXILENWIDTH.W))

  val rready  = Output(Bool())
  val rvalid  = Input(Bool())
  val rdata   = Input(UInt(AXIDATAWIDTH.W))
  val rlast   = Input(Bool())
}
class ChannelOut (width: Int) extends Bundle with Config{
  val valid  = Output(Bool())
  val data   = Output(UInt((width).W))
  val ready  = Input(Bool())
  val sop    = Output(Bool())
  val eop    = Output(Bool())
}
class DataChannel (width: Int) extends Bundle with Config{
  val valid  = Input(Bool())
  val data   = Input(UInt((width).W))
  val sop    = Input(Bool())
  val eop    = Input(Bool())
  val prior  = Input(UInt(priorwidth.W))
  val ready  = Output(Bool())
}
class AddrChannel (width: Int) extends Bundle with Config{
  val valid  = Input(Bool())
  val addr   = Input(UInt((width).W))
  val length = Input(UInt((lenwidth).W))
  val prior  = Input(UInt(priorwidth.W))
  val ready  = Output(Bool())
}


