import chisel3._
import circt.stage.ChiselStage
import core.Core

// A wrapper module that hides debug signals and only exposes the LED
class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W)) // Matches "io_led" in XDC
  })

  val core = Module(new Core())

  // Connect the Core's LED output to the Top output
  io.led := core.io.led

  // The debug outputs from Core are left unconnected (ignored)
}

object Top extends App {
  // Generate Verilog for the 'Top' module
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    args = Array("--target-dir", "generated")
  )
}