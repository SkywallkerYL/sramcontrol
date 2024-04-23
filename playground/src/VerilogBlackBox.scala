package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import java.io._

/***************
ArbiterBridge 模块
功能：
通过仲裁模块，根据输入端口的数据转发到对应的目的端口

首先从Fifo里读出数据，根据该数据的目的端口，将其转发到对应的数据分散模块
注意支持16个端口同时读写，
相当于一个多

****************/

class ArbiterBridge extends BlackBox with Config {
  val io = IO(new Bundle {
    //16个端口的数据读
    val fifoRead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(DataWidth))))
    //16个端口的数据长度读
    val LenfifoRead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(lenwidth))))
    //转发给对应的数据分散模块
    val Data2Scater = MixedVec(Seq.fill(portnum)(Flipped(new DataChannel(DataWidth))))
  })
  //override def desiredName = "crc"
}

class ArbiterBridgeModel extends Module with Config{
  val io = IO(new Bundle {
    val fifoRead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(DataWidth))))
    val LenfifoRead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(lenwidth))))
    val Data2Scater = MixedVec(Seq.fill(portnum)(Flipped(new DataChannel(DataWidth))))
  })
    val arbiter = Module(new ArbiterBridge)
    io <> arbiter.io
    //for(i <- 0 until portnum){
    //  io.fifoRead(i).read := arbiter.io.fifoRead(i).read
    //  arbiter.io.fifoRead(i).dout := io.fifoRead(i).dout
    //  arbiter.io.fifoRead(i).empty := io.fifoRead(i).empty
//
    //  io.LenfifoRead(i).read := arbiter.io.LenfifoRead(i).read
    //  arbiter.io.LenfifoRead(i).dout := io.LenfifoRead(i).dout
    //  arbiter.io.LenfifoRead(i).empty := io.LenfifoRead(i).empty
//
    //  io.Data2Scater(i).valid :=  arbiter.io.Data2Scater(i).valid
    //  io.Data2Scater(i).data := arbiter.io.Data2Scater(i).data
    //  io.Data2Scater(i).sop := arbiter.io.Data2Scater(i).sop
    //  io.Data2Scater(i).eop := arbiter.io.Data2Scater(i).eop
    //  io.Data2Scater(i).prior := arbiter.io.Data2Scater(i).prior
    //  arbiter.io.Data2Scater(i).ready := io.Data2Scater(i).ready 
    //}
}
/*******************
数据输入处理模块
功能：
状态机处理数据的输入，当数据输入时，根据数据的sop和eop标志位，判断数据的开始和结束
将数据写入fifo，当前数据包的长度写入长度fifo中
*******************/
class DataInControl extends BlackBox with Config {
  val io = IO(new Bundle{
    //16个端口的写入
    val Wr = MixedVec(Seq.fill(portnum)(Flipped(new ChannelOut(DataWidth)))) 
    //16个端口的fifo写
    //val fifowrite = MixedVec(Seq.fill(portnum)(Flipped(new WriterIO(DataWidth))))
    //16个端口的长度fifo写
    //val lenfifowrite = MixedVec(Seq.fill(portnum)(Flipped(new WriterIO(lenwidth))))
    //16个端口的fifo读 输出给ArbiterBridge
    val fifoRead = MixedVec(Seq.fill(portnum)((new ReaderIO(DataWidth))))
    //16个端口的长度fifo读 输出给ArbiterBridge
    val LenfifoRead = MixedVec(Seq.fill(portnum)((new ReaderIO(lenwidth))))

  })
}
class DataInControlModel extends Module with Config {
  val io = IO(new Bundle{
    //16个端口的写入
    val Wr = MixedVec(Seq.fill(portnum)(Flipped(new ChannelOut(DataWidth)))) 
    //16个端口的fifo写
    //val fifowrite = MixedVec(Seq.fill(portnum)(Flipped(new WriterIO(DataWidth))))
    //16个端口的长度fifo写
    //val lenfifowrite = MixedVec(Seq.fill(portnum)(Flipped(new WriterIO(lenwidth))))
    //16个端口的fifo读 输出给ArbiterBridge
    val fifoRead = MixedVec(Seq.fill(portnum)((new ReaderIO(DataWidth))))
    //16个端口的长度fifo读 输出给ArbiterBridge
    val LenfifoRead = MixedVec(Seq.fill(portnum)((new ReaderIO(lenwidth))))

  })
  val dataincontrol = Module(new DataInControl)
  io <> dataincontrol.io
}
/*******************
MMU 内存管理控制模块
功能：
1.接受ScatterCollecter发送的写数据请求，返回当前拆包后数据写入的首地址，
该模块接受ScatterCollecter传入的数据，当能写入的地址不连续时，返回当前数据包写入的首地址，
继续接受数据，直到数据包写入完成，每一包数据都返回一个地址，
2.接受ScatterCollecter发送的读数据请求，根据传入的数据首地址，和数据包的长度，
读取数据，返回给ScatterCollecter
3.将数据写和读的信息转发给SramControl模块。

该模块内部管理内存的分配和释放

********************/

