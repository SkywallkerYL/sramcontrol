package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/*******
写入队列控制模块 
对数据输入进行处理，将对应的数据存入该通道对应的fifo中。

添加一个目的端口握手机制,
表示当前数据要传送的目的端口和该端口握手成功,当第一个数据写入后,
会将该数据对应的目的端口存入fifo,

然后另外一个状态机来处理这个fifo中的数据,,
读出目的端口数据,向目的端口发送valid.

进入一个等待状态,目的端口发送ready握手成功后,开始等待目的端口
从该fifo中读取数据, 
然后当目的端口读取完毕后,发送一个结束信号,释放该端口,回到idle状态.
当目的端口fifo非空时,继续读取,等待下一次握手.
*******/
class WriteInCore extends Module with Config {
  val io = IO(new Bundle{
    val Wr = Flipped(new ChannelOut(DataWidth))
    val datafiforead = (new ReaderIO(DataWidth))
    val lenfiforead = (new ReaderIO(lenwidth))
    val destport = new AxiStream(portwidth)
    //val shakeportin = Input(UInt(portwidth.W)
    val finish = Input(Bool())
  })
    //一些默认的输出
    io.Wr.ready := false.B

    //当前通道的数据fifo 
    val datafifo = Module(new fiforam(MaxfifoNum,DataWidth))
    //当前通道的长度fifo
    val lenfifo = Module(new fiforam(MaxfifoNum,lenwidth))
    //当前数据包目的端口fifo
    val destportfifo = Module(new fiforam(MaxfifoNum,portwidth))
    io.datafiforead <> datafifo.io.fifo.fiforead
    datafifo.io.fifo.fiforead.read := false.B
    io.lenfiforead <> lenfifo.io.fifo.fiforead
    lenfifo.io.fifo.fiforead.read := false.B

    datafifo.io.fifo.fifowrite.write := false.B
    datafifo.io.flush := false.B 
    lenfifo.io.fifo.fifowrite.write := false.B
    lenfifo.io.flush := false.B
    datafifo.io.fifo.fifowrite.din := io.Wr.data

    destportfifo.io.fifo.fifowrite.write := false.B
    destportfifo.io.flush := false.B
    destportfifo.io.fifo.fifowrite.din := io.Wr.data(portwidth-1,0)
    destportfifo.io.fifo.fiforead.read := false.B
    io.destport.valid := false.B
    io.destport.data := destportfifo.io.fifo.fiforead.dout
    io.destport.last := false.B
    //数据写入状态机处理
    val sIdle :: sWrite :: Nil = Enum(2)
    val state = RegInit(sIdle)
    val datacount = RegInit(0.U(lenwidth.W))
    lenfifo.io.fifo.fifowrite.din := datacount
    switch(state){
      is(sIdle){
        io.Wr.ready := !datafifo.io.fifo.fifowrite.full
        when(io.Wr.valid && io.Wr.ready && io.Wr.sop){
          state := sWrite
          datafifo.io.fifo.fifowrite.write := true.B
          destportfifo.io.fifo.fifowrite.write := true.B
          //lenfifo.io.fifo.fifowrite.write := true.B
          datacount := 1.U
        }
      }
      is(sWrite){
        io.Wr.ready := !datafifo.io.fifo.fifowrite.full
        when(io.Wr.valid && io.Wr.ready ){
            datafifo.io.fifo.fifowrite.write := true.B
            datacount := datacount + 1.U
            when(io.Wr.eop){
                lenfifo.io.fifo.fifowrite.write := true.B
                state := sIdle
            }
        }
      }
    }
    //数据目的端口状态分配和状态机
    val sIdle1 :: sWait :: sSend :: Nil = Enum(3)
    val deststate = RegInit(sIdle1)
    switch(deststate){
      is(sIdle1){
        //当destportfifo 不为空时,读取数据
        when(!destportfifo.io.fifo.fiforead.empty){
          destportfifo.io.fifo.fiforead.read := true.B
          deststate := sWait
        }
      }
      is(sWait){
        io.destport.valid := true.B
        when(io.destport.ready ){
          //握手成功,进入等待数据读取状态 ,
          deststate := sSend
        }
      }
      is(sSend){
        //数据读完后,进入idle状态
        io.destport.valid := true.B
        datafifo.io.fifo.fiforead.read := io.datafiforead.read
        lenfifo.io.fifo.fiforead.read := io.lenfiforead.read
        when(io.finish){
          deststate := sIdle1
        }
      }
    }
}

class WriteInProcess extends Module with Config {
  val io = IO(new Bundle{
    //16个端口的写入
    val Wr =  MixedVec(Seq.fill(portnum)(Flipped(new ChannelOut(DataWidth))))  
    //Flipped(new ChannelOut(DataWidth))
    //16个端口的fifo读 输出给ArbiterBridge
    val datafiforead = MixedVec(Seq.fill(portnum)(new ReaderIO(DataWidth)))
    val lenfiforead = MixedVec(Seq.fill(portnum)(new ReaderIO(lenwidth)))
    val destport = MixedVec(Seq.fill(portnum)(new AxiStream(portwidth)))
    //val shakeportin = Input(UInt(portwidth.W)
    val finish = MixedVec(Seq.fill(portnum)(Input(Bool())))
  })
    val writeincore = VecInit(Seq.fill(portnum)(Module(new WriteInCore)).map(_.io))
    for(i <- 0 until portnum){
      writeincore(i).Wr <> io.Wr(i)
      io.datafiforead(i) <> writeincore(i).datafiforead
      io.lenfiforead(i) <> writeincore(i).lenfiforead
      io.destport(i) <> writeincore(i).destport
      writeincore(i).finish := io.finish(i)
    }
}