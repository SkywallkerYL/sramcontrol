package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._
/*********
数据分发模块的核心模块
功能：根据优先级，从fifo中读出数据，从Datalenfifo中获取总的数据长
优先级更高的fifo 不空时，优先从里面读
每<=60个数据包计算一次CRC，校验数据，然后将校验数据添加到尾部，一块发送给仲裁模块
每8bit发送一次，64个数据作为最大包长。将对应的Datalength存如拆包后的fifo中
并且将仲裁模块返回的地址存如地址fifo中
$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

更好的方法是仲裁模块决定数据包的长度，这边根据返回的end信号，终止数据包长度的计算
这样就可以支持地址分散。但这个信号已经加在addrchannel里面了，这里就不改了
就是Addr通道的valid拉高时，表面当前仲裁模块把数据包的长度和地址都返回了，就直接存入fifo中
但这个还没实现，还是靠上边拆分数据包的方法。其实就是我拆我的，你存你的，接受你返回的地址和长度

*********/
class ScaterCore extends Module with Config {
  val io = IO(new Bundle{
    //数据fifo读
    val datafiforead = Vec(priornum,Flipped(new ReaderIO(DataWidth)))
    //数据包总长度fifo读
    val lenfiforead = Vec(priornum,Flipped(new ReaderIO(lenwidth)))

    //仲裁模块写数据请求
    val ArbiterData = Flipped(new DataChannel(DataWidth))
    //仲裁模块返回的地址
    val ArbiterAddr = new AddrChannel(AddrWidth)

    //拆包后数据包个数写入fifo 
    val unpackedNumFifoWrite = Vec(priornum,Flipped(new WriterIO(DataWidth)))
    //拆包后数据包长度写入fifo
    val unpackedLenFifoWrite = Vec(priornum,Flipped(new WriterIO(lenwidth)))
    //拆包后数据包地址写入fifo
    val unpackedAddrFifoWrite = Vec(priornum,Flipped(new WriterIO(AddrWidth)))
    

  })
  io.datafiforead.foreach(_.read := false.B)
  io.lenfiforead.foreach(_.read := false.B)
  io.ArbiterData.valid := false.B
  io.ArbiterData.data := 0.U
  io.ArbiterData.sop := false.B
  io.ArbiterData.eop := false.B
  io.ArbiterData.prior := 0.U
  io.ArbiterAddr.ready := false.B
  io.unpackedNumFifoWrite.foreach(_.write := false.B)
  io.unpackedLenFifoWrite.foreach(_.write := false.B)
  io.unpackedAddrFifoWrite.foreach(_.write := false.B)
  io.unpackedNumFifoWrite.foreach(_.din := 0.U)
  io.unpackedLenFifoWrite.foreach(_.din := 0.U)
  io.unpackedAddrFifoWrite.foreach(_.din := 0.U)
  //CRC 模块
  val crc = Module(new CrcModel)
  crc.io.crcen := false.B
  crc.io.data := 0.U
  crc.io.rst := false.B
  
  val crcCount = RegInit(0.U(2.W))
  val crcData = MuxLookup(crcCount,0.U,Array(
    0.U -> crc.io.crc(31,24),
    1.U -> crc.io.crc(23,16),
    2.U -> crc.io.crc(15,8),
    3.U -> crc.io.crc(7,0)
  ))

  //统计fifo的空情况
  val fifo_empty = Wire(Vec(priornum,Bool()))
  for(i <- 0 until priornum){
    fifo_empty(i) := io.datafiforead(i).empty
  }
  //总的fifo的空情况
  val fifo_empty_all = fifo_empty.reduce(_ && _)
  //当优先级更高的fifo不空时，确认prior的值
  val priorMux = Wire(UInt(priorwidth.W))
  priorMux := 0.U
  for(i <- 0 until priornum){
    when(!fifo_empty(i)){
      priorMux := i.U
    }
  }
  //记录选择的优先级,防止中途改变
  val prior = RegInit(0.U(priorwidth.W)) 
  //当前处理的数据 
  val datain = io.datafiforead(prior).dout
  val lenin = io.lenfiforead(prior).dout
  //当前数据总的长度
  val DataLen = RegInit(0.U(lenwidth.W))
  //记录数据的一个DataLen,统计拆包后的数据包长度//真实的DataLen为DataLen+1
  val unpackDataLen = RegInit(0.U(lenwidth.W))
  //记录拆包后的数据包个数
  val unpackDataNum = RegInit(0.U(lenwidth.W))
  
  //主状态机
  val sIdle :: sCrc :: sData :: sUpdate :: Nil = Enum(4)
  val state = RegInit(sIdle)
  switch(state){
  	is(sIdle){
      //当有数据包长度fifo不空时，读取数据包长度
      when(!fifo_empty_all){
        state := sData
        prior := priorMux
	      DataLen := 0.U
        //读取数据包长度 和 数据
        io.datafiforead(priorMux).read := true.B
        io.lenfiforead(priorMux).read := true.B
        unpackDataLen := 0.U
        unpackDataNum := 0.U
        //注意fifo的模型要保持数据 
        //就是读取一次，数据还在，直到下一次读取
        //crc 复位
        crc.io.rst := true.B
      }
    }
    is(sCrc){
      //拿到了数据 ，一边计算CRC，一边发送数据 
      io.ArbiterData.valid := true.B
      io.ArbiterData.data := datain 
      crc.io.data := datain
      
      when(io.ArbiterData.ready){
        io.datafiforead(prior).read := true.B
        DataLen := DataLen + 1.U
        unpackDataLen := unpackDataLen + 1.U
        crc.io.crcen := true.B 
        //当达到最大crc长度 或者 datalen 达到总的长度时，结束crc的输入
        when(DataLen === lenin || unpackDataLen === maxcrcnum.U-1.U){
          io.datafiforead(prior).read := false.B
          state := sData
          crcCount := 0.U 
          unpackDataNum := unpackDataNum + 1.U
        }
      }
    }
    is(sData){
      io.ArbiterData.valid := true.B
      io.ArbiterData.data := crcData
      
      when(io.ArbiterData.ready){
        unpackDataLen := unpackDataLen + 1.U
        crcCount := crcCount + 1.U
        //当达到最大crc长度 或者 datalen 达到总的长度时，结束crc的输入
        when(crcCount === 3.U){
          io.ArbiterAddr.ready := true.B
          //这里默认这个addr一直拉高，不考虑其不拉高的情况。
          //要考虑，因为这个addr是从仲裁模块返回的，可能会有延迟
          //也可以不考虑，因为仲裁模块把data的ready拉高了
          when(io.ArbiterAddr.valid){
            //写入当前拆包的数据的地址 和数据包长度
            io.unpackedLenFifoWrite(prior).write := true.B
            io.unpackedLenFifoWrite(prior).din := unpackDataLen 
            io.unpackedAddrFifoWrite(prior).write := true.B
            io.unpackedAddrFifoWrite(prior).din := io.ArbiterAddr.addr
            when(DataLen =/= lenin){
              state := sCrc
              unpackDataLen := 0.U 
              io.datafiforead(prior).read := true.B
            }.otherwise{
              state := sUpdate
            }
          }.otherwise{
            io.ArbiterData.valid := false.B 
            crcCount := crcCount
            unpackDataLen := unpackDataLen
          }
        }
      }
    }
		//向外部输出DataLen和prior
		is(sUpdate){
      //整个数据包拆分完成，写入数据包个数
      io.unpackedNumFifoWrite(prior).write := true.B
      io.unpackedNumFifoWrite(prior).din := unpackDataNum
      state := sIdle
		}
  }
}