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
    val inportid = MixedVec(Seq.fill(portnum)(Flipped(new AxiStream(portwidth))))
    val infinish = MixedVec(Seq.fill(portnum)(Output(Bool())))
    //16个目的端口fifo的读入
    val destfiforead = MixedVec(Seq.fill(portnum)((new ReaderIO(DataWidth))))
    //16个目的端口lenfifo的读入
    val destlenfiforead = MixedVec(Seq.fill(portnum)((new ReaderIO(lenwidth))))

    //16个目的端口的当前处理的源端口id
    val destportid = MixedVec(Seq.fill(portnum)(Input(UInt(portwidth.W))))
    val destidready = MixedVec(Seq.fill(portnum)(Input(Bool())))
    val destidvalid = MixedVec(Seq.fill(portnum)(Output(Bool())))
    val destfinish = MixedVec(Seq.fill(portnum)(Input(Bool())))
    val toscaterport = MixedVec(Seq.fill(portnum)(Output(UInt(portwidth.W))))
  })
  for(i <- 0 until portnum){
    io.toscaterport(i) := 0.U
    for (j <- 0 until portnum){
      when(io.inportid(j).data === i.U){
        io.toscaterport(i) := j.U
      }
    }
  }

  //根据destportid 将输入输出的fifo互联起
  for(i <- 0 until portnum){
    io.destfiforead(i) <> io.infiforead(i)
    io.destlenfiforead(i) <> io.inlenfiforead(i)
    for (j <- 0 until portnum){
      when(io.destportid(i) === j.U){
        //这里要对输入输出分别进行操作 这样才能保证判断的正确性,进根据
        //改端口对应的id来判断
        io.destfiforead(i).dout := io.infiforead(j).dout
        io.destfiforead(i).empty := io.infiforead(j).empty
        io.destlenfiforead(i).dout := io.inlenfiforead(j).dout
        io.destlenfiforead(i).empty := io.inlenfiforead(j).empty
      }
    }
  }
  for(j <- 0 until portnum){
    for ( i<- 0 until portnum){
      when(io.destportid(i) === j.U){
        //这里要对输入输出分别进行操作
        io.infiforead(j).read := io.destfiforead(i).read
        io.inlenfiforead(j).read := io.destlenfiforead(i).read
      }
    }
  }
  //对inport id 的端口进行互联
  for (i <- 0 until portnum){
    io.inportid(i).ready := false.B
    io.infinish(i) := false.B
    for (j <- 0 until portnum){
      when(io.destportid(j) === i.U){
        io.inportid(i).ready := io.destidready(j)
        io.infinish(i) := io.destfinish(j)
      }
    }
  }
  //对destportid的端口进行互联
  for (i <- 0 until portnum){
    io.destidvalid(i) := false.B
    for (j <- 0 until portnum){
      when(io.inportid(j).data === j.U){
        io.destidvalid(i) := io.inportid(j).valid
        
      }
    }
  }
}