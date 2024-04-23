package FFT


import chisel3._
import chisel3.util._
class AxiStream(width : Int) extends Bundle with Config{
  val valid  = Output(Bool())
  val data   = Output(UInt((width).W))
  val ready  = Input(Bool())
  val last   = Output(Bool())
}
class WriterIO(size: Int) extends Bundle {
    val write = Input(Bool())
    val full = Output(Bool())
    val din = Input(UInt(size.W))
}
class ReaderIO(size: Int) extends Bundle {
    val read = Input(Bool())
    val empty = Output(Bool())
    val dout = Output(UInt(size.W))
}


class SyncFifoIO(width: Int) extends Bundle with Config {
  val fiforead = new ReaderIO(width)
  val fifowrite= new WriterIO(width)
}
//基本的RAM读写端口
class RamRead(addrwidth:Int, datawidth : Int) extends Bundle with Config{
  val rdData = Input(UInt(datawidth.W))
  val rden   = Output(Bool())
  val rdAddr = Output(UInt(addrwidth.W))  
  val rdValid = Input(Bool())   
}
class RamWrite(addrwidth:Int, datawidth : Int) extends Bundle with Config{
  val wrData = Output(UInt(datawidth.W))
  val wren   = Output(Bool())
  val wrAddr = Output(UInt(addrwidth.W))  
  val wrValid = Input(Bool())   
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


