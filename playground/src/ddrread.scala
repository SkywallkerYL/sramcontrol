package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

/******
完成一组起始地址位 Addr coef，
长度为L的系数与起始地址位 Addr data
M 脉冲、N距离门、K通道数据读取，
数据均为 32bit 浮点表示夏数，虚部高 32bit。
Addr_coef L Addr data、M 、N、K 
以及通道偏移地址 chann shift addr: 
脉冲偏移地址 Pulse shift addr 均为动态可配置参数

DDR 读出来的数据先写入fifo
然后再通过fifo往外面送

//理解通道偏移地址
K通道 M脉冲  
读的时候是
Addr_data + Pulse*M + K * Chann

*******/
class DDrReadModule extends Module with Config {
  val io = IO(new Bundle{
    val conFig = Flipped(new AxiStream) 
    val coef   = new AxiStream
    val ddr    = (new AxiRead)
    val dout   = ((new AxiStream))
    val finish = Output(Bool())
  })
  //default out 
  io.conFig.ready := false.B  
  io.coef.valid := false.B 
  io.coef.data := 0.U 
  io.coef.last := 0.U 

  io.ddr.arvalid := false.B 
  io.ddr.arlen := 0.U 
  io.ddr.araddr := 0.U 
  io.ddr.rready := 0.U 
  
  io.dout.valid := false.B 
  io.dout.data := 0.U 
  io.dout.last := false.B 
  io.finish := false.B 
  //先通过config设置Addr_coef 和长度L 
  //然后读取起始地址， M N K Chann_shift_addr Pulse_shift_addr
  val AddrCoef = RegInit(0.U(AXIADDRWIDTH.W))
  val L        = RegInit(0.U((AXIDATAWIDTH-AXIADDRWIDTH).W))
  val Lcount   = RegInit(0.U((AXIDATAWIDTH-AXIADDRWIDTH).W))
  val AddrData = RegInit(0.U(AXIADDRWIDTH.W))
  val M        = RegInit(0.U(PulseWidth.W))
  val N        = RegInit(0.U(PulseWidth.W))
  //最大256   
  //N一般比较大   就是比如8192   
  //那么N > 256的时候 就把那个Arlen设定成64   
  //那么就拆成了N/64 N>>6次进行读
  //DDR的一个地址存8b ,一次读的256b 
  //那么就要一次读加的地址偏移就是32 
  //完成一个arlen的地址偏移就是32*64  = 2048
  val Arlen    = RegInit(0.U(8.W))
  val K        = RegInit(0.U(PulseWidth.W))

  val ChannelShiftAddr = RegInit(0.U(AXIADDRWIDTH.W)) 
  val PulseShiftAddr = RegInit(0.U(AXIADDRWIDTH.W))
  val DoorShiftAddr = RegInit(0.U(AXIADDRWIDTH.W))

