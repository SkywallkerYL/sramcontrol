package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/*******
写入队列控制模块 
对数据输入进行处理，将对应的数据存入该通道对应的fifo中。

*******/
class WriteInCore extends Module with Config {
  val io = IO(new Bundle{
    val Wr = Flipped(new ChannelOut(DataWidth))
    val datafiforead = (new ReaderIO(DataWidth))
    val lenfiforead = (new ReaderIO(lenwidth))
  })
    //一些默认的输出
    io.Wr.ready := false.B

    //当前通道的数据fifo 
    val datafifo = Module(new fiforam(MaxfifoNum,DataWidth))
    //当前通道的长度fifo
    val lenfifo = Module(new fiforam(MaxfifoNum,lenwidth))
    io.datafiforead <> datafifo.io.fifo.fiforead
    io.lenfiforead <> lenfifo.io.fifo.fiforead

    datafifo.io.fifo.fifowrite.write := false.B
    datafifo.io.flush := false.B 
    lenfifo.io.fifo.fifowrite.write := false.B
    lenfifo.io.flush := false.B
    datafifo.io.fifo.fifowrite.din := io.Wr.data
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
}

class WriteInProcess extends Module with Config {
  val io = IO(new Bundle{
    //16个端口的写入
    val Wr =  MixedVec(Seq.fill(portnum)(Flipped(new ChannelOut(DataWidth))))  
    //Flipped(new ChannelOut(DataWidth))
    //16个端口的fifo读 输出给ArbiterBridge
    val datafiforead = MixedVec(Seq.fill(portnum)(new ReaderIO(DataWidth)))
    val lenfiforead = MixedVec(Seq.fill(portnum)(new ReaderIO(lenwidth)))
  })
    val writeincore = VecInit(Seq.fill(portnum)(Module(new WriteInCore)).map(_.io))
    for(i <- 0 until portnum){
      writeincore(i).Wr <> io.Wr(i)
      io.datafiforead(i) <> writeincore(i).datafiforead
      io.lenfiforead(i) <> writeincore(i).lenfiforead
    }
}