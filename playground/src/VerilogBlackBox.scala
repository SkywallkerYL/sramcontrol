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
class SramControlV extends BlackBox with Config {
  val io = IO(new Bundle{
    //SramControl写数据数据通道
    val SramWr  = Flipped(new AxiWrite)
    //SramControl读数据数据通道
    val SramRd  = Flipped(new AxiRead)
  })
}

class SramControlModel extends Module with Config {
  val io = IO(new Bundle{
    //SramControl写数据数据通道
    val SramWr  = Flipped(new AxiWrite)
    //SramControl读数据数据通道
    val SramRd  = Flipped(new AxiRead)
  })
  val sramcontrol = Module(new SramControlV)
  io <> sramcontrol.io
}

/**
//Sram 分配模块
//实现一个Sram分配模块 最小分配单元是一块Sram，即1KB 

当某个通道有写入请求时，首先向该模块发送一个请求分配。然后该模块将空闲的Sram分配给该通道

当某个通道的数据全部读出后，向该模块发送一个释放请求，将该Sram释放。
*///
class SramManagerV extends BlackBox with Config {
  val io = IO(new Bundle{
    //Sram 请求分配通道
    val SramReq = MixedVec(Seq.fill(portnum)(new AxiStream(SramIdwidth)))
    //Sram 释放通道
    val SramRelease = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(SramIdwidth))))
  })
}

class SramManagerModel extends Module with Config {
  val io = IO(new Bundle{
    //Sram 请求分配通道
    val SramReq = MixedVec(Seq.fill(portnum)(new AxiStream(SramIdwidth)))
    //Sram 释放通道
    val SramRelease = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(SramIdwidth))))
  })
  val srammanager = Module(new SramManagerV)
  io <> srammanager.io
}

//空闲地址管理模块

class FreeAddrManagerV extends BlackBox with Config {
  val io = IO(new Bundle{
    //Sram 请求分配通道
    val SramReq = Flipped(new AxiStream(SramIdwidth))
    //Sram 释放通道
    val SramRelease = new AxiStream(SramIdwidth)
    //空闲地址输出
    val FreeAddr = new AxiStream(AddrWidth)
    //当前最大能写的长度 其实也可以不要这个。
    val MaxLen = Output(UInt(AddrWidth.W))

    //读地址通道，根据这个对地址进行释放
    val RdAddr = Flipped(new AxiStream(AddrWidth))
    //写地址通道，根据这个对地址进行分配
    val WrAddr = Flipped(new AxiStream(AddrWidth))


  })
}

//优先级选择模块

class PrioritySelect extends BlackBox with Config {
  val io = IO(new Bundle{
    //8个fifo的empty作为输入，表明该fifo是否为空 ，可以选择一个读出
    val FifoEmpty = MixedVec(Seq.fill(priornum)(Input(Bool())))  
    //输出一个选择的优先级
    val Prior = Output(UInt(priorwidth.W))
  })
}
class PrioritySelectModel extends Module with Config {
  val io = IO(new Bundle{
    //8个fifo的empty作为输入，表明该fifo是否为空 ，可以选择一个读出
    val FifoEmpty = MixedVec(Seq.fill(priornum)(Input(Bool()))) 
    //输出一个选择的优先级
    val Prior = Output(UInt(priorwidth.W))
  })
  val priorityselect = Module(new PrioritySelect)
  io <> priorityselect.io
}