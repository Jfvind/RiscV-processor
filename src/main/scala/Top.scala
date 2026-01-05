import chisel3._
import circt.stage.ChiselStage

object Top extends App {
  // This generates the Verilog file "Core.sv" (SystemVerilog)
  ChiselStage.emitSystemVerilogFile(
    new core.Core(),
    args = Array("--target-dir", "generated")
  )
}