package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import java.io._



class CrcBlackBox extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    val crcen = Input(Bool())
    val data = Input(UInt(8.W))
    val crc = Output(UInt(32.W))
  })
  override def desiredName = "crc"
}

class CrcModel extends Module with Config{
  val io = IO(new Bundle {
    val crcen = Input(Bool())
    val data = Input(UInt(8.W))
    val rst = Input(Bool())
    val crc = Output(UInt(32.W))
  })
  val crc = Module(new CrcBlackBox)
  crc.io.clk := clock
  crc.io.rst := reset.asBool || io.rst
  crc.io.crcen := io.crcen
  crc.io.data := io.data
  io.crc := crc.io.crc
}

class ramModel(addrwidth:Int, datawidth : Int) extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
    val write = Flipped(new RamWrite(addrwidth,datawidth))
  })
  override def desiredName = s"ramModel_${addrwidth}_${datawidth}"
}
class SramModel(addrwidth:Int, datawidth : Int, Type : Int = 0) extends Module with Config{
  val io = IO(new Bundle {
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
    //val write = Flipped(new RamWrite(addrwidth,datawidth))
  })
  // 创建同步SRAM，参数为存储单元的数量和数据宽度
  if(Type == 0){
    // 提取非-1的元素作为ROM的数据
    val rom = SyncReadMem(MaxfifoNum, UInt(datawidth.W))
    //for (i <- 0 until nonNegativeCount) {
    //  rom.write(i.U, romData(i))
    //}
    io.read.rdData := ( (rom.read(io.read.rdAddr)))
   
  }
}
class DualSramModel(addrwidth:Int, datawidth : Int) extends Module with Config{
  val io = IO(new Bundle {
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
    val write = Flipped(new RamWrite(addrwidth,datawidth))
  })
  // 创建同步SRAM，参数为存储单元的数量和数据宽度
  val num = scala.math.pow(2,addrwidth).toInt - 1
  val sram = SyncReadMem(num, UInt(datawidth.W))

  // 写入数据
  when(io.write.wren) {
    sram.write(io.write.wrAddr, io.write.wrData)
  }
  // 读取数据
  io.read.rdData := sram.read(io.read.rdAddr)
}

class ramblackbox(addrwidth:Int, datawidth : Int) extends Module with Config{
  val io = IO(new Bundle {
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
    val write = Flipped(new RamWrite(addrwidth,datawidth))
  })

    val ram = Module(new DualSramModel(addrwidth,datawidth))
    io.read <> ram.io.read
    io.write <> ram.io.write
  
}

class romModel(addrwidth:Int, datawidth : Int) extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
  })
  override def desiredName = s"romModel_${addrwidth}_${datawidth}"
}
class romblackbox(addrwidth:Int, datawidth : Int, Type : Int = 0) extends Module with Config{
  val io = IO(new Bundle {
    val read  = Flipped(new RamRead(addrwidth,datawidth) )
  })
    val rom = Module(new SramModel(addrwidth,datawidth,Type))
    io.read <> rom.io.read
}
  