  val MCount = RegInit(0.U(PulseWidth.W))
  val NCount = RegInit(0.U(PulseWidth.W))
  val Ntime = RegInit(0.U(PulseWidth.W))
  val NtimeCount = RegInit(0.U(PulseWidth.W))
  val KCount = RegInit(0.U(PulseWidth.W))
  val finish = RegInit(false.B )
//记录当前通道的地址
  val KAddr = RegInit(0.U(AXIADDRWIDTH.W))
//记录当前脉冲地址的累加
  val MAddr = RegInit(0.U(AXIADDRWIDTH.W))
//读数据的地址为  
  val readaddr = AddrData + KAddr + MAddr + DoorShiftAddr

//存读出数据的fifo 
  //val fifodata = Module(new fifoinline)
  val fifodata = Seq.fill(4)(Module(new fifoinst(128,64)))
  val fiforden = Wire(Bool())
  fiforden := false.B 
  val fifowren = Wire(Bool())
  fifowren := false.B 
  val fifosel  = RegInit(0.U(2.W)) 
  val fifooutlocal = Wire(UInt(64.W))
  fifooutlocal := 0.U 
  for( i <- 0 until 4){
    fifodata(i).io.fifo.clk := clock 
    fifodata(i).io.fifo.rst := reset.asBool
    fifodata(i).io.fifo.din := io.ddr.rdata( (i+1)*64-1,i*64)
    fifodata(i).io.fifo.wr_en := fifowren 
    fifodata(i).io.fifo.rd_en := fiforden
    when(fifosel === i.U ){
      //fifodata(i).io.fifo.rd_en := fiforden
      fifooutlocal := fifodata(i).io.fifo.dout
    }
  }
  
  
//控制读系数和读数据的DDR
  val idle :: getCoefar :: getCoefr :: readDataar :: readDatar :: Nil = Enum(5)
  val ddrstate = RegInit(idle) 
  switch(ddrstate){
    is(idle){
      io.conFig.ready := false.B 
      when(io.conFig.valid){
        AddrCoef := io.conFig.data(AXIADDRWIDTH-1,0)
        L := io.conFig.data(DataWidthIn*2-1,AXIADDRWIDTH)
        ddrstate := getCoefar
      }
    }
    is(getCoefar){
      io.ddr.arvalid := true.B  
      io.ddr.araddr := AddrCoef 
      io.ddr.arlen  := L 
      when(io.ddr.arready){
        ddrstate := getCoefr 
        Lcount := 0.U 
      }
    }
    is(getCoefr){
      io.ddr.rready := true.B  
      when(io.ddr.rvalid){
        switch(Lcount){
          is(0.U){
            AddrData := io.ddr.rdata
          }
          is(1.U){
            M  := io.ddr.rdata
          }
          is(2.U){
            N  := io.ddr.rdata
          }
          is(3.U){
            K  := io.ddr.rdata
          }
          is(4.U){
            ChannelShiftAddr  := io.ddr.rdata
            KAddr := 0.U
          }
          is(5.U){
            PulseShiftAddr  := io.ddr.rdata
            MAddr := 0.U
          }
        }
        Lcount := Lcount + 1.U 
      }
      when(io.ddr.rlast){
        ddrstate := readDataar
        finish := false.B 
        MCount   := 0.U 
        NCount   := 0.U 
        KCount   := 0.U
        NtimeCount := 0.U
        DoorShiftAddr := 0.U  
        when(N > 256.U){
          Ntime := N>>6.U 
          Arlen := 64.U 
        }.otherwise{
          Ntime := 1.U 
          Arlen := N 
        }
      }
    } 
    is(readDataar){
      //读K通道 M脉冲数据   N门数理解为arlen
      io.ddr.araddr := readaddr 
      io.ddr.arlen := Arlen 
      io.ddr.arvalid := true.B 
      when(io.ddr.arready){
        ddrstate := readDatar
      }
    }
    is(readDatar){
      io.ddr.rready := !fifodata(0).io.fifo.full
      when(io.ddr.rvalid && io.ddr.rready){
        //fifodata.io.fifo.din := io.ddr.rdata
        fifowren := true.B 
        DoorShiftAddr := DoorShiftAddr + 32.U 
      }
      when(io.ddr.rlast && io.ddr.rready){
        ddrstate := readDataar
        //收集到该次所有门
        when(NtimeCount  === Ntime-1.U){
          NtimeCount := 0.U
          DoorShiftAddr := 0.U 
          when(MCount === M-1.U){
            MCount := 0.U 
            MAddr := 0.U
            when(KCount === K-1.U){
              KAddr := 0.U
              KCount := 0.U 
              ddrstate := idle
              finish := true.B 
            }.otherwise{

              KAddr := KAddr + ChannelShiftAddr
              KCount := KCount + 1.U 
            }
          }.otherwise{
            MAddr := MAddr + PulseShiftAddr
            MCount := MCount + 1.U 
          }
        }.otherwise{
          //当前的arlen读完了  返回ar态进行下一次的读取
          NtimeCount := NtimeCount + 1.U
          ddrstate := readDataar
        }
      }
    }
  }
  //控制输出通道的状态机
  val fifooutvalid = RegInit(false.B)
  val fifoout =  RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
  when(fifooutvalid){
    for(i <- 0 until 4){
      fifoout(i) := fifodata(i).io.fifo.dout
    }
  }
  val idle0 :: read1  :: Nil =  Enum(2)
  val ddroutstate = RegInit(idle0)
  switch(ddroutstate){
    is(idle0){
      when(!fifodata(0).io.fifo.empty){
        ddroutstate := read1
        fiforden := true.B 
        fifooutvalid := true.B 
        fifosel := 0.U 
      }
    }
    is(read1){
      io.dout.valid := true.B 
      fifooutvalid := false.B 
      when(io.dout.ready){
        //依次输出4组 64位的数据
        when(fifosel === 3.U) {
          when(NCount ===  N-1.U ){
            NCount := 0.U 
            io.dout.last := true.B 
          }.otherwise{
            NCount := NCount + 1.U 
          }
          when(!fifodata(0).io.fifo.empty){
            //ddroutstate := read1
            fiforden := true.B 
            fifooutvalid := true.B 
          }.otherwise{
            ddroutstate := idle0
          }
        }
        fifosel := fifosel + 1.U 
        //ddroutstate := idle0
        
      }
      for(i <- 0 until 4){
        when(i.U === fifosel){
          io.dout.data  := Mux(fifooutvalid,fifodata(i).io.fifo.dout,fifoout(i))
        }
        
      }
      
      when(finish && fifodata(0).io.fifo.empty){
        io.finish := true.B 
      }

    }
    
  }
  //控制系数输出的状态机
  val idle1 :: read :: Nil = Enum(2)
  val coefstate = RegInit(idle1)
  switch(coefstate){
    is(idle1){
      when(finish){
        Lcount := 0.U 
        coefstate := read 
      }
    }
    is(read){
      io.coef.valid := true.B 
      switch(Lcount){
        is(0.U){
          io.coef.data := AddrData 
        }
        is(1.U){
          io.coef.data := M 
        }
        is(2.U){
          io.coef.data := N 
        }
        is(3.U){
          io.coef.data := K 
        }
        is(4.U){
          io.coef.data := ChannelShiftAddr  
        }
        is(5.U){
          
          io.coef.data := PulseShiftAddr  
        }
      }
      when(Lcount === 5.U) {
        io.coef.last := true.B 
        coefstate := idle1
      }
      when(io.coef.ready){
        Lcount := Lcount + 1.U 
      }
    }
  }
  
}

class DDrsimTop extends Module with Config {
  val io = IO(new Bundle {
    // read pix 
    // write bit 
    //input 
    val conFig = Flipped(new AxiStream) 
    val coef   = new AxiStream
    val ddr    = ((new AxiRead))
    val dout   = ((new AxiStream256))
    val finish = Output(Bool()) 
  })
  val ddr = Module(new DDrReadModule)
  val ram = Module(new  ramtop)
  ddr.io.ddr <> ram.io.axi 
  io.ddr.rready  := false.B 
  io.ddr.araddr  := 0.U 
  io.ddr.arvalid := false.B 
  io.ddr.arlen   := 0.U 
  when(io.ddr.rvalid){
    ddr.io.ddr <> io.ddr
    ram.io.axi.arvalid := false.B 
    ram.io.axi.rready := false.B 
  }
  ddr.io.conFig <> io.conFig
  ddr.io.coef <> io.coef
  io.dout <> ddr.io.dout
  io.finish := ddr.io.finish
  //io.econtrol.finish :=  jls.io.econtrol.finish
}


