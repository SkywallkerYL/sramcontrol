package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._
/*********
状态机处理数据的输入，当数据输入时，根据数据的sop和eop标志位，判断数据的开始和结束

根据datavalid拉高时的数据，提取数据的优先级，根据优先级将数据写入对应的fifo中

*********/
class DataInProcess extends Module with Config {
  val io = IO(new Bundle{
    val Wr = Flipped(new ChannelOut(DataWidth))
    val fifowrite = Vec(priornum,Flipped(new WriterIO(DataWidth)))
		val lenfifowrite = Vec(priornum,Flipped(new WriterIO(lenwidth)))
		val update = Output(Bool())
    val prior = Output(UInt(priorwidth.W))
    val DataLen = Output(UInt(lenwidth.W))
  })
  //一些默认的输出
  io.Wr.ready := false.B
	io.update := false.B
	io.fifowrite.foreach(_.write := false.B)
	io.lenfifowrite.foreach(_.write := false.B)
	io.fifowrite.foreach(_.din := 0.U)
	io.lenfifowrite.foreach(_.din := 0.U)
  //记录选择的优先级
  val prior = RegInit(0.U(priorwidth.W))      
  //数据的优先级
  val priority = io.Wr.data(6,4)
  
  //对应fifo的写full信号，当fifo满时，不接受数据
  val fifo_full = Wire(Vec(priornum,Bool()))  
  for(i <- 0 until priornum){
    fifo_full(i) := io.fifowrite(i).full
  }
  //记录数据的一个DataLen 
  val DataLen = RegInit(0.U(lenwidth.W))
  //真实的DataLen为DataLen+1
  io.DataLen := DataLen
  io.prior := prior
  
  //主状态机
  val sIdle :: sData :: sUpdate :: Nil = Enum(3)
  val state = RegInit(sIdle)
  switch(state){
  	is(sIdle){
      //数据有效时，根据数据的sop标志位，判断数据的开始
      io.Wr.ready := !fifo_full(priority)
      when(io.Wr.valid && io.Wr.sop && io.Wr.ready){
        state := sData
        prior := priority
	      DataLen := 0.U
        io.fifowrite(prior).write := true.B
        io.fifowrite(prior).din := io.Wr.data
      }
    }
    is(sData){
      io.Wr.ready := !fifo_full(prior)
      when(io.Wr.valid && io.Wr.ready){
        io.fifowrite(prior).write := true.B
        io.fifowrite(prior).din := io.Wr.data
				DataLen := DataLen + 1.U
			}
      when(io.Wr.valid && io.Wr.ready && io.Wr.eop){
        state := sUpdate
      }
    }
		//向外部输出DataLen和prior
		is(sUpdate){
			io.Wr.ready := false.B
			io.update := true.B
			when(!io.lenfifowrite(prior).full){
				io.lenfifowrite(prior).write := true.B
				io.lenfifowrite(prior).din := DataLen
				state := sIdle
			}
		}
  }
}