import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage
import core._

// A wrapper module that hides debug signals and only exposes the LED
class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W)) // Matches "io_led" in XDC
    val tx  = Output(Bool())
  }) // The debug outputs from Core are left unconnected (ignored)

  // --- CORE INSTANTIATION ---
  // Otherwise we use a filepath: e.g. when running the prime_benchmark
  val core = Module(new Core(programFile = "prime_bench.mem"))
  // --- LED INSTANTIATION/CONNECTION ---
  io.led := core.io.led

  // --- UART INSTANTIATION ---
  io.tx := core.io.tx
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    Array("--target-dir", "generated")
  )
}