class MMU extends BlackBox with Config {
  val io = IO(new Bundle{
    //ScatterCollecter写数据请求
    val WrData = Flipped(new AxiStream(DataWidth))
    //ScatterCollecter写数据返回地址通道
    val WrAddr = new AxiStream(AddrWidth)
    //ScatterCollecter读数据请求数据通道
    val RdData = (new AxiStream(DataWidth))
    //ScatterCollecter读数据请求地址通道
    val RdAddr = Flipped(new AxiStream(AddrWidth))
    //SramControl写数据数据通道
    val SramWrData = (new AxiStream(DataWidth))
    //SramControl写数据地址通道
    val SramWrAddr = (new AxiStream(AddrWidth))
    //SramControl读数据数据通道
    val SramRdData = Flipped(new AxiStream(DataWidth))
    //SramControl读数据地址通道
    val SramRdAddr = (new AxiStream(AddrWidth))
  })
}

class MMUModel extends Module with Config {
  val io = IO(new Bundle{
    val WrData = Flipped(new AxiStream(DataWidth))
    val WrAddr = new AxiStream(AddrWidth)
    val RdData = (new AxiStream(DataWidth))
    val RdAddr = Flipped(new AxiStream(AddrWidth))
    val SramWrData = (new AxiStream(DataWidth))
    val SramWrAddr = (new AxiStream(AddrWidth))
    val SramRdData = Flipped(new AxiStream(DataWidth))
    val SramRdAddr = (new AxiStream(AddrWidth))
  })
  val mmu = Module(new MMU)
  io <> mmu.io
}

/*******************
SramControl 模块
功能：
1.接受MMU发送的写数据请求，根据地址写入数据
2.接受MMU发送的读数据请求，根据地址读取数据
3.将数据写和读的信息转发给MMU模块。
4.控制内部的Sram存储器
********************/
class SramControl extends BlackBox with Config {
  val io = IO(new Bundle{
    //MMU写数据数据通道
    val SramWrData = Flipped(new AxiStream(DataWidth))
    //MMU写数据地址通道
    val SramWrAddr = Flipped(new AxiStream(AddrWidth))
    //MMU读数据数据通道
    val SramRdData = (new AxiStream(DataWidth))
    //MMU读数据地址通道
    val SramRdAddr = Flipped(new AxiStream(AddrWidth))
  })
}

class SramControlModel extends Module with Config {
  val io = IO(new Bundle{
    val SramWrData = Flipped(new AxiStream(DataWidth))
    val SramWrAddr = Flipped(new AxiStream(AddrWidth))
    val SramRdData = (new AxiStream(DataWidth))
    val SramRdAddr = Flipped(new AxiStream(AddrWidth))
  })
  val sramcontrol = Module(new SramControl)
  io <> sramcontrol.io
}