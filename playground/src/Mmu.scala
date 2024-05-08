package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._
/*********
内存管理单元

功能：

ScatterCollecter发送写数据请求，Mmu接受写数据请求，将数据写入SRAM中

同时进行一个内存的管理， 
接受外部的写数据请求，外部只会发一个写数据请求和写的长度
然后根据写的长度，将数据写入SRAM中，同时将写入的地址返回给ScatterCollecte

注意这里要实现一个内部的地址管理，对该MMU负责的2KB内存进行管理

数据是2KB，一个地址对应1个B 
所以地址是11位 

每次处理外部发来的数据，记录当前数据的首地址

默认发出去的Addrvalid 一直拉高
直到接收到ArbiterAddr ready请求，表明上边模块对当前数据的拆包
拆完了

MMU进行一个内存的管理 
记录每一个地址是否是dirty的。
dirty表明这个地址已经被写过了。用一个Ram来存

然后呢，每次写数据的时候，根据当前的地址，将对应的dirty位设置为1

然后呢，每次读数据的时候，根据当前的地址，将对应的dirty位设置为0

这样就可以实现一个内存的管理了

由此一个数据包可能不会写入连续的地址，因此要管理一张表。

他由3个fifo组成

一个是当前数据的写入首地址，即传给数据分散模块的地址
然后是由于数据可能不连续，所以要记录每个地址和对应的长度。以及分散地址的个数。
当然这个是每个优先级都有一个这样的表 因为读出的时候是根据优先级读的

比如Addr起始 是3 写入了64个数据，这个能写的最大长度由空闲地址管理模块决定
0-21 从3开始写
22-55 从60开始写
56-63 从134开始写
那么就要记录3个地址，和3个长度 以及当前分散了3个地址。

//实现一个Sram分配模块 最小分配单元是一块Sram，即1KB 

当某个通道有写入请求时，首先向该模块发送一个请求分配。然后该模块将空闲的Sram分配给该通道

当某个通道的数据全部读出后，向该模块发送一个释放请求，将该Sram释放。

//
还要有一个模块，进行一个空闲地址的管理，
该模块首先向Sram管理模块发送一个请求，获取一个分配的Sram编号 0-31

每一个MMU设置32个寄存器
存对应编号的Sram 剩余空间的大小。
为分配之前，大小是0,分配后，大小是1KB 数值即为1KB/1B = 1024  //1023

每次写入数据时，寄存器的数值-1,
每次读出数据时，寄存器的数值+1，
对寄存器的值进行动态管理 。
注意 如果同时有写入和读出，需要保证读写地址是对于同一个id的Sram。

用一个Flag来记录空闲情况
当读出数据时，把数据读出后的地址 和长度 传给该模块。存如两个fifo中。

当写地址的指针首次到达1023时，表明该Sram已经写满，当然实际上前面
可能已经有数据读出，空间有空闲了。这个时候就往外发空闲fifo中的地址。
这个时候把Flag拉高，表明该Sram已经写过一边。

当Flag为低的时候，给MMU返回的地址就是写入指针，不能是1024-剩余空间的大小
因为这个时候可能有数据读出，空间有空闲了，写入指针不一定等于1024-剩余空间的大小
最大的长度即1024-写入指针
当Flag为高的时候，给MMU返回的地址就是空闲fifo中的地址。和空闲fifo的长度。

如果没有读出，那么就向Sram管理模块申请。
注意这里状态不能卡住，比如Sram管理模块一直不给分配，
当又有数据读出的时候，即模块又空闲了，就不用申请，继续写入数据。

*********/
class Mmu extends Module with Config {
  val io = IO(new Bundle{
    //ScatterCollecter写数据请求
    val WrData = (new DataChannel(DataWidth))
    //ScatterCollecter写数据返回地址通道
    val WrAddr = Flipped(new AddrChannel(AddrWidth))
    //ScatterCollecter 读数据通道
    val RdData = Flipped(new DataChannel(DataWidth))
    //ScatterCollecter 读数据请求地址通道
    val RdAddr = (new AddrChannel(AddrWidth))

    //SramControl写数据数据通道
    val SramWr  = (new AxiWrite)
    //SramControl读数据数据通道
    val SramRd  = (new AxiRead)
    //Sram 请求分配通道
    val SramReq = Flipped(new AxiStream(SramIdwidth))
    //Sram 释放通道
    val SramRelease = new AxiStream(SramIdwidth)

  })
  //一些默认的输出
  io.WrData.ready := false.B

