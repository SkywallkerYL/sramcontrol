package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/******
读出队列控制模块
功能：
1.当外部有读出信号时 根据数据优先级往外部送数据
2.从SG模块获取Data进行ECC的处理
3.从SG模块获取地址与调度模块通信
4.向调度模块发送读请求以及读地址
5.从调度模块获取要读出的数据
//内部模块架构划分

8个优先级对应的ECC存储fifo
8个记录数据包长度的fifo
数据流进来时根据data做ECC 存入ECC的fifo 
然后做一个count指示当前数据包的包长度  
收到包尾信号的时候结束计数，然后把count存入包长度fifo


然后读出数据时，根据ECC fifo的空情况，从优先级由高到底选择对应的fifo进行数据
先给SG模块发送读出数据的请求ready拉高。然后获得地址和长度
然后向仲裁模块申请读，发送地址和长度

拿到数据后从对应的ECC里面读出对应的校验做校验，校验失败直接不发，校验成功就往外送






*******/
class ReadQueueControl extends Module with Config {
  val io = IO(new Bundle{
    val Rd = new ChannelOut(DataWidth)
    val SgData = new DataChannel(DataWidth)
    val SgAddr = new AddrChannel(AddrWidth)
    val ArbiterData = (new DataChannel(DataWidth))
    val ArbiterAddr = Flipped(new AddrChannel(AddrWidth))
  })
  //一些默认的输出
  io.Rd.valid := false.B
  io.Rd.data := 0.U
  io.Rd.sop := false.B
  io.Rd.eop := false.B

  //SG直接转发给arbiter
  io.ArbiterAddr <> io.SgAddr


  //首先实例化8个优先级对应的ECC的Fifo
  val eccFifo = Seq.fill(priornum)(Module(new fiforam(addrwidth,8)))
  //实例化1个ecc的寄存器，位宽为8
  val eccReg = (RegInit(0.U(8.W)))
  //记录当前ecc的位数
  val eccCount = RegInit(0.U(3.W))
  //做奇偶校验的数是输入数据自身异或左移eccCount位
  val eccData = Wire(UInt((8).W)
  eccData := io.SgData.data.xorR << eccCount
  //然后实例化1个记录数据包长度的寄存器
  val lenReg = (RegInit(0.U(lenwidth.W)))
  //然后实例化8个记录数据包长度的fifo
  val lenFifo = Seq.fill(priornum)(Module(new fiforam(addrwidth,lenwidth)))
  //prior 记录当前处理的数据优先级
  val prior = RegInit(0.U(priorwidth.W)) 
  //数据读入部分的控制状态机
  val sIdle :: sData :: Nil = Enum(2)
  //初始化状态机
  val rdstate = RegInit(sIdle)
  switch(rdstate){
    is(sIdle){
      //当SG模块数据Valid拉高时
      when(io.SgData.valid && io.SgData.sop){
        rdstate := sData
        prior := io.SgData.prior
        for(i <- 0 until priornum){
          when(io.SgData.prior === i.U){
            //eccReg存输入数据的奇偶校验 即输入数据的自身所有位异或
            eccReg := eccData | eccReg
            lenReg := 0.U
            eccCount := 1.U 
          }
        }
      }
    }
    is(sData){
      //当输入数据有效时将输入数据的奇偶校验存入eccReg
      when(io.SgData.valid){
        lenReg := lenReg + 1.U 
        eccReg := eccData | eccReg
        eccCount := eccCount + 1.U
        //eccCount = 7时，表示做满了8个数据的奇偶校验，此时将eccReg的数据存入对应的eccFifo
        //并且eccCount清零 然后eccReg清零
        //最后一个数据到来时也要做这个操作
        when(eccCount === 7.U || io.SgData.eop){
          for(i <- 0 until priornum){
            when(prior === i.U){
              eccFifo(i).io.fifowrite.din := eccData | eccReg
              eccFifo(i).io.fifowrite.write := true.B
            }
          }
          eccReg := 0.U
          eccCount := 0.U
          when(io.SgData.eop){
            //数据包长度存入lenFifo
            for(i <- 0 until priornum){
              when(prior === i.U){
                lenFifo(i).io.fifowrite.din := lenReg + 1.U
                lenFifo(i).io.fifowrite.write := true.B
              }
            }
            //跳回Idle状态 等待下一个数据包
            rdstate := sIdle
          }
        }

      }
    }   
  }
  //数据输出的控制状态机   
  //当外部信号的ready拉高时，根据优先级从对应的fifo中读出校验数据 和数据包的长度 
  // i 越小 优先级越高，优先从i=0的fifo中读取数据
  
  //首先获取每个fifo的空状态 
  val eccFifonotEmpty = Wire(Vec(priornum,Bool()))
  for(i <- 0 until priornum){
    eccFifonotEmpty(i) := !eccFifo(i).io.fiforead.empty && !lenFifo(i).io.fiforead.empty
  }
  //eccFifonotEmpty(i)为true时表示第i个fifo不为空
  //将他自己做一个或，确定至少有一个fifo非空
  val eccFifonotEmptyOr = eccFifonotEmpty.reduce(_||_).asBool
  //根据优先级选择输出时的优先级
  //当i小的empty满足时 优先级选择i
  val priorchoose = Wire(UInt(priorwidth.W))

  for(i <- 0 until priornum){
    when(eccFifonotEmpty(i)){
      priorchoose := i.U
    }
  }
  //当前8个Byte的ecc数据
  val eccrData = RegInit(0.U(8.W))
  val checkdata = io.ArbiterData.data
  //做校验 通过异或运算 判断是否校验成功 成功为0  
  val checkparity = (checkdata.xorR ^ eccrData(0)).asBool

  val ByteCount = RegInit(0.U(3.W))
  def ByteAdd(index : UInt) : UInt = {
      Mux(index === (8 - 1).U, 0.U, index + 1.U)
  }
  //当前数据包的长度
  val lenrData = RegInit(0.U(lenwidth.W))

  //统计已经输出的数据
  val lencount = RegInit(0.U(lenwidth.W))
  //eccrData的数据来源于eccFifo,在读信号拉高之后的一个周期更新eccrData

  val eccread = VecInit(Seq.fill(priornum)(Wire(Bool())))
  for(i <- 0 until priornum){
    eccread(i) := eccFifo(i).io.fiforead.read
  } 
  val eccreadOr = eccread.reduce(_||_).asBool
  val eccreadNext = RegNext(eccreadOr)
  val priorNext = RegNext(priorchoose)
  val eccReadlocal = Wire(UInt(8.W))
  for(i <- 0 until priornum){
    when(priorNext === i.U){
      eccReadlocal := eccFifo(i).io.fiforead.dout
    }
  }
  when(eccreadNext){
    for(i <- 0 until priornum){
      when(priorNext === i.U){
        eccrData := eccFifo(i).io.fiforead.dout
      }
    }
  }
  val lenread = VecInit(Seq.fill(priornum)(Wire(Bool())))
  for(i <- 0 until priornum){
    lenread(i) := lenFifo(i).io.fiforead.read
  }
  val lenreadOr = lenread.reduce(_||_).asBool
  val lenreadNext = RegNext(lenreadOr)

  when(lenreadNext){
    for(i <- 0 until priornum){
      when(priorNext === i.U){
        lenrData := lenFifo(i).io.fiforead.dout
      }
    }
  }

  //存地址和读取长度
  val readaddr = RegInit(0.U(AddrWidth.W))
  val readlength = RegInit(0.U(lenwidth.W))

  //统计这个数据包已经接受的数据
  val lencountlocal = RegInit(0.U(lenwidth.W))

  //首先会根据选择的优先级向sg模块发送读请求 获取该数据对应的地址和长度
  //然后向仲裁模块发送读请求 获取数据的数值，并且和eccFifo中的数据做校验
  //校验成功则将数据输出到外部 
  //当数据包的长度为0时，发送eop信号
  //否则仍然向sg模块发送读请求 获取下一个拆分的数据
  //状态命名不能和之前相同
  val sRead :: getDatalen :: sArbiter :: getData ::waitupdate :: Nil = Enum(5)
  val wrstate = RegInit(sRead)
  io.SgAddr.prior := priorNext

  switch(wrstate){
    is(sRead){
      //当外部信号的ready拉高时 并且至少有一个fifo非空时
      io.SgAddr.ready := false.B

      when(io.Rd.ready&&eccFifonotEmptyOr){
        //根据优先级从对应的fifo中读出校验数据 和数据包的长度
        for(i <- 0 until priornum){
          when(priorchoose === i.U){
            //读出eccFifo中的数据
            eccFifo(i).io.fiforead.read := true.B
            //读出lenFifo中的数据
            lenFifo(i).io.fiforead.read := true.B
          }
        }
        //向sg模块发送读请求 获取该数据对应的地址和长度
        io.SgAddr.ready := true.B
        //状态机跳转到仲裁状态
        wrstate := getDatalen
      }
    }
    
    is(getDatalen){
      //当前周期获取了data和len
      lencount := 0.U 
      ByteCount := 0.U 
      wrstate := sArbiter
    }

    is(sArbiter){
      //向SG模块获取读的地址和长度，转发给仲裁模块
      //从SG模块得到的地址和长度，直接传给仲裁模块
      //准备接受地址和长度
      io.SgAddr.ready := io.ArbiterAddr.ready
      io.ArbiterAddr.valid := io.SgAddr.valid
      //当仲裁模块准备号接受数据之后 
      when(io.ArbiterAddr.valid && io.ArbiterAddr.ready ){
        readaddr := io.SgAddr.addr
        readlength := io.SgAddr.length
        //状态机跳转到获取数据状态
        wrstate := getData
        lencountlocal := 0.U 
      }
    }
    //数据直接输出 
    is(getData){
      

      io.Rd.valid := io.ArbiterData.valid
      io.ArbiterAddr.ready := io.Rd.ready
      io.Rd.data := Mux(!checkparity,io.ArbiterData.data,0.U) 
      io.Rd.sop := lencount === 0.U
      io.Rd.eop := lencount === lenrData 
      when(io.ArbiterData.valid && io.Rd.ready){
        ByteCount := ByteAdd(ByteCount)
        eccData := eccData >> 1.U 


        lencount := lencount + 1.U
        lencountlocal := lencountlocal + 1.U
        when(lencount === lenrData){
          wrstate := sRead
        }.elsewhen(lencountlocal === readlength){
          //当前长度的数据发送完成，但是数据包还没有完全发送，
          //跳转下一个态，根据优先级获取下一个数据包的地址和长度
          wrstate := sArbiter
        }.elsewhen(ByteCount === 7.U){
          //8个Byte校验完成，读取下一个eccData
          //自动拉高eccFifo的读信号
          for(i <- 0 until priornum){
            when(priorNext === i.U){
              eccFifo(i).io.fiforead.read := true.B
            }
          }
          //空一个状态出来等待eccFifo的数据
          wrstate := waitupdate
        }
      }
    }
    is(waitupdate){
      //等待eccFifo的数据
      wrstate := getData
    }
  }



}
/******
实现一个CRC校验模块，用于对数据进行校验
根据数据的优先级存入对应的fifo
******/



/******
实现一个CRC校验模块，用于对数据进行校验
输入是一个8bit数据
输出是一个1bit的校验码
*******/

