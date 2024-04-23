package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._




class Asynfifo(val size : Int, val width : Int) extends Module {
  val io = IO(new Bundle {
    val read = new ReaderIO(width)
    val rclk    = Input(Clock())
    val rrst  = Input(Bool()) 

    val write = new WriterIO(width)
    val wclk  = Input(Clock())
    val wrst  = Input(Bool())
  })
  val AddrSize = log2Ceil(size)+1
  //size 是地址的位宽
  val mem = Mem(size, UInt(width.W))

  val Wptr = Wire(UInt(AddrSize.W)) 
  val Rptr = Wire(UInt(AddrSize.W))
  //写数据端口
  withClockAndReset(io.wclk,io.wrst){
    val wPointer = RegInit(0.U((AddrSize).W))
    val wAddr = wPointer(AddrSize-2,0)
    when(io.write.write && (!io.write.full)){
      mem(wAddr) := io.write.din 
      wPointer := wPointer + 1.U 
    }

    val wPtrGray = wPointer ^ (wPointer >> 1.U)
    Wptr := wPtrGray
    val rptr2w = RegNext(RegNext(Rptr))

    io.write.full := Wptr === ((~rptr2w(AddrSize-1)) ## (~rptr2w(AddrSize-2))## rptr2w(AddrSize-3,0))

  }
  //读数据端口
  //io.read.dout := 0.U 
  withClockAndReset(io.rclk,io.rrst){
    val Dout = RegInit(0.U(width.W))
    io.read.dout := Dout 
    val rPointer = RegInit(0.U((AddrSize).W))
    val rAddr = rPointer(AddrSize-2,0)
    when(io.read.read && (!io.read.empty)){
      Dout := mem(rAddr)  
      rPointer := rPointer + 1.U 
    }
    val rPtrGray = rPointer ^ (rPointer >> 1.U) 

    Rptr := rPtrGray
    val wptr2r = RegNext(RegNext(Wptr))

    io.read.empty := Rptr === wptr2r
  }

}
class fifo(val size : Int, val width : Int) extends Module {
  val io = IO(new Bundle {
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
    val writeFlag = Input(Bool())
    val readFlag = Input(Bool())
    val full = Output(Bool())
    val empty = Output(Bool())
  })

  val count = RegInit(0.U((log2Ceil(size)+1).W))
  val mem = Mem(size, UInt(width.W))
  val wPointer = RegInit(0.U((log2Ceil(size)).W))
  val rPointer = RegInit(0.U((log2Ceil(size)).W))
  val dataOut = RegInit(0.U(width.W))

  def indexAdd(index : UInt) : UInt = {
      Mux(index === (size - 1).U, 0.U, index + 1.U)
  }

  when(io.writeFlag === true.B && io.readFlag === true.B) {
    when(count === 0.U) { dataOut := io.dataIn }
    .otherwise {
      dataOut := mem(rPointer)
      rPointer := indexAdd(rPointer)
      mem(wPointer) := io.dataIn
      wPointer := indexAdd(wPointer)
    } 
  } .elsewhen (io.writeFlag === true.B && io.readFlag === false.B) {
    dataOut := 0.U
    when(count < size.U) {
      mem(wPointer) := io.dataIn
      wPointer := indexAdd(wPointer)
      count := count + 1.U
    }
  } .elsewhen (io.writeFlag === false.B && io.readFlag === true.B) {
    when(count > 0.U) {
      dataOut := mem(rPointer)
      rPointer := indexAdd(rPointer)
      count := count - 1.U
    } .otherwise {
      dataOut := 0.U
    }
  } .otherwise {
    dataOut := 0.U
  }

  io.dataOut := dataOut
  io.full := (size.U === count)
  io.empty := (count === 0.U)
}
/*******
实现一个具有flush功能的fifo

当有flush信号时，将读指针定位到写指针
然后呢，使用外部的RAM 


*******/

class flushramfifo(val size : Int, val width : Int) extends Module {
  val io = IO(new Bundle {
    val dataIn = Input(UInt(width.W))
    val dataOut = Output(UInt(width.W))
    val writeFlag = Input(Bool())
    val readFlag = Input(Bool())
    val full = Output(Bool())
    val empty = Output(Bool())
    val flush = Input(Bool())
  })
  val addrwidth = (log2Ceil(size))

  val count = RegInit(0.U((log2Ceil(size)+1).W))
  
  
  val wPointer = RegInit(0.U(addrwidth.W))
  val rPointer = RegInit(0.U(addrwidth.W))
  //使用的RAM 模型是不带寄存器的，即这一个周期申请读，下一个周期就能拿到数据
  val mem = Module(new ramblackbox(addrwidth,width))// Mem(size, UInt(width.W))
  mem.io.read.rden := io.readFlag
  mem.io.read.rdAddr := rPointer
  mem.io.write.wren := io.writeFlag
  mem.io.write.wrData := io.dataIn
  mem.io.write.wrAddr := wPointer
  val dataOut = Wire(UInt(width.W))
  dataOut := mem.io.read.rdData
  def indexAdd(index : UInt) : UInt = {
      Mux(index === (size - 1).U, 0.U, index + 1.U)
  }
  when(io.writeFlag === true.B && io.readFlag === true.B) {
    //when(count === 0.U) { dataOut := io.dataIn }
    //.otherwise {
      rPointer := indexAdd(rPointer)
      wPointer := indexAdd(wPointer)
    //} 
  }.elsewhen (io.writeFlag === true.B && io.readFlag === false.B) {
    when(count < size.U) {
      wPointer := indexAdd(wPointer)
      count := count + 1.U
    }
  } .elsewhen (io.writeFlag === false.B && io.readFlag === true.B) {
    when(count > 0.U) {
      rPointer := indexAdd(rPointer)
      count := count - 1.U
    }
  }
  when(io.flush){
    rPointer := 0.U 
    wPointer := 0.U
    count := 0.U 
  }
  io.dataOut := dataOut
  io.full := (size.U === count)
  io.empty := (count === 0.U)
}
class fiforam(val size : Int, val width : Int) extends Module with Config{
  val io = IO(new Bundle {
        val fifo = new SyncFifoIO(width)
        val flush = Input(Bool())
  })
  val fifo = Module(new flushramfifo(size,width) )
  io.fifo.fiforead.dout := fifo.io.dataOut
  io.fifo.fifowrite.full := fifo.io.full 
  io.fifo.fiforead.empty:= fifo.io.empty
  fifo.io.dataIn := io.fifo.fifowrite.din 
  fifo.io.writeFlag := io.fifo.fifowrite.write
  fifo.io.readFlag := io.fifo.fiforead.read
  fifo.io.flush := io.flush
}
class fifoinst(val size : Int, val width : Int) extends Module with Config{
  val io = IO(new Bundle {
        val fifo = new SyncFifoIO(width)
  })
  val fifo = Module(new fifo(size,width) )
  io.fifo.fiforead.dout := fifo.io.dataOut
  io.fifo.fifowrite.full := fifo.io.full 
  io.fifo.fiforead.empty:= fifo.io.empty
  fifo.io.dataIn := io.fifo.fifowrite.din 
  fifo.io.writeFlag := io.fifo.fifowrite.write
  fifo.io.readFlag := io.fifo.fiforead.read
}
