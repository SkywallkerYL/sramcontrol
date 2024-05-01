// package FFT

// import chisel3._
// import chisel3.util._
// import chisel3.experimental._
// /*********
// 内存管理单元

// 功能：

// ScatterCollecter发送写数据请求，Mmu接受写数据请求，将数据写入SRAM中

// 同时进行一个内存的管理， 
// 接受外部的写数据请求，外部只会发一个写数据请求和写的长度
// 然后根据写的长度，将数据写入SRAM中，同时将写入的地址返回给ScatterCollecte

// 注意这里要实现一个内部的地址管理，对该MMU负责的2KB内存进行管理

// 数据是2KB，一个地址对应1个B 
// 所以地址是11位 

// 每次处理外部发来的数据，记录当前数据的首地址

// 默认发出去的Addrvalid 一直拉高
// 直到接收到ArbiterAddr ready请求，表明上边模块对当前数据的拆包
// 拆完了

// MMU进行一个内存的管理 
// 记录每一个地址是否是dirty的。
// dirty表明这个地址已经被写过了。用一个Ram来存

// 然后呢，每次写数据的时候，根据当前的地址，将对应的dirty位设置为1

// 然后呢，每次读数据的时候，根据当前的地址，将对应的dirty位设置为0

// 这样就可以实现一个内存的管理了

// 由此一个数据包可能不会写入连续的地址，因此要管理一张表。

// 他由3个fifo组成

// 一个是当前数据的写入首地址，即传给数据分散模块的地址
// 然后是由于数据可能不连续，所以要记录每个地址和对应的长度。以及分散地址的个数。
// 当然这个是每个优先级都有一个这样的表 因为读出的时候是根据优先级读的

// 比如Addr起始 是3 写入了64个数据，这个能写的最大长度由空闲地址管理模块决定
// 0-21 从3开始写
// 22-55 从60开始写
// 56-63 从134开始写
// 那么就要记录3个地址，和3个长度 以及当前分散了3个地址。

// //实现一个Sram分配模块 最小分配单元是一块Sram，即1KB 

// 当某个通道有写入请求时，首先向该模块发送一个请求分配。然后该模块将空闲的Sram分配给该通道

// 当某个通道的数据全部读出后，向该模块发送一个释放请求，将该Sram释放。

// //
// 还要有一个模块，进行一个空闲地址的管理，
// 该模块首先向Sram管理模块发送一个请求，获取一个分配的Sram编号 0-31

// 每一个MMU设置32个寄存器
// 存对应编号的Sram 剩余空间的大小。
// 为分配之前，大小是0,分配后，大小是1KB 数值即为1KB/1B = 1024  //1023

// 每次写入数据时，寄存器的数值-1,
// 每次读出数据时，寄存器的数值+1，
// 对寄存器的值进行动态管理 。
// 注意 如果同时有写入和读出，需要保证读写地址是对于同一个id的Sram。

// 用一个Flag来记录空闲情况
// 当读出数据时，把数据读出后的地址 和长度 传给该模块。存如两个fifo中。

// 当写地址的指针首次到达1023时，表明该Sram已经写满，当然实际上前面
// 可能已经有数据读出，空间有空闲了。这个时候就往外发空闲fifo中的地址。
// 这个时候把Flag拉高，表明该Sram已经写过一边。

// 当Flag为低的时候，给MMU返回的地址就是写入指针，不能是1024-剩余空间的大小
// 因为这个时候可能有数据读出，空间有空闲了，写入指针不一定等于1024-剩余空间的大小
// 最大的长度即1024-写入指针
// 当Flag为高的时候，给MMU返回的地址就是空闲fifo中的地址。和空闲fifo的长度。

// 如果没有读出，那么就向Sram管理模块申请。
// 注意这里状态不能卡住，比如Sram管理模块一直不给分配，
// 当又有数据读出的时候，即模块又空闲了，就不用申请，继续写入数据。

