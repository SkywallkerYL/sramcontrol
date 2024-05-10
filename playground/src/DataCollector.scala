package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._
/*********
数据汇集模块
当外部有读请求的时候，从优先级最高的fifo中读取数据包的个数 和 数据包的长度
以及数据包的地址，然后根据地址从仲裁模块获取数据，然后进行CRC校验

目前考虑不管数据校验是否通过，都发送出去，只是在包尾加一个校验通过的标志位
校验通过发全1
校验不通过发全0

注意fifo里面存的数据包个数 是datanum ，没有-1 
但是数据的长度是-1了的
*********/
class DataCollector extends Module with Config {
  val io = IO(new Bundle{
    //与外部的通信
    val Rd = new ChannelOut(DataWidth)
    //数据包个数fifo读
    val unpackedNumFifoRead = MixedVec(Seq.fill(priornum)(Flipped(new ReaderIO(DataWidth))))
    //数据包长度fifo读
    val unpackedLenFifoRead = MixedVec(Seq.fill(priornum)(Flipped(new ReaderIO(lenwidth))))
    //数据包地址fifo读
    val unpackedAddrFifoRead = MixedVec(Seq.fill(priornum)(Flipped(new ReaderIO(AddrWidth))))
    //仲裁模块读数据请求
    val ArbiterAddr = Flipped(new AddrChannel(AddrWidth))
    //仲裁模块返回的数据
    val ArbiterData = (new DataChannel(DataWidth))
  })
  //一些默认的输出
  io.Rd.valid := false.B
  io.Rd.data := 0.U
  io.Rd.sop := false.B
  io.Rd.eop := false.B
  io.unpackedNumFifoRead.foreach(_.read := false.B)
  io.unpackedLenFifoRead.foreach(_.read := false.B)
  io.unpackedAddrFifoRead.foreach(_.read := false.B)
  io.ArbiterData.ready := false.B
  io.ArbiterAddr.valid := false.B
  io.ArbiterAddr.addr := 0.U
  io.ArbiterAddr.length := 0.U
  io.ArbiterAddr.prior := 0.U
  //CRC 模块
  val crc = Module(new CrcModel)
  crc.io.crcen := false.B
  crc.io.data := io.ArbiterData.data
  crc.io.rst := false.B
  
  val crcCount = RegInit(0.U(2.W))
  val crcData = MuxLookup(crcCount,0.U,Array(
    0.U -> crc.io.crc(31,24),
    1.U -> crc.io.crc(23,16),
    2.U -> crc.io.crc(15,8),
    3.U -> crc.io.crc(7,0)
  ))

  //统计fifo的空情况 以数据包的个数fifo为准,一个数据包的个数fifo不空就不空
  //一个数据包的个数fifo的数据对应多个数据包的长度fifo和地址fifo的数据
  val fifo_empty = Wire(Vec(priornum,Bool()))
  for(i <- 0 until priornum){
    fifo_empty(i) := io.unpackedNumFifoRead(i).empty
  }
  //总的fifo的空情况
  val fifo_empty_all = fifo_empty.reduce(_ && _)
  //当优先级更高的fifo不空时，确认prior的值
  //这里其实是一个输出调度的情况，后边可以优化，就是当制定一种策略，
  //输入是fifo的空情况，输出是一个优先级的选择
  val priorMux = Wire(UInt(priorwidth.W))
  priorMux := 0.U
  for(i <- 0 until priornum){
    when(!fifo_empty(i)){
      priorMux := i.U
    }
  }
  //记录选择的优先级,防止中途改变
  val prior = RegInit(0.U(priorwidth.W)) 
  io.ArbiterAddr.prior := prior
  //记录当前数据包的CRC校验状态
  val crcState = RegInit(0.U(DataWidth.W))

  //当前处理的数据 
  val unpackDataNumin = MuxLookup(prior,0.U,
    (0 until priornum).map(i => i.U -> io.unpackedNumFifoRead(i).dout))
  //io.unpackedNumFifoRead(prior).data
  
  val unpackDataLenin = MuxLookup(prior,0.U,
    (0 until priornum).map(i => i.U -> io.unpackedLenFifoRead(i).dout))
  val unpackDataAddrin = MuxLookup(prior,0.U,
    (0 until priornum).map(i => i.U -> io.unpackedAddrFifoRead(i).dout))
  val datain = io.ArbiterData.data
  //当前拆包后的数据包长度 当这个数据包长度等于数据包长度fifo的数据-4时，表明这一包的数据已经读完
  //这个不一定了，数据包长度由下面的模块决定因此这里自己统计
  //剩下的4个数据是CRC数据 不要向外部输出
  val unpackDataLen = RegInit(0.U(lenwidth.W))
  //记录拆包后的数据包个数 当这个数据包个数等于数据包个数fifo的数据时，发送eop
  val unpackDataNum = RegInit(0.U(lenwidth.W))
  
