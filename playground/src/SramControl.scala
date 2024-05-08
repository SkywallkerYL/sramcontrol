package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._


/*****
Sram控制核 ，将Axi接口转换为Sram的读写接口 处理外部的读写请求
******/
class SramControlCore extends Module with Config {
  val io = IO(new Bundle{
    //读数据通道
    val SramRd = Flipped(new AxiRead)
    //写数据通道
    val SramWr = Flipped(new AxiWrite)
    //给Ram的读端口
    val RamRd = (new RamRead(SramSizeWidth-1,DataWidth))
    val ReadId = Output(UInt((SramIdwidth+1).W))
    //给Ram的写端口
    val RamWr = (new RamWrite(SramSizeWidth-1,DataWidth))
    val WriteId = Output(UInt((SramIdwidth+1).W))
  })
  //这只是一个类似Axi的接口，没有完全遵守Axi协议
  //一些默认的输出
  io.SramRd.arready := false.B
  io.SramRd.rvalid := false.B
  io.SramRd.rdata := 0.U
  io.SramRd.rlast := false.B

  io.SramWr.awready := false.B
  io.SramWr.wready := false.B

  io.RamRd.rden := false.B
  io.RamRd.rdAddr := io.SramRd.araddr(SramSizeWidth-2,0)

  io.RamWr.wren := false.B
  io.RamWr.wrAddr := io.SramWr.awaddr(SramSizeWidth-2,0)
  io.RamWr.wrData := io.SramWr.wdata

  io.ReadId := Sramnum.U //io.SramRd.araddr(AddrWidth-1,AddrWidth-SramIdwidth)
  io.WriteId := Sramnum.U //io.SramWr.awaddr(AddrWidth-1,AddrWidth-SramIdwidth)

  //状态机控制 读状态分配
  val sRIdle :: sRead :: Nil = Enum(2)
  val rdstate = RegInit(sRIdle)
  //读长度计数器 
  val rdlen = RegInit(0.U(AXILENWIDTH.W))
  val rdcount = RegInit(0.U(AXILENWIDTH.W))
  //锁存首地址
  val rdaddr = RegInit(0.U((SramSizeWidth-1).W))
  switch(rdstate){
    is(sRIdle){
      io.SramRd.arready := true.B
      when(io.SramRd.arvalid){
        //提前一个周期读数据 
        io.RamRd.rden := true.B
        rdaddr := io.SramRd.araddr(SramSizeWidth-2,0)
        rdlen := io.SramRd.arlen
        rdcount := 0.U
        rdstate := sRead
      }
    }
    is(sRead){
      io.ReadId := io.SramRd.araddr(AddrWidth-1,AddrWidth-SramIdwidth)
      io.SramRd.rvalid := true.B
      io.SramRd.rdata := io.RamRd.rdData 
      io.RamRd.rden := true.B
      io.SramRd.rlast := (rdcount === rdlen)
      when(io.SramRd.rready){
        rdcount := rdcount + 1.U
        //提前读下一个地址的数据
        io.RamRd.rdAddr := rdaddr + rdcount + 1.U     
        when(io.SramRd.rlast){
          rdstate := sRIdle
          io.RamRd.rden := false.B
        }
      }.otherwise{
        io.RamRd.rdAddr := rdaddr + rdcount
      }
    }
  }

  //写状态分配 和写状态机
  //写数据的时候不知道要写多少，所以是外部发一个last信号
  val sWIdle :: sWrite :: Nil = Enum(2)
  val wrstate = RegInit(sWIdle)
  val wraddr = RegInit(0.U((SramSizeWidth-1).W))
  val wrcount = RegInit(0.U(AXILENWIDTH.W))
  switch(wrstate){
    is(sWIdle){
      io.SramWr.awready := true.B
      when(io.SramWr.awvalid){
        wraddr := io.SramWr.awaddr(SramSizeWidth-2,0)
        wrcount := 0.U
        wrstate := sWrite
      }
    }
    is(sWrite){
      io.SramWr.wready := true.B
      io.WriteId := io.SramWr.awaddr(AddrWidth-1,AddrWidth-SramIdwidth)
      io.RamWr.wrData := io.SramWr.wdata
      io.RamWr.wrAddr := wraddr + wrcount
      when(io.SramWr.wvalid){
        wrcount := wrcount + 1.U
        io.RamWr.wren := true.B
        when(io.SramWr.wlast){
          wrstate := sWIdle
        }
      }
    }
  }
}

/*********
Sram控制模块 
Sram也在这里面例化
功能：
1.16个通道会向Sram控制模块发送请求，对Sram进行读写，读写的数据是Sram的ID
识别某个通道的请求，然后根据请求的ID，将数据发送到对应的Sram
或者从对应的Sram读出数据

因为外部的管理模块，所以Id不会冲突，
一个策略是，每个Sram的读写端口根据ID来区分，当Id与该Sram相匹配，则把数据的读写使能，和Sram内部的地址
转发给当前Sram。
2.

*********/
class SramControl extends Module with Config {
  val io = IO(new Bundle{
    //16个读数据通道
    val SramRd = MixedVec(Seq.fill(portnum)(Flipped(new AxiRead)))
    //16写数据通道
    val SramWr = MixedVec(Seq.fill(portnum)(Flipped(new AxiWrite)))
  })
  //例化Sram控制核
  val sramcontrolcore = VecInit(Seq.fill(portnum)(Module(new SramControlCore)).map(_.io))
  //Sram控制核的读写端口 和一些默认的输出
  for(i <- 0 until portnum){
    sramcontrolcore(i).SramRd <> io.SramRd(i)
    sramcontrolcore(i).SramWr <> io.SramWr(i)
    sramcontrolcore(i).RamRd.rdData := 0.U 
    sramcontrolcore(i).RamRd.rdValid := false.B
    sramcontrolcore(i).RamWr.wrValid := false.B
  }
  //Read 端口默认输出 
  //例化Sram
  val SramGroup = VecInit(Seq.fill(Sramnum)(Module(new ramblackbox(SramSizeWidth-1, DataWidth,OneSramSize))).map(_.io))
  //一些默认的输出
  for(i <- 0 until Sramnum){
    SramGroup(i).read.rden := false.B
    SramGroup(i).read.rdAddr := 0.U 
    SramGroup(i).write.wren := false.B
    SramGroup(i).write.wrAddr := 0.U
    SramGroup(i).write.wrData := 0.U
  }
  //Sram控制核的读写端口  
  for(i <- 0 until portnum){
    for(j <- 0 until Sramnum){
      //这里直接这么写， 如果默认初始都是0,就会出问题，要在core里面改一下默认值
      when(sramcontrolcore(i).ReadId === j.U ){
        SramGroup(j).read <> sramcontrolcore(i).RamRd
      }
      when(sramcontrolcore(i).WriteId === j.U ){
        SramGroup(j).write <> sramcontrolcore(i).RamWr
      }
    }
  }

}