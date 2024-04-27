package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/******
吼吼吼 自己实现的数据分发模块

功能：
1.外部有写入信号时，根据数据优先级将数据写入对应的FIFO中  
2.根据优先级从fifo取出数据，对数据进行拆包
3.目前就按照每64Byte为一个包，向仲裁模块发送写请求，和写入的数据
4.8bit 并行送给CRC计算模块，在一个数据包结尾处补上校验数据，校验数据和数据一起给仲裁模块写入
5.采用CRC-32 ，因此每60个数据+4个校验数据为一个数据包

内部记录每个拆分的数据包长度，以及仲裁模块的写入地址 

以及记录当前帧数据包总的长度。

6.数据读出时，根据优先级 从fifo读出数据的地址和长度
7.向仲裁模块发送读请求，获取数据
8.数据读出后，根据数据包长度，将数据送给CRC模块计算校验数据。 
9.校验过的数据送入一个fifo中，等待校验通过后输出.CRC部分不存入fifo ，
如果校验不通过，并且上一包数据已经完全输出时，则给fifo发送flush请求，
将读指针增加该数据包的长度，这里有一个地址溢出的问题，考虑一下就可以。这包数据作废，同时对应的长度计数器也要减去。
10.校验通过的数据输出到外部 通过fifo拉高，往外部输出。
用两个长度存储器，一个存当前往外输出的，一个存当前从仲裁模块拿的数据的。

最后一个数据发送eop,如果是校验失败了，并且正好是最后一个数据包，则valid拉低就行
//这里会有一个延迟的问题。 即，外部有读请求的时候，至少要等一包数据校验完之后才能输出。
//因此除了第一包数据延迟。 当外部有读请求的时候，，也要向仲裁模块申请读请求，获取数据，这样保证后边的数据延迟不会太大。
不然每次都得等第一包数据校验完。
这样子fifo里面会同时有不同包的数据。

//还有一个问题，接受数据的端口和目的端口是不一样的， 
这里想的一个方法是scater模块外部还要套一个仲裁  
可以实现一个AHB或者什么的 。根据输入数据的目的端口，和优先级，将数据发送给对应的scater模块。
但这里先不管了，先实现一个scater模块，然后再考虑这个问题。Scater只管接受数据，做校验，输出数据。

*******/
class DataScater extends Module with Config {
  val io = IO(new Bundle{
    val Rd = new ChannelOut(DataWidth)
    //与Bridge模块通信
    //val BridgeData = new DataChannel(DataWidth)
    val Bridgefiforead    = Flipped(new ReaderIO(DataWidth))
    val Bridgelenfiforead = Flipped(new ReaderIO(lenwidth))

    //与MMU模块通信 写数据通道
    val WrData = Flipped(new DataChannel(DataWidth))
    val WrAddr = (new AddrChannel(AddrWidth))
    //与MMU模块通信 读数据通道
    val RdData = (new DataChannel(DataWidth))
    val RdAddr = Flipped(new AddrChannel(AddrWidth))
  })
    //一些默认的输出
    io.Rd.valid := false.B
    io.Rd.data := 0.U
    io.Rd.sop := false.B
    io.Rd.eop := false.B

    //priornum个优先级对应的数据fifo
    val DataFifo = Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,DataWidth)))
    DataFifo.foreach(_.io.flush := false.B)
    //priornum个length Reg 统计当前接受数据包的长度
    val DataLen = RegInit(VecInit(Seq.fill(priornum)(0.U(lenwidth.W))))

    //priornum个length fifo 统计已经存入的数据包长度 就是整包数据都写进去后，把DataLen写入这个fifo
    val DataLenFifo = Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,lenwidth)))
    DataLenFifo.foreach(_.io.flush := false.B)
    //一个处理模块，从外部接受数据，根据优先级写入fifo，并且统计数据长度 记录到Datalen
    val InProcess = (Module(new DataInProcess)) 
    InProcess.io.fiforead <> io.Bridgefiforead
    InProcess.io.lenfiforead <> io.Bridgelenfiforead
    //依次连接每一个fifo
    for(i <- 0 until priornum){
      InProcess.io.fifowrite(i) <> DataFifo(i).io.fifo.fifowrite
      InProcess.io.lenfifowrite(i) <> DataLenFifo(i).io.fifo.fifowrite
    }
    //一个处理模块，根据优先级，从fifo中读出数据，从Datalenfifo中获取总的数据长，
    //优先级更高的fifo 不空时，优先从里面读
    //每<=60个数据包计算一次CRC，校验数据，然后将校验数据添加到尾部，一块发送给仲裁模块
    //每8bit发送一次，64个数据作为最大包长。将对应的Datalength存如拆包后的fifo中
    //并且将仲裁模块返回的地址存如地址fifo中
    val ScaterCoreInst = (Module(new ScaterCore))
    io.WrData <> ScaterCoreInst.io.ArbiterData
    io.WrAddr <> ScaterCoreInst.io.ArbiterAddr
    //priornum个fifo 存拆包后的数据长度。同时统计拆了多少个数据包。
    val ScaterDataLenFifo = Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,lenwidth)))
    //priornum个fifo 存拆完后的数据包个数。 
    val ScaterDataNumFifo = Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,lenwidth)))
    //priornum个fifo 仲裁模块返回的写入地址
    val ScaterAddrFifo = Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,AddrWidth)))
    
    ScaterDataLenFifo.foreach(_.io.flush := false.B)
    ScaterDataNumFifo.foreach(_.io.flush := false.B)
    ScaterAddrFifo.foreach(_.io.flush := false.B)
    //模块端口连接
    //顺序连接每一个fifo端口 与Datafifo相连

    // for (i <- 0 until priornum){
    //   ScaterCoreInst.io.datafiforead(i) <> DataFifo(i).io.fifo.fiforead
    //   ScaterCoreInst.io.lenfiforead(i) <> DataLenFifo(i).io.fifo.fiforead
    //   ScaterCoreInst.io.unpackedNumFifoWrite(i) <> ScaterDataNumFifo(i).io.fifo.fifowrite
    //   ScaterCoreInst.io.unpackedLenFifoWrite(i) <> ScaterDataLenFifo(i).io.fifo.fifowrite
    //   ScaterCoreInst.io.unpackedAddrFifoWrite(i) <> ScaterAddrFifo(i).io.fifo.fifowrite
    // }
    (ScaterCoreInst.io.datafiforead zip DataFifo.map(_.io.fifo.fiforead)).foreach { case (core, fifo) => core <> fifo }
    (ScaterCoreInst.io.lenfiforead zip DataLenFifo.map(_.io.fifo.fiforead)).foreach { case (core, fifo) => core <> fifo }
    (ScaterCoreInst.io.unpackedNumFifoWrite zip ScaterDataNumFifo.map(_.io.fifo.fifowrite)).foreach { case (core, fifo) => core <> fifo }
    (ScaterCoreInst.io.unpackedLenFifoWrite zip ScaterDataLenFifo.map(_.io.fifo.fifowrite)).foreach { case (core, fifo) => core <> fifo }
    (ScaterCoreInst.io.unpackedAddrFifoWrite zip ScaterAddrFifo.map(_.io.fifo.fifowrite)).foreach { case (core, fifo) => core <> fifo }
    ScaterCoreInst.io.ArbiterData <> io.WrData
    ScaterCoreInst.io.ArbiterAddr <> io.WrAddr
    //一个处理模块，从ScaterDataNumFifo读出数据包个数，从ScaterDataFifo读出数据长度，从AddrFifo读出地址
    //向仲裁模块发送读请求 获取数据，计算CRC校验数据，校验通过后，将数据输出到外部 
    //这里还得想一下数据校验不过怎么办，但是大致的流程就是读出来 发出去。
    //状态机控制，包头给sop,包尾给eop
    val DataCollectorInst = (Module(new DataCollector))
    DataCollectorInst.io.Rd <> io.Rd
    (DataCollectorInst.io.unpackedNumFifoRead zip ScaterDataNumFifo.map(_.io.fifo.fiforead)).foreach { case (core, fifo) => core <> fifo }
    (DataCollectorInst.io.unpackedLenFifoRead zip ScaterDataLenFifo.map(_.io.fifo.fiforead)).foreach { case (core, fifo) => core <> fifo }
    (DataCollectorInst.io.unpackedAddrFifoRead zip ScaterAddrFifo.map(_.io.fifo.fiforead)).foreach { case (core, fifo) => core <> fifo }
    io.RdData <> DataCollectorInst.io.ArbiterData 
    io.RdAddr <> DataCollectorInst.io.ArbiterAddr  

    ////一个Datafifo，存经过Crc计算后的数据，当这一包的数据全部校验完成后，最后一个数据包发送end信号，第一个数据包发送start信号
    //val AfterCrcDataFifo = Module(new fiforam(MaxfifoNum,DataWidth))
    ////如果校验通过，将数据输出到外部，如果校验不通过，将数据丢弃，同时将读指针增加对应的长度。注意读指针的溢出问题。
    ////一个模块处理数据的输出。 
    ////一个状态机 当上一包数据校验通过后，接受start信号 end信号和读出数据的长度信号，从Datafifo中读出数据，输出到外部，同时输出sop和eop信号。




}