  //主状态机
  val sIdle :: sAddr :: sCrc :: sData :: sCheck :: sUpdate :: Nil = Enum(6)
  val state = RegInit(sIdle)
  switch(state){
  	is(sIdle){
      //当有数据包长度fifo不空时，读取数据包长度 并且外部有读请求时，开始读取数据
        when(!fifo_empty_all && io.Rd.ready){
            state := sAddr
            prior := priorMux
            //读取数据包的个数 和 数据包的长度 和 数据包的地址
            io.unpackedNumFifoRead.zipWithIndex.foreach { case (fifo, i) =>
              when(priorMux === i.U) {
                fifo.read := true.B
              }
            }
            io.unpackedLenFifoRead.zipWithIndex.foreach { case (fifo, i) =>
              when(priorMux === i.U) {
                fifo.read := true.B
              }
            }
            io.unpackedAddrFifoRead.zipWithIndex.foreach { case (fifo, i) =>
              when(priorMux === i.U) {
                fifo.read := true.B
              }
            }
            unpackDataLen := 0.U
            unpackDataNum := 0.U
            //注意fifo的模型要保持数据 
            //就是读取一次，数据还在，直到下一次读取
            //crc 复位
            crc.io.rst := true.B
        }
    }
    is(sAddr){
        //向仲裁模块发送读请求
        io.ArbiterAddr.valid := true.B
        io.ArbiterAddr.addr := unpackDataAddrin
        io.ArbiterAddr.length := unpackDataLenin
        unpackDataLen := 0.U 
        when(io.ArbiterAddr.ready){
          state := sData
        }
    }
    is(sData){
        //获取数据 ，一边计算CRC，一边发送数据 
        io.ArbiterData.ready := io.Rd.ready
        //crc.io.data := io.ArbiterData.data
        io.Rd.valid := io.ArbiterData.valid
        io.Rd.data := io.ArbiterData.data
        io.Rd.sop := unpackDataLen === 0.U && unpackDataNum === 0.U
        when(io.ArbiterData.valid && io.ArbiterData.ready){
            //数据有效时，计算CRC
            crc.io.crcen := true.B
            unpackDataLen := unpackDataLen + 1.U
            //当数据长度达到数据包长度-4时，表明这一包数据已经读完，剩下的是CRC数据
            when(unpackDataLen === unpackDataLenin-4.U){
              state := sCrc
              crcCount := 0.U 
            }
        }
    }
    is(sCrc){
        io.ArbiterData.ready := io.Rd.ready
        when(io.ArbiterData.valid && io.ArbiterData.ready){
            unpackDataLen := unpackDataLen + 1.U
            crc.io.crcen := true.B
            crcCount := crcCount + 1.U
            //当crc计算完成
            when(crcCount === 3.U){
              //直接跳入check状态
              state := sCheck
              //处理的数据包个数加1
              unpackDataNum := unpackDataNum + 1.U
            }
        }
    }
    is(sCheck){
        //校验通过则输出全1，校验不通过则输出全0
        when(crc.io.crc === 0.U && crcState === 0.U){
          crcState := 0.U
        }.otherwise{
          //8bit的数据 全1
          crcState := 255.U
        }
        //当前包的数据没有处理完
        when(unpackDataNum =/= unpackDataNumin){
          state := sAddr
          //获取下一个拆包的数据包长度 和 数据包地址
          io.unpackedAddrFifoRead.zipWithIndex.foreach { case (fifo, i) =>
            when(prior === i.U) {
              fifo.read := true.B
            }
          }
          io.unpackedLenFifoRead.zipWithIndex.foreach { case (fifo, i) =>
            when(prior === i.U) {
              fifo.read := true.B
            }
          }
          unpackDataLen := 0.U
          //crc 归零
          crc.io.rst := true.B
        }.otherwise{
          //当前包的数据处理完
          state := sUpdate
        }
    }
		//向外部输出该包数据是否有误 发送包尾eop
	  is(sUpdate){
          //
          io.Rd.valid := true.B
          io.Rd.eop := true.B
          io.Rd.data := crcState
          when(io.Rd.ready){
            state := sIdle
          }
	  }
  }
}