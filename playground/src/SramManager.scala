package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/*********
Sram管理模块 

功能：
16个通道会向Sram管理模块发送请求，请求分配Sram

用一个32位的寄存器来统计每一块Sram是否被分配，如果被分配了，

则对应的Sram编号的寄存器位数为0，否则为1

比如5个寄存器 00000
234被分配了 00011
然后就 取个反码+1 11101 两个与起来 00001 说明最小的未被分配的Sram编号是0 

这样可以快速定位到一个未被分配的Sram编号

规定一次只能处理一个分配请求，
16个端口都发送请求，但是只有一个会被处理，其他的会被忽略，即ready不会拉高。
端口0优先级最高。 传的数据是Sram的Id  0-31
*********/
class SramManager extends Module with Config {
  val io = IO(new Bundle{
    //16个 Sram 请求分配通道
    val SramReq = MixedVec(Seq.fill(portnum)((new AxiStream(SramIdwidth))))
    //Sram 释放通道
    val SramRelease = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(SramIdwidth))))
  })
    //一些默认的输出
    io.SramReq.foreach(_.valid := false.B)
    io.SramReq.foreach(_.data := 0.U)
    io.SramReq.foreach(_.last := false.B)

    io.SramRelease.foreach(_.ready := false.B)

    //寄存器编号统计 初始为全1 位宽为Sramnum
    val SramReg = RegInit((1.U << Sramnum) - 1.U)
    //最小空闲寄存器
    val minSram = SramReg & (~SramReg + 1.U)
    //根据minSram的值，计算ID  
    val minSramId = Wire(UInt(SramIdwidth.W))
    minSramId := 0.U 
    for(i <- 0 until Sramnum){
      when(minSram(i)){
        minSramId := i.U
      }
    }
    //处理请求
    //记录选择的通道 
    val Reqselect = RegInit(0.U(portwidth.W))
    print(portnum,portwidth)
    //记录当前周期的选择
    val ReqlocalSelect = Wire(UInt(portwidth.W))
    ReqlocalSelect := 0.U 
    for(i <- 0 until portnum){
      when(io.SramReq(i).valid){
        ReqlocalSelect := i.U
      }
    }
    //请求分配状态机  和状态分配 优先处理 释放请求
    val sIdle :: sAlloc ::sRelease :: Nil = Enum(3)
    val stateReq = RegInit(sIdle)
    switch(stateReq){
      is(sIdle){
        for(i <- 0 until portnum){
            when(io.SramRelease(i).valid){
                Reqselect := i.U
                stateReq := sRelease
            }.elsewhen(io.SramReq(i).ready){
                Reqselect := i.U
                stateReq := sAlloc
            }
        }
      }
      is(sAlloc){
        //这里有个bug, 没有判空,即所有Sram都写满了
        when(SramReg =/= 0.U){
          for(i <- 0 until portnum){
            when(Reqselect === i.U){
              io.SramReq(i).valid := true.B
              io.SramReq(i).data := minSramId
              when(io.SramReq(i).ready){
                SramReg := SramReg & (~minSram)
                stateReq := sIdle
              }
            }
          }
        }.otherwise{
          stateReq := sIdle
        }
        

      }
      is(sRelease){
        for(i <- 0 until portnum){
          when(Reqselect === i.U){
            io.SramRelease(i).ready := true.B
            when(io.SramRelease(i).valid){
              SramReg := SramReg | (1.U << io.SramRelease(i).data)
              stateReq := sIdle
            }
          }
        }
      }
    }
}