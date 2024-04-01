package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/******
读出队列控制模块
功能：
1.当外部有读出信号时 根据数据优先级往外部送数据
2.从SG模块获取Data进行ECC的处理
3.从SG模块获取地址与调度模块通信
4.向调度模块发送读请求以及读地址
5.从调度模块获取要读出的数据
//内部模块架构划分

8个优先级对应的ECC存储fifo
8个记录数据包长度的fifo
数据流进来时根据data做ECC 存入ECC的fifo 
然后做一个count指示当前数据包的包长度  
收到包尾信号的时候结束计数，然后把count存入包长度fifo


然后读出数据时，根据ECC fifo的空情况，从优先级由高到底选择对应的fifo进行数据
先给SG模块发送读出数据的请求ready拉高。然后获得地址和长度
然后向仲裁模块申请读，发送地址和长度

拿到数据后从对应的ECC里面读出对应的校验做校验，校验失败直接不发，校验成功就往外送






*******/
class ReadQueueControl extends Module with Config {
  val io = IO(new Bundle{
    val Rd = new ChannelOut(DataWidth)
    val SgData = new DataChannel(DataWidth)
    val SgAddr = new AddrChannel(AddrWidth)
    val ArbiterData = Flipped(new DataChannel(DataWidth))
    val ArbiterAddr = Flipped(new AddrChannel(AddrWidth))
  })




}
/******
实现一个CRC校验模块，用于对数据进行校验
根据数据的优先级存入对应的fifo
******/


class EccCalculate extends Module with Config {
  val io = IO(new Bundle{
    val Rd = new ChannelOut(DataWidth)
    val SgData = new DataChannel(DataWidth)
    val SgAddr = new AddrChannel(AddrWidth)
    val ArbiterData = Flipped(new DataChannel(DataWidth))
    val ArbiterAddr = Flipped(new AddrChannel(AddrWidth))
  })
  //根据输入的数据进行ECC的校验
  //实例化8个暂存Data的Fifo
  val dataFifo = Seq.fill(8)(Module(new fiforam(addrwidth,BLKSIZE)))
  //SgData的数据有效时，将数据写入对应的Fifo
  for(i <- 0 until 8){
    when(io.SgData.valid && io.SgData.prior === i.U){
      dataFifo(i).io.dataIn := io.SgData.data
      dataFifo(i).io.writeFlag := true.B
    }
  }
  
  //首先实例化8个优先级对应的ECC的Fifo
  val eccFifo = Seq.fill(8)(Module(new fiforam(addrwidth,BLKSIZE)))
  //然后实例化8个记录数据包长度的fifo
  val lenFifo = Seq.fill(8)(Module(new fiforam(addrwidth,BLKSIZE)))

  //数据读入部分的控制状态机 
  val sIdle :: sData :: sEcc :: sLen :: Nil = Enum(4)
  //初始化状态机
  val rdstate = RegInit(sIdle)

  switch(rdstate){
    is(sIdle){
      //当SG模块数据Valid拉高时
      when(io.SgData.valid){
        rdstate := sData
      }
    }
    is(sData){
      when(io.SgData.eop){
        rdstate := sEcc
      }
    }
    is(sEcc){
      when(io.SgData.eop){
        rdstate := sLen
      }
    }
    is(sLen){
      when(io.SgData.eop){
        rdstate := sIdle
      }
    }
  }
}