  io.WrAddr.valid := false.B
  io.WrAddr.addr := 0.U
  io.WrAddr.length := 0.U
  io.WrAddr.prior := 0.U

  io.RdData.valid := false.B
  io.RdData.data := 0.U
  io.RdData.sop := false.B
  io.RdData.eop := false.B
  io.RdData.prior := 0.U

  io.RdAddr.ready := false.B
  
  io.SramWr.awvalid := false.B
  io.SramWr.awaddr := 0.U
  io.SramWr.awlen := 0.U
  io.SramWr.wvalid := false.B 
  io.SramWr.wdata := 0.U
  io.SramWr.wlast := false.B

  io.SramRd.arvalid := false.B
  io.SramRd.araddr := 0.U
  io.SramRd.arlen := 0.U
  io.SramRd.rready := false.B
  //空闲地址管理模块

  val FreeAddrManagerInst = Module(new FreeAddrManager)
  FreeAddrManagerInst.io.SramReq <> io.SramReq
  FreeAddrManagerInst.io.SramRelease <> io.SramRelease
  FreeAddrManagerInst.io.FreeAddr.ready := false.B 

  FreeAddrManagerInst.io.RdAddr.valid := false.B
  FreeAddrManagerInst.io.RdAddr.data := 0.U
  FreeAddrManagerInst.io.RdAddr.last := false.B

  FreeAddrManagerInst.io.WrAddr.valid := false.B
  FreeAddrManagerInst.io.WrAddr.data := 0.U
  FreeAddrManagerInst.io.WrAddr.last := false.B 
  //每个优先级对应的首地址对应的分散个数的fifo ，记录当前数据包被分到了几个地址
  val PackAddrNumFifo = VecInit(Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,4))).map(_.io))
  //默认值
  PackAddrNumFifo.foreach(_.fifo.fifowrite.write := false.B)
  PackAddrNumFifo.foreach(_.fifo.fiforead.read := false.B)
  PackAddrNumFifo.foreach(_.fifo.fifowrite.din := 0.U)
  PackAddrNumFifo.foreach(_.flush := false.B)
  //Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,4)))
  //每个分散的首地址fifo
  val PackAddrFifo = VecInit(Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,AddrWidth))).map(_.io))
  PackAddrFifo.foreach(_.fifo.fifowrite.write := false.B)
  PackAddrFifo.foreach(_.fifo.fiforead.read := false.B)
  PackAddrFifo.foreach(_.fifo.fifowrite.din := 0.U)
  PackAddrFifo.foreach(_.flush := false.B)
  //Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,AddrWidth)))
  //每个分散的长度fifo
  //val PackLenFifo = VecInit(Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,(SramSizeWidth-1)))))
  val PackLenFifo = VecInit(Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,(SramSizeWidth-1)))).map(_.io))
  PackLenFifo.foreach(_.fifo.fifowrite.write := false.B)
  PackLenFifo.foreach(_.fifo.fiforead.read := false.B)
  PackLenFifo.foreach(_.fifo.fifowrite.din := 0.U)
  PackLenFifo.foreach(_.flush := false.B)
  //Seq.fill(priornum)(Module(new fiforam(MaxfifoNum,7)))

  //记录数据包个数 从0开始 
  val PackNum = RegInit(0.U(4.W))

  //锁存优先级
  val prior = RegInit(0.U(priorwidth.W))
  //锁存首地址
  val FirstAddr = RegInit(0.U(AddrWidth.W))
  val LocalFirstAddr = RegInit(0.U(AddrWidth.W))
  //锁存当前地址能写的最大长度 
  val MaxLen = RegInit(0.U((SramSizeWidth-1).W))
  //记录已经写了的长度
  val WriteLen = RegInit(0.U((SramSizeWidth-1).W))
  //外部写数据请求 处理状态机 当外部有写数据时， 会先向空闲地址管理模块发送请求,获取一个空闲地址
  //状态分配和状态机
  val sIdle :: sAddr :: sWrite :: sWait :: Nil = Enum(4)
  val wrstate = RegInit(sIdle)
  switch(wrstate){
    is(sIdle){
      //当外部有写数据请求时
      when(io.WrData.valid){
        //向空闲地址管理模块发送请求 ,请求一个空闲地址 
        FreeAddrManagerInst.io.FreeAddr.ready := true.B
        when(FreeAddrManagerInst.io.FreeAddr.valid){
          //收到了一个空闲首地址 锁存优先级，和当前地址
          prior := io.WrData.prior
          FirstAddr := FreeAddrManagerInst.io.FreeAddr.data
          LocalFirstAddr := FreeAddrManagerInst.io.FreeAddr.data
          MaxLen := FreeAddrManagerInst.io.MaxLen
          WriteLen := 0.U
          wrstate := sAddr
          PackNum := 0.U 
        }
      }
    }
    is(sAddr){
      //与Sram控制模块进行一次写地址握手，让Sram控制模块进入写Sram的状态
      io.SramWr.awvalid := true.B
      io.SramWr.awaddr := LocalFirstAddr
      when(io.SramWr.awready){
        //写地址握手成功，开始写数据
        wrstate := sWrite
      }
    }
    is(sWrite){
      io.SramWr.wvalid := io.WrData.valid && !PackAddrFifo(prior).fifo.fifowrite.full
      io.SramWr.wdata := io.WrData.data
      io.WrData.ready := io.SramWr.wready
      io.WrAddr.valid := true.B && !PackAddrFifo(prior).fifo.fifowrite.full
      io.WrAddr.addr := FirstAddr
      io.SramWr.awaddr := LocalFirstAddr + WriteLen
      FreeAddrManagerInst.io.WrAddr.data := LocalFirstAddr + WriteLen
      //由于上面的模块在写入数据的时候，最后一个数据会接受当前的一个地址 
      //io.WrAddr.ready 即为WrData.last
      //因此把Addr通道的ready当作最后一个数据的标志
      when(io.SramWr.wvalid &&io.SramWr.wready){
        //地址的信息也要传递给空闲地址管理模块进行一个大小的维护。
        FreeAddrManagerInst.io.WrAddr.valid := true.B
        
        WriteLen := WriteLen + 1.U
        //里面的len是 比真实的Len-1.所以这里当前周期判断相等即可。
        when(WriteLen === MaxLen && !io.WrAddr.ready){
          //写满了 当前空闲地址写满，但是数据包还没写完， 
          //需要申请另外一个空闲地址了 。
          
          //更新分散地址的个数 
          PackNum := PackNum + 1.U
          //记录当前首地址和长度 
          //when(!PackAddrFifo.io.fifo.fifowrite.full && !PackLenFifo.io.fifo.fifowrite.full){
            PackAddrFifo(prior).fifo.fifowrite.din := LocalFirstAddr
            PackAddrFifo(prior).fifo.fifowrite.write := true.B  
            PackLenFifo(prior).fifo.fifowrite.din := WriteLen
            PackLenFifo(prior).fifo.fifowrite.write := true.B
            wrstate := sWait
          io.SramWr.wlast := true.B
          //}
        }.elsewhen(io.WrAddr.ready){
          //写完了
          //when(!PackAddrFifo.io.fifo.fifowrite.full && !PackLenFifo.io.fifo.fifowrite.full){
            PackAddrNumFifo(prior).fifo.fifowrite.din := PackNum
            PackAddrNumFifo(prior).fifo.fifowrite.write := true.B
            PackAddrFifo(prior).fifo.fifowrite.din := LocalFirstAddr
            PackAddrFifo(prior).fifo.fifowrite.write := true.B  
            PackLenFifo(prior).fifo.fifowrite.din := WriteLen
            PackLenFifo(prior).fifo.fifowrite.write := true.B
            wrstate := sIdle
          io.SramWr.wlast := true.B
          //}
        }
      }
    }
    is(sWait){
      FreeAddrManagerInst.io.FreeAddr.ready := true.B
      when(FreeAddrManagerInst.io.FreeAddr.valid){
        LocalFirstAddr := FreeAddrManagerInst.io.FreeAddr.data
        MaxLen := FreeAddrManagerInst.io.MaxLen
        WriteLen := 0.U
        wrstate := sAddr
      }
    }
  }
  //处理读数据请求  
  /*****
  当外部有读请求时，首先识别优先级，然后从对应优先级的fifo中读出地址和长度 以及个数
  然后向Sram控制模块发送读请求，读出数据
  同时把读数据的使能转发给空闲地址管理模块。
  //这里发现其实不需要地址，因为地址都在下面记录的。
  //好像也不需要长度，因为MMU记录了总长度，和已经读取的长度
  *****/
  //记录当前读取的数据的优先级 
  val priorRd = RegInit(0.U(priorwidth.W))
  val priorLocal = io.RdAddr.prior
  //记录已经读取的拆分后的分散地址的个数 
  val PackNumRd = RegInit(0.U(4.W))
  //锁存当前数据包的长度，即要读的数据的长度
  val DataLen = RegInit(0.U((lenwidth).W))
  //记录已经读取的数据的长度
  val ReadLen = RegInit(0.U((SramSizeWidth-1).W))
  //锁存当前读取的首地址 
  val ReadFirstAddr = RegInit(0.U(AddrWidth.W))
  //所存总长度 
  val TotalLen = RegInit(0.U((SramSizeWidth-1).W))
  //锁存当前的分散地址个数  
  val ReadPackNum = RegInit(0.U(4.W))
  //状态分配和状态机
  val sRdIdle ::sRdwait:: sRdAddr :: sRdData :: Nil = Enum(4)
  val rdstate = RegInit(sRdIdle)
  switch(rdstate){
    is(sRdIdle){
      //当有读请求时
      io.RdAddr.ready := true.B
      when(io.RdAddr.valid){
        //读取当前优先级的地址和长度
        PackAddrNumFifo(priorLocal).fifo.fiforead.read := true.B
        PackAddrFifo(priorLocal).fifo.fiforead.read := true.B
        PackLenFifo(priorLocal).fifo.fiforead.read := true.B
        //DataLen := io.RdAddr.length
        priorRd := priorLocal
        rdstate := sRdwait
        PackNumRd := 0.U
      }
    }
    is(sRdwait){
      ReadFirstAddr := PackAddrFifo(priorRd).fifo.fiforead.dout
      ReadPackNum := PackAddrNumFifo(priorRd).fifo.fiforead.dout
      DataLen := PackLenFifo(priorRd).fifo.fiforead.dout
      rdstate := sRdAddr
    }
    is(sRdAddr){
      //向Sram控制模块发送读请求
      //获取了地址
      io.SramRd.arvalid := true.B
      io.SramRd.araddr := ReadFirstAddr //PackAddrFifo(priorRd).io.fifo.fiforead.dout
      io.SramRd.arlen := DataLen //PackLenFifo(priorRd).io.fifo.fiforead.dout
      when(io.SramRd.arready){
        //读请求握手成功
        //TotalLen := PackLenFifo(priorRd).io.fifo.fiforead.dout
        ReadLen := 0.U
        rdstate := sRdData
      }
    }
    is(sRdData){
      //读数据
      io.RdData.valid := io.SramRd.rvalid
      io.RdData.data := io.SramRd.rdata
      io.RdData.prior := priorRd
      io.SramRd.rready := io.RdData.ready
      FreeAddrManagerInst.io.RdAddr.data := ReadFirstAddr + ReadLen
      io.SramRd.araddr := ReadFirstAddr + ReadLen
      when(io.SramRd.rvalid && io.SramRd.rready){
        ReadLen := ReadLen + 1.U
        //读完了
        FreeAddrManagerInst.io.RdAddr.valid := true.B
        when(ReadLen === DataLen){
          //如果读完了 ，并且分散后的地址个数不为0
          //则继续申请下一个地址
          when(ReadPackNum === PackNumRd){
            //读完了
            rdstate := sRdIdle
          }.otherwise{
            //当前的数据包还有分散的地址，继续读
            PackAddrFifo(priorRd).fifo.fiforead.read := true.B
            PackLenFifo(priorRd).fifo.fiforead.read := true.B
            PackNumRd := PackNumRd + 1.U
            rdstate := sRdwait
          }
        }
      }
    }
  }

}