// *********/
// class Mmu extends Module with Config {
//   val io = IO(new Bundle{
//     //ScatterCollecter写数据请求
//     val WrData = (new DataChannel(DataWidth))
//     //ScatterCollecter写数据返回地址通道
//     val WrAddr = Flipped(new AddrChannel(AddrWidth))
//     //ScatterCollecter 读数据通道
//     val RdData = Flipped(new DataChannel(DataWidth))
//     //ScatterCollecter 读数据请求地址通道
//     val RdAddr = (new AddrChannel(AddrWidth))

//     //SramControl写数据数据通道
//     val SramWr  = (new AxiWrite)
//     //SramControl读数据数据通道
//     val SramRd  = (new AxiRead)
//   })
//   io.datafiforead.foreach(_.read := false.B)
//   io.lenfiforead.foreach(_.read := false.B)
//   io.ArbiterData.valid := false.B
//   io.ArbiterData.data := 0.U
//   io.ArbiterData.sop := false.B
//   io.ArbiterData.eop := false.B
//   io.ArbiterData.prior := 0.U
//   io.ArbiterAddr.ready := false.B
//   io.unpackedNumFifoWrite.foreach(_.write := false.B)
//   io.unpackedLenFifoWrite.foreach(_.write := false.B)
//   io.unpackedAddrFifoWrite.foreach(_.write := false.B)
//   io.unpackedNumFifoWrite.foreach(_.din := 0.U)
//   io.unpackedLenFifoWrite.foreach(_.din := 0.U)
//   io.unpackedAddrFifoWrite.foreach(_.din := 0.U)
//   //CRC 模块
//   val crc = Module(new CrcModel)
//   crc.io.crcen := false.B
//   crc.io.data := 0.U
//   crc.io.rst := false.B
  
//   val crcCount = RegInit(0.U(2.W))
//   val crcData = MuxLookup(crcCount,0.U,Array(
//     0.U -> crc.io.crc(31,24),
//     1.U -> crc.io.crc(23,16),
//     2.U -> crc.io.crc(15,8),
//     3.U -> crc.io.crc(7,0)
//   ))

//   //统计fifo的空情况
//   val fifo_empty = Wire(Vec(priornum,Bool()))
//   for(i <- 0 until priornum){
//     fifo_empty(i) := io.datafiforead(i).empty
//   }
//   //总的fifo的空情况
//   val fifo_empty_all = fifo_empty.reduce(_ && _)
//   //当优先级更高的fifo不空时，确认prior的值
//   //这里其实是一个输出调度的情况，后边可以优化，就是当制定一种策略，
//   //输入是fifo的空情况，输出是一个优先级的选择
//   val priorMux = Wire(UInt(priorwidth.W))
//   priorMux := 0.U
//   for(i <- 0 until priornum){
//     when(!fifo_empty(i)){
//       priorMux := i.U
//     }
//   }
//   //记录选择的优先级,防止中途改变
//   val prior = RegInit(0.U(priorwidth.W)) 
//   //当前处理的数据 
//   val datain = MuxLookup(prior, io.datafiforead(0).dout, 
//   (0 until priornum).map(i => (i.U -> io.datafiforead(i).dout)))
//   val lenin = MuxLookup(prior, io.lenfiforead(0).dout, 
//   (0 until priornum).map(i => (i.U -> io.lenfiforead(i).dout)))
//   //当前数据总的长度
//   val DataLen = RegInit(0.U(lenwidth.W))
//   //记录数据的一个DataLen,统计拆包后的数据包长度//真实的DataLen为DataLen+1
//   val unpackDataLen = RegInit(0.U(lenwidth.W))
//   //记录拆包后的数据包个数
//   val unpackDataNum = RegInit(0.U(lenwidth.W))
  
//   //主状态机
//   val sIdle :: sCrc :: sData :: sUpdate :: Nil = Enum(4)
//   val state = RegInit(sIdle)
//   switch(state){
//   	is(sIdle){
//       //当有数据包长度fifo不空时，读取数据包长度
//       when(!fifo_empty_all){
//         state := sData
//         prior := priorMux
// 	      DataLen := 0.U
//         //读取数据包长度 和 数据
//         io.datafiforead.zipWithIndex.foreach { case (fifo, i) =>
//           when(priorMux === i.U) {
//             fifo.read := true.B
//           }
//         }
//         io.lenfiforead.zipWithIndex.foreach { case (fifo, i) =>
//           when(priorMux === i.U) {
//             fifo.read := true.B
//           }
//         }
//         unpackDataLen := 0.U
//         unpackDataNum := 0.U
//         //注意fifo的模型要保持数据 
//         //就是读取一次，数据还在，直到下一次读取
//         //crc 复位
//         crc.io.rst := true.B
//       }
//     }
//     is(sCrc){
//       //拿到了数据 ，一边计算CRC，一边发送数据 
//       io.ArbiterData.valid := true.B
//       io.ArbiterData.data := datain 
//       crc.io.data := datain
      
//       when(io.ArbiterData.ready){
//         io.datafiforead.zipWithIndex.foreach { case (fifo, i) =>
//           when(prior === i.U) {
//             fifo.read := true.B
//           }
//         }
//         DataLen := DataLen + 1.U
//         unpackDataLen := unpackDataLen + 1.U
//         crc.io.crcen := true.B 
//         //当达到最大crc长度 或者 datalen 达到总的长度时，结束crc的输入
//         when(DataLen === lenin || unpackDataLen === maxcrcnum.U-1.U){
//           io.datafiforead.zipWithIndex.foreach { case (fifo, i) =>
//             when(prior === i.U) {
//               fifo.read := false.B
//             }
//           }
//           state := sData
//           crcCount := 0.U 
//           unpackDataNum := unpackDataNum + 1.U
//         }
//       }
//     }
//     is(sData){
//       io.ArbiterData.valid := true.B
//       io.ArbiterData.data := crcData
      
//       when(io.ArbiterData.ready){
//         unpackDataLen := unpackDataLen + 1.U
//         crcCount := crcCount + 1.U
//         //当达到最大crc长度 或者 datalen 达到总的长度时，结束crc的输入
//         when(crcCount === 3.U){
//           io.ArbiterAddr.ready := true.B
//           //这里默认这个addr一直拉高，不考虑其不拉高的情况。
//           //要考虑，因为这个addr是从仲裁模块返回的，可能会有延迟
//           //也可以不考虑，因为仲裁模块把data的ready拉高了
//           when(io.ArbiterAddr.valid){
//             //写入当前拆包的数据的地址 和数据包长度
//             io.unpackedLenFifoWrite.zipWithIndex.foreach { case (fifo, i) =>
//               when(prior === i.U) {
//                 fifo.write := true.B
//                 fifo.din := unpackDataLen
//               }
//             }
//             io.unpackedAddrFifoWrite.zipWithIndex.foreach { case (fifo, i) =>
//               when(prior === i.U) {
//                 fifo.write := true.B
//                 fifo.din := io.ArbiterAddr.addr
//               }
//             }
//             when(DataLen =/= lenin){
//               state := sCrc
//               unpackDataLen := 0.U 
//               io.datafiforead.zipWithIndex.foreach { case (fifo, i) =>
//                 when(prior === i.U) {
//                   fifo.read := true.B
//                 }
//               }
//             }.otherwise{
//               state := sUpdate
//             }
//           }.otherwise{
//             io.ArbiterData.valid := false.B 
//             crcCount := crcCount
//             unpackDataLen := unpackDataLen
//           }
//         }
//       }
//     }
// 		//向外部输出DataLen和prior
// 		is(sUpdate){
//       //整个数据包拆分完成，写入数据包个数
//       io.unpackedNumFifoWrite.zipWithIndex.foreach { case (fifo, i) =>
//         when(prior === i.U) {
//           fifo.write := true.B
//           fifo.din := unpackDataNum
//         }
//       }
//       state := sIdle
// 		}
//   }
// }