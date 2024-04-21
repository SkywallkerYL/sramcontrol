package FFT

import scala.math._
import chisel3._
import chisel3.util._
trait Config {

  val DataWidthIn  = 32
  //val FloatWidth = DataWidth/2


  

  val use_ip = true//是否使用Xilinx的浮点运算IP
//config of fixedpoint data format
  val Nmax = 12
  val Mmax = 32
  
  val MemReadModule = false 
  val Nwidth = log2Ceil(Nmax)
  val Mwidth = log2Ceil(Mmax)
  val AXIDATAWIDTH = 256 
  val AXISTRBWIDTH = 8
  val AXIADDRWIDTH = 36
  val AXILENWIDTH  = 8
  val AXISIZEWIDTH = 3
  val AXIBURSTWIDTH= 2 
  val AXIIDWIDTH   = 4
  val AXIRESPWIDTH = 2
  val DataWidth = 8
  val AddrWidth = 32 

  val MaxfifoNum = 200

  val maxlenNum = 1024
  val lenwidth = log2Ceil(maxlenNum)

  val priornum = 8
  val priorwidth = log2Ceil(priornum-1)
  //crc的最大数据位数
  val maxcrcnum = 60
}
object Config extends Config {}
