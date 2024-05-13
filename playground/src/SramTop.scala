package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._


class SramTop extends Module with Config {
  val io = IO(new Bundle{
    //16个端口的写入
    val Wr =  MixedVec(Seq.fill(portnum)(Flipped(new ChannelOut(DataWidth))))  
    //16个端口的读出
    val Rd = MixedVec(Seq.fill(portnum)(new ChannelOut(DataWidth)))
  })
  //一个写入处理模块
  val writein = Module(new WriteInProcess)
  //与16个写入端口相连
  for(i <- 0 until portnum){
    io.Wr(i)  <> writein.io.Wr(i)  
  }
  //16个数据分散汇集模块
  val dataScatercollector = VecInit(Seq.fill(portnum)(Module(new DataScater)).map(_.io))
  //与输出端口相连
  for(i <- 0 until portnum){
    dataScatercollector(i).Rd <> io.Rd(i)
  }
  //转接桥模块
  val arbiterbridge = Module(new ArbiterBridge)
  for(i <- 0 until portnum){
    arbiterbridge.io.infiforead(i) <> writein.io.datafiforead(i)
    arbiterbridge.io.inlenfiforead(i) <> writein.io.lenfiforead(i)
    arbiterbridge.io.source2destportidIn(i) <> writein.io.destport(i)
    writein.io.finish(i) := arbiterbridge.io.infinish(i)

    dataScatercollector(i).Bridgefiforead <> arbiterbridge.io.destfiforead(i)
    dataScatercollector(i).Bridgelenfiforead <> arbiterbridge.io.destlenfiforead(i)
    arbiterbridge.io.dest2sourceportid(i) <> dataScatercollector(i).sourceport
    arbiterbridge.io.source2destportidOut(i) <> dataScatercollector(i).inport
    arbiterbridge.io.destfinish(i) := dataScatercollector(i).finish
  }

  //16个MMU 
  val mmu = VecInit(Seq.fill(portnum)(Module(new Mmu)).map(_.io))
  //与数据分散模块相连
  for(i <- 0 until portnum){
    //直接写会有问题，每个信号单独写
    //mmu(i).WrData.valid := dataScatercollector(i).WrData.valid
    //mmu(i).WrData.data  := dataScatercollector(i).WrData.data
    //mmu(i).WrData.sop   := dataScatercollector(i).WrData.sop
    //mmu(i).WrData.eop   := dataScatercollector(i).WrData.eop
    //mmu(i).WrData.prior := dataScatercollector(i).WrData.prior
    //dataScatercollector(i).WrData.ready := mmu(i).WrData.ready 
    //Flipped
    dataScatercollector(i).WrData <>  mmu(i).WrData 
    mmu(i).WrAddr <> dataScatercollector(i).WrAddr
    mmu(i).RdData <> dataScatercollector(i).RdData
    //mmu(i).RdAddr.valid := dataScatercollector(i).RdAddr.valid
    //mmu(i).RdAddr.addr := dataScatercollector(i).RdAddr.addr
    //mmu(i).RdAddr.prior := dataScatercollector(i).RdAddr.prior
    //mmu(i).RdAddr.length := dataScatercollector(i).RdAddr.length
    //dataScatercollector(i).RdAddr.ready := mmu(i).RdAddr.ready
    dataScatercollector(i).RdAddr <> mmu(i).RdAddr
  }
  //SramControl 模块
  val sramcontrol = Module(new SramControl)
  //与MMU相连
  for(i <- 0 until portnum){
    sramcontrol.io.SramRd(i) <> mmu(i).SramRd
    sramcontrol.io.SramWr(i) <> mmu(i).SramWr
  }
  //SramManager 模块
  val srammanager = Module(new SramManager)
  //与MMU相连
  for(i <- 0 until portnum){
    srammanager.io.SramReq(i) <> mmu(i).SramReq
    srammanager.io.SramRelease(i) <> mmu(i).SramRelease
  }
}