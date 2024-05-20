package FFT

import scala.math._
import chisel3._
import chisel3.util._
trait Config {

  val DataWidthIn  = 32
  //val FloatWidth = DataWidth/2


  

  val USEIP = false//是否使用ROM IP
//config of fixedpoint data format
  val Nmax = 12
  val Mmax = 32
  
  val MemReadModule = false 
  val Nwidth = log2Ceil(Nmax)
  val Mwidth = log2Ceil(Mmax)
  val AXIDATAWIDTH = 8 
  val AXISTRBWIDTH = 8
  //val AXIADDRWIDTH = 10+5
  val AXILENWIDTH  = 8
  val AXISIZEWIDTH = 3
  val AXIBURSTWIDTH= 2 
  val AXIIDWIDTH   = 4
  val AXIRESPWIDTH = 2
  val DataWidth = 8
  //val AddrWidth = 10+5//1KB * 32 

  val MaxfifoNum = 1024

  val maxlenNum = 1024
  val lenwidth = log2Ceil(maxlenNum)

  val priornum = 8
  val priorwidth = log2Ceil(priornum)

  val portnum = 16
  val portwidth = log2Ceil(portnum)

  val Sramnum = 32 
  val OneSramSize = 1024 * 256
  val SramSizeWidth = log2Ceil(OneSramSize)+1
  val readSramWidth = log2Ceil(OneSramSize)
  val SramIdwidth = log2Ceil(Sramnum)

  val AXIADDRWIDTH = readSramWidth + SramIdwidth
  val AddrWidth = AXIADDRWIDTH
  val ReleaseTimer = 1000
  val ReleaseTimerWidth = log2Ceil(ReleaseTimer-1)
  //crc的最大数据位数
  val maxcrcnum = 60
}
object Config extends Config {}
