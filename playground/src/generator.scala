package FFT
import chisel3._
import circt.stage._
import chisel3.stage.ChiselStage

object generator extends App with Config {
  //(new ChiselStage).emitVerilog(new FFTtop,Array("--target-dir",s"generated/${FFTlength}Point_${FFTparallel}parallel_${use_float}float_${DataWidth-1}width/"))
  (new ChiselStage).emitVerilog(new Asynfifo(2,16),Array("--target-dir",s"build/"))
  // DDrReadModule MatrixInvfullTop DDrsimTop
  //(new ChiselStage).emitVerilog(new Switch(1, MyFixComplex))
  //val data2 = (VecInit(Seq.fill(radix)(0.S((2 * DataWidth).W).asTypeOf(MyComplex))))
  //def top = new FFTtop

  //val generator1 = Seq(
    //chisel3.stage.ChiselGeneratorAnnotation(() => top),
    //firrtl.stage.RunFirrtlTransformAnnotation(new AddModulePrefix()),
    //ModulePrefixAnnotation("ysyx_22050550_")
  //)
  //(new chisel3.stage.ChiselStage).execute(args, generator1) 
}
object Elaborate extends App with Config {
  // DataInProcess ScaterCore DataCollector DataScater 
  //SramManagerModel SramControlModel PrioritySelectModel
  def top = new PrioritySelectModel
  val useMFC = false // use MLIR-based firrtl compiler
  val generator = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top),
    //firrtl.stage.RunFirrtlTransformAnnotation(new AddModulePrefix()),
    //ModulePrefixAnnotation("ysyx_22050550_")
  )
  if (useMFC) {
    (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
  } else{
    (new chisel3.stage.ChiselStage).execute(args, generator)
  }
}