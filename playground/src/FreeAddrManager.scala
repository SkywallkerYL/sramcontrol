package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/*********
进行一个空闲地址的管理，
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
class FreeAddrManager extends Module with Config {
  val io = IO(new Bundle{
    //Sram 请求分配通道
    val SramReq = Flipped(new AxiStream(SramIdwidth))
    //Sram 释放通道
    val SramRelease = new AxiStream(SramIdwidth)
    //空闲地址输出
    val FreeAddr = new AxiStream(AddrWidth)
    //当前最大能写的长度 其实也可以不要这个。
    val MaxLen = Output(UInt(AddrWidth.W))

    //读地址通道，根据这个对地址进行释放 即记录读出的地址
    val RdAddr = Flipped(new AxiStream(AddrWidth))
    //写地址通道，根据这个对地址进行分配 即记录写入的地址
    val WrAddr = Flipped(new AxiStream(AddrWidth))
  })
    //一些默认的输出
    io.SramReq.ready := false.B
    
    io.SramRelease.valid := false.B
    io.SramRelease.data := 0.U
    io.SramRelease.last := false.B

    io.FreeAddr.valid := false.B
    io.FreeAddr.data := 0.U
    io.FreeAddr.last := false.B

    io.MaxLen := 0.U

    io.RdAddr.ready := true.B

    io.WrAddr.ready := true.B

    //Sramnum 个寄存器，存对应编号的Sram 剩余空间的大小。
    //1KB 最多1024个，10位足够 但是要用11位
    val SramSizeReg = RegInit(VecInit(Seq.fill(Sramnum)(0.U(SramSizeWidth.W))))
    //记录SramSizeReg的大小，对是否还有空间进行判断 有一个还有空间，则表明不用向申请新的。
    val SramRest = Seq.tabulate(Sramnum)(i => SramSizeReg(i) =/= 0.U)
    val RestFlag = SramRest.reduce(_ || _)
    //维护某个Sram是否已经被分配
    val SramFlagReg = RegInit(VecInit(Seq.fill(Sramnum)(false.B)))
    val SramAllocated = Seq.tabulate(Sramnum)(i => SramFlagReg(i) === true.B)
    val SramAllocatedFlag = SramAllocated.reduce(_ || _)
    //记录一个写地址的指针 ，即某一个Sram的写入指针，
    //当写入指针未到达1023时，表明该Sram还有空间 可以继续写入
    //当写入指针到达1023时，表明该Sram已经写满，当然实际可能已经读除了，这个时候把Flag拉高
    //向外送的空闲地址就是空闲fifo中的地址
    val WrAddrReg = RegInit(VecInit(Seq.fill(Sramnum)(1024.U(SramSizeWidth.W))))
    val SramWriteFlag = Seq.tabulate(Sramnum)(i => WrAddrReg(i) === 1024.U)
    val allWrite = SramWriteFlag.reduce(_ && _)
    //释放计时器，当某个Sram的空间达到最大后，计时器开始计时，如果这期间，
    //没有数据写入该Sram，则向外部发送释放该块Sram的信号
    val SramReleaseTimer = RegInit(VecInit(Seq.fill(Sramnum)(0.U(ReleaseTimerWidth.W))))

    //Sram请求分配的控制状态机 和状态分配
    val reqIdle :: reqWait :: reqAlloc :: Nil = Enum(3)
    val reqState = RegInit(reqIdle)
    switch(reqState){
        is(reqIdle){
            //以FreeAddr的ready为触发条件 表明此时需要这边给出一个空闲的地址
            //当没有Sram被分配时，且需要空闲地址时，向Sram管理模块发送请求
            when(io.FreeAddr.ready && !RestFlag){
                reqState := reqWait
                //提前一个周期拉高 ready 
                io.SramReq.ready := true.B
            }
        }
        is(reqWait){
            io.SramReq.ready := true.B
            when(io.SramReq.valid){
                //获取被分配的Sram编号
                val SramId = io.SramReq.data
                for(i <- 0 until Sramnum){
                    when(SramId === i.U){
                        //将被分配的Sram编号的剩余空间大小设置为1024
                        SramSizeReg(i) := 1024.U
                        //将SramFlagReg对应编号的Sram设置为已经分配
                        SramFlagReg(i) := true.B
                        //写指针复位
                        WrAddrReg(i) := 0.U
                        reqState := reqAlloc
                    }
                }
            }   
        }
        is(reqAlloc){
            reqState := reqIdle
        }
    }
    //Sram释放的控制状态机 和状态分配
    val releaseIdle :: releaseWait :: releaseAlloc :: Nil = Enum(3)
    val releaseState = RegInit(releaseIdle)
    //记录当前请求释放的id
    val chooseId = RegInit(0.U(SramIdwidth.W))
    switch(releaseState){
        is(releaseIdle){
            //当某一个Sram 被分配了，并且剩余空间为1024时，开始计时
            //当计时器到达最大值时，向Sram管理模块发送释放信号
            when(SramAllocatedFlag){
                for(i <- 0 until Sramnum){
                    when(SramSizeReg(i) === 1024.U && SramFlagReg(i)){
                        SramReleaseTimer(i) := SramReleaseTimer(i) + 1.U
                        when(SramReleaseTimer(i) === ReleaseTimer.U){
                            chooseId := i.U
                            releaseState := releaseWait
                        }
                    }.otherwise{
                        //没有被分配的Sram 或者 剩余空间不为1024时，计时器复位 
                        SramReleaseTimer(i) := 0.U
                    }
                }
            }
        }
        is(releaseWait){
            io.SramRelease.valid := true.B
            io.SramRelease.data := chooseId
            when(io.SramRelease.ready){
                for(i <- 0 until Sramnum){
                    when(chooseId === i.U){
                        //释放该Sram
                        SramSizeReg(i) := 0.U
                        SramFlagReg(i) := false.B
                        WrAddrReg(i) := 1024.U
                        SramReleaseTimer(i) := 0.U
                        releaseState := releaseAlloc
                    }
                }
            }
        }
        is(releaseAlloc){
            releaseState := releaseIdle
        }
    }
    //两个fifo 进行空闲地址的存储 和该空闲地址对应的最大长度 
    //注意这里地址的位宽是ID 和 1KB的地址拼接的 5 + 10
    //当读地址发送last的时候，即记录了当前地址和初始地址的差值，这就是当前空闲地址对应的长度，存入fifo
    val FreeAddrFifo = Module(new fiforam(MaxfifoNum,AddrWidth))
    val MaxLenFifo = Module(new fiforam(MaxfifoNum,AddrWidth))
    FreeAddrFifo.io.fifo.fifowrite.din := io.WrAddr.data
    FreeAddrFifo.io.fifo.fifowrite.write := false.B 
    FreeAddrFifo.io.fifo.fiforead.read := false.B
    FreeAddrFifo.io.flush := false.B

    MaxLenFifo.io.fifo.fifowrite.din := 0.U
    MaxLenFifo.io.fifo.fifowrite.write := false.B
    MaxLenFifo.io.fifo.fiforead.read := false.B
    MaxLenFifo.io.flush := false.B

    //当申请到分配的Sram时，输出的空闲地址即当前Sram的Id + Sram的写指针。
    //当Sram的写指针到达1024时，输出的空闲地址即从fifo中取出的地址 
    
    //空闲地址的输出 状态 和状态机
    val freeIdle :: freeFirstWrite :: freeAddrFromFifo :: Nil = Enum(3)
    val freeState = RegInit(freeIdle)
    //记录选择的Sram编号 避免这期间如果申请了新的Sram，导致写指针变化
    val chooseSramId = RegInit(0.U(SramIdwidth.W))
    //记录当前真实地址相对首地址的偏移。
    val AddrOffset = RegInit(0.U(AddrWidth.W))
    //记录从fifo里读出的地址
    val FreeAddr = FreeAddrFifo.io.fifo.fiforead.dout
    //记录最大的偏移值，即当前空闲地址对应的最大长度
    val MaxOffset = MaxLenFifo.io.fifo.fiforead.dout
    switch(freeState){
        is(freeIdle){
            //当有分配的Sram还没有写满的时候 分配
            when(SramAllocatedFlag && !allWrite){
                for(i <- 0 until Sramnum){
                    when(SramFlagReg(i) && WrAddrReg(i) =/= 1024.U){
                        chooseSramId := i.U
                        freeState := freeFirstWrite
                    }
                }
            }.elsewhen(SramAllocatedFlag && allWrite && !FreeAddrFifo.io.fifo.fiforead.empty){
                //分配的Sram都写过一遍了，但是空闲地址还有，从空闲地址里读取地址输出。
                freeState := freeAddrFromFifo
                FreeAddrFifo.io.fifo.fiforead.read := true.B
                MaxLenFifo.io.fifo.fiforead.read := true.B
                AddrOffset := 0.U
            }
        }
        is(freeFirstWrite){
            //当写指针到达1024时，输出的空闲地址即从fifo中取出的地址
            io.FreeAddr.valid := true.B
            for(i <- 0 until Sramnum){
                when(chooseSramId === i.U){
                    io.FreeAddr.data := i.U ## WrAddrReg(i)(9,0)
                    io.MaxLen := 1023.U - WrAddrReg(i)(9,0)
                    when(WrAddrReg(i) === 1024.U){
                        io.FreeAddr.valid := false.B 
                        //当前选择的Sram已经写过一边，这个时候有两种情况 
                        //第一是当前Sram有空闲，这个时候进入下一个状态输出空闲地址
                        //第二个是当前Sram没有空闲，这个时候回idle状态
                        when(!FreeAddrFifo.io.fifo.fiforead.empty){ 
                            freeState := freeAddrFromFifo
                            FreeAddrFifo.io.fifo.fiforead.read := true.B
                            MaxLenFifo.io.fifo.fiforead.read := true.B 
                            AddrOffset := 0.U
                        }.otherwise{
                            freeState := freeIdle
                        }
                    }
                }
            }
            
        }
        is(freeAddrFromFifo){
            io.FreeAddr.valid := true.B
            io.FreeAddr.data := FreeAddr + AddrOffset
            io.MaxLen := MaxOffset
            when(io.WrAddr.valid){
                //当前地址外部进行了写入，地址偏移+1
                AddrOffset := AddrOffset + 1.U
                when(AddrOffset === MaxOffset){
                    io.FreeAddr.valid := false.B 
                    AddrOffset := MaxOffset
                    when(!FreeAddrFifo.io.fifo.fiforead.empty){ 
                        freeState := freeAddrFromFifo
                        FreeAddrFifo.io.fifo.fiforead.read := true.B
                        MaxLenFifo.io.fifo.fiforead.read := true.B
                        AddrOffset := 0.U
                    }.otherwise{
                        freeState := freeIdle
                    }
                }
            }
        }
    }
    //读写Sram的空间大小维护 以及空闲地址记录 
    //识别写入id 
    val WrId = io.WrAddr.data(AddrWidth - 1,10)
    //识别读出id
    val RdId = io.RdAddr.data(AddrWidth - 1,10)
    //第一种情况，读写同一个地址
    when(WrId === RdId){
        when(io.WrAddr.valid && io.RdAddr.valid ){
            when(SramFlagReg(WrId)){
                SramSizeReg(WrId) := SramSizeReg(WrId)
                //当写入指针还未达到1024时
                when(WrAddrReg(WrId) =/= 1024.U){
                    WrAddrReg(WrId) := WrAddrReg(WrId) + 1.U
                }
            }
        }.elsewhen(io.WrAddr.valid){
            //写入数据
            when(SramFlagReg(WrId) && SramSizeReg(WrId) =/= 0.U){
                SramSizeReg(WrId) := SramSizeReg(WrId) - 1.U
                when(WrAddrReg(WrId) =/= 1024.U){
                    WrAddrReg(WrId) := WrAddrReg(WrId) + 1.U
                }
            }
        }.elsewhen(io.RdAddr.valid){
            //读出数据
            when(SramFlagReg(RdId)&& SramSizeReg(RdId) =/= 1024.U){
                SramSizeReg(RdId) := SramSizeReg(RdId) + 1.U
            }
        }
    }.otherwise{
        when(io.WrAddr.valid){
            //写入数据
            when(SramFlagReg(WrId) && SramSizeReg(WrId) =/= 0.U){
                SramSizeReg(WrId) := SramSizeReg(WrId) - 1.U
                when(WrAddrReg(WrId) =/= 1024.U){
                    WrAddrReg(WrId) := WrAddrReg(WrId) + 1.U
                }
            }
        }.elsewhen(io.RdAddr.valid){
            //读出数据
            when(SramFlagReg(RdId)&& SramSizeReg(RdId) =/= 1024.U){
                SramSizeReg(RdId) := SramSizeReg(RdId) + 1.U
            }
        }
    }
    //状态机控制 Addr fifo 的写入 
    //状态分配
    val addrIdle :: addrRead :: Nil = Enum(2)
    val addrState = RegInit(addrIdle) 
    //空闲地址首地址记录
    val FreeAddrFirst = RegInit(0.U(AddrWidth.W))
    //长度记录 从0开始 ，比如总共24,则长度为23
    val FreeAddrLen = RegInit(0.U(AddrWidth.W))
    switch(addrState){
        //当外部信号的valid拉高时，写入fifo
        is(addrIdle){
            when(io.RdAddr.valid){
                FreeAddrFirst := io.RdAddr.data
                //设成1是为了记录真正的偏移，如果进入下一个状态
                //RdAddr.last 为true时，如果记0,会少一个
                FreeAddrLen := 1.U
                when(io.RdAddr.last){
                    FreeAddrFifo.io.fifo.fifowrite.din := FreeAddrFirst
                    FreeAddrFifo.io.fifo.fifowrite.write := true.B
                    MaxLenFifo.io.fifo.fifowrite.din := 0.U
                    MaxLenFifo.io.fifo.fifowrite.write := true.B
                    addrState := addrIdle
                }.otherwise{
                    addrState := addrRead
                }
            }
        }
        is(addrRead){
            when(io.RdAddr.valid){
                FreeAddrLen := FreeAddrLen + 1.U
                when(io.RdAddr.last){
                    FreeAddrFifo.io.fifo.fifowrite.din := FreeAddrFirst
                    FreeAddrFifo.io.fifo.fifowrite.write := true.B
                    MaxLenFifo.io.fifo.fifowrite.din := FreeAddrLen
                    MaxLenFifo.io.fifo.fifowrite.write := true.B
                    addrState := addrIdle
                }.otherwise{
                    addrState := addrRead
                }
            }
        }
    }

}