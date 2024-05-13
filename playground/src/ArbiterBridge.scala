package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/*********
输入转接桥
实现的功能是将16个端口的输入数据转发给对应的目的端口
大概的功能是
输入模块添加一个目的端口的指示信号portwidth 位
每来一个数据包, 就记录该数据包的目的端口 ,存入一个Fifo中 

然后该模块根据每个端口的目的端口,将datafifo lenfifo的read输出连接到对应的端口

该模块输入是16个端口的datafifo lenfifo的read输出
以及16个端口的目的端口指示信号

还有16个端口当前处理的源端口id
每次只处理一个端口的数据,要锁存当前的Id
输出是16个端口的datafifo lenfifo的read输入

*********/
class ArbiterBridge extends Module with Config {
  val io = IO(new Bundle{
    //16个读入端口的fiforead输出
    val infiforead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(DataWidth))))
    //16个读入端口的lenfiforead输出
    val inlenfiforead = MixedVec(Seq.fill(portnum)(Flipped(new ReaderIO(lenwidth))))
    //16个读入端口当前输出的目的端口id 
    val source2destportidIn = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(portwidth))))
    val infinish = MixedVec(Seq.fill(portnum)(Output(Bool())))
    //16个目的端口fifo的读入
    val destfiforead = MixedVec(Seq.fill(portnum)((new ReaderIO(DataWidth))))
    //16个目的端口lenfifo的读入
    val destlenfiforead = MixedVec(Seq.fill(portnum)((new ReaderIO(lenwidth))))

    //16个目的端口的当前处理的源端口id 即DataInProcess 下的sourceport
    val dest2sourceportid = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(portwidth))))
    //16个目的端口接受的源端口id 即DataInProcess 下的inport
    val source2destportidOut = MixedVec(Seq.fill(portnum)((new AxiStream(portwidth))))
    //16个目的端口的finish
    val destfinish = MixedVec(Seq.fill(portnum)(Input(Bool())))
  })
  //根据destportid 将输入输出的fifo互联起
  for(i <- 0 until portnum){
    io.destfiforead(i).dout     := 0.U
    io.destfiforead(i).empty    := false.B
    io.destlenfiforead(i).dout  := 0.U
    io.destlenfiforead(i).empty := false.B
    io.dest2sourceportid(i).ready := true.B
    for (j <- 0 until portnum){
      //当目的端口握手成功后,发送valid数据,将
      //将目的端口的fifo输入与源端口的fifo输出连接
      when(io.dest2sourceportid(i).data === j.U && io.dest2sourceportid(i).valid){
        //这里要对输入输出分别进行操作 这样才能保证判断的正确性,进根据
        //改端口对应的id来判断
        io.destfiforead(i).dout     := io.infiforead(j).dout
        io.destfiforead(i).empty    := io.infiforead(j).empty
        io.destlenfiforead(i).dout  := io.inlenfiforead(j).dout
        io.destlenfiforead(i).empty := io.inlenfiforead(j).empty
      }
    }
  }
  for(j <- 0 until portnum){
    io.infiforead(j).read     := false.B
    io.inlenfiforead(j).read  := false.B
    io.infinish(j) := false.B
    for ( i<- 0 until portnum){
      //互联的原理和上边一致,目的端口给出的源Id和当前的Id相等时,并且valid有效
      //将fifo读使能进行互联
      when(io.dest2sourceportid(i).data === j.U && io.dest2sourceportid(i).valid){
        //这里要对输入输出分别进行操作
        io.infiforead(j).read     := io.destfiforead(i).read
        io.inlenfiforead(j).read  := io.destlenfiforead(i).read
        io.infinish(j) := io.destfinish(i)
      }
    }
  }
  //对source2destportidOut的端口进行互联
  for(i <- 0 until portnum){
    io.source2destportidOut(i).valid := false.B
    io.source2destportidOut(i).data := 0.U
    io.source2destportidOut(i).last := false.B
    for(j <- 0 until portnum){
      //当源端口有Valid,并且源端口给出的Id和当前的Id相等时,将输出的valid和data连接
      //但是过去的data,要改成源端口的Id
      when(io.source2destportidIn(j).data === i.U && io.source2destportidIn(j).valid){
        io.source2destportidOut(i).valid := io.source2destportidIn(j).valid
        io.source2destportidOut(i).data := j.U
      }
    }
  }

  //对source2destportidIn 的端口进行互联 
  //对source的握手进行操作
  for (i <- 0 until portnum){
    io.source2destportidIn(i).ready := false.B 
    for (j <- 0 until portnum){
      when(io.dest2sourceportid(j).data === i.U && io.dest2sourceportid(j).valid){
        io.source2destportidIn(i).ready := io.source2destportidOut(j).ready
      }
    }
  }
}