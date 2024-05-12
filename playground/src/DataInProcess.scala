package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._
/*********
状态机处理数据的输入，从Arbiter转接过来的fifo进行数据的读取，根据读取的
Datalen 和优先级，将数据写入对应的fifo中
//当对应fifo未满时，将数据写入fifo中


根据Datalen直接确定要写多少数据


*********/
class DataInProcess extends Module with Config {
  val io = IO(new Bundle{
    val fiforead = Flipped(new ReaderIO(DataWidth))
    val lenfiforead = Flipped(new ReaderIO(lenwidth))

    val fifowrite = MixedVec(Seq.fill(priornum)(Flipped(new WriterIO(DataWidth))))
	  val lenfifowrite = MixedVec(Seq.fill(priornum)(Flipped(new WriterIO(lenwidth))))
	  val update = Output(Bool())
    val prior = Output(UInt(priorwidth.W))
    val DataLen = Output(UInt(lenwidth.W))

    //处理数据包的端口握手
    val inport = Flipped(new AxiStream(portwidth))
    val sourceport = Output(UInt(portwidth.W))
    val finish = Output(Bool())
  })
    //一些默认的输出
    io.update := false.B
    io.fiforead.read := false.B
    io.lenfiforead.read := false.B
    val fifoempty = io.lenfiforead.empty
    io.fifowrite.foreach(_.write := false.B)
    io.lenfifowrite.foreach(_.write := false.B)
    io.fifowrite.foreach(_.din := io.fiforead.dout)
    io.lenfifowrite.foreach(_.din := io.lenfiforead.dout)

    io.inport.ready := false.B
    io.finish := false.B
    //记录选择的优先级
    val prior = RegInit(0.U(priorwidth.W))      
    //数据的优先级
    val priority = io.fiforead.dout(6,4)
    
    //对应fifo的写full信号，当fifo满时，不接受数据
    val fifo_full = Wire(Vec(priornum,Bool()))  
    for(i <- 0 until priornum){
      fifo_full(i) := io.fifowrite(i).full
    }
    //对应优先级fifo不满时，读取数据
    val ChoosePrior = Wire(UInt(priorwidth.W))
    ChoosePrior := priority
    val fifochoosefullprior = MuxLookup(ChoosePrior,false.B,
   (0 until priornum).map(i => i.U -> fifo_full(i)))
    

    //记录数据的一个DataLen 
    val DataLen = RegInit(0.U(lenwidth.W))
    //真实的DataLen为DataLen+1
    io.DataLen := DataLen
    io.lenfifowrite.foreach(_.din := DataLen)
    val lencount = RegInit(0.U(lenwidth.W))
    io.prior := prior
    //锁存当前的输入端口Id 
    val portid = RegInit(0.U(portwidth.W))
    io.sourceport := portid
    //主状态机
    val sIdle :: sPortShake :: sgetfirstData :: sWriteAll :: Nil = Enum(4)
    val state = RegInit(sIdle)
    switch(state){
    is(sIdle){
        //握手成功后，开始读取数据
        when(io.inport.valid){
          state := sPortShake
          portid := io.inport.data
          
        }
    }
    is(sPortShake){
        //当有数据包长度fifo不空时，读取数据包长度 和数据 ，提取优先级
        io.inport.ready := true.B
        when(!fifoempty){
          state := sgetfirstData
          prior := priority
          io.fiforead.read := true.B
          io.lenfiforead.read := true.B
        }
    }
    is(sgetfirstData){
        //使用的fifo模型都会保持数据，所以只需要读取一次
        //即fiforead的读取没有拉高时，仍然会继续输出上一次的数据
        //获取优先级和数据长度
        DataLen := io.lenfiforead.dout
        //去下一个态的时候，第一个数据已经写入了，所以lencount = 1
        lencount := 1.U
        prior := priority
        //对应优先级的fifo未满时，写入数据
        when(fifochoosefullprior === false.B){
            io.fifowrite.zipWithIndex.foreach { case (fifo, i) =>
              when(priority === i.U) {
                fifo.write := true.B
                //fifo.din := io.fiforead.dout
              }
            }
            //继续读取数据
            io.fiforead.read := true.B
            //保险起见，数据全部写完之后，再写数据长度。
            state := sWriteAll
        }
    }
    //写入该优先级的数据 
	  is(sWriteAll){
      ChoosePrior := prior
		  when(fifochoosefullprior === false.B){
        io.fifowrite.zipWithIndex.foreach { case (fifo, i) =>
          when(prior === i.U) {
            fifo.write := true.B
            //fifo.din := io.fiforead.dout
          }
        }
        //继续读取数据
        io.fiforead.read := true.B
        //更新数据长度
        lencount := lencount + 1.U
        when(lencount === DataLen){
            io.finish := true.B
            io.lenfifowrite.zipWithIndex.foreach { case (fifo, i) =>
              when(prior === i.U) {
                fifo.write := true.B
                fifo.din := DataLen
              }
            }
            io.fiforead.read := false.B
            //当前这个周期也是要写入的
            //io.fifowrite.foreach(_.write := false.B)
            state := sIdle
        }
      }
	  }
  }
}