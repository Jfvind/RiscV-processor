import chisel3._
import circt.stage.ChiselStage
import core.Core

// A wrapper module that hides debug signals and only exposes the LED
class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W)) // Matches "io_led" in XDC
  }) // The debug outputs from Core are left unconnected (ignored)


  // --- FREE-RUNNING CLOCK DIVIDER ---
  // We use 'withReset(false.B)' to force this counter to IGNORE the reset button.
  // This ensures the clock keeps ticking even when you hold the Reset button.
  val slowClock = withReset(false.B) {
    val cnt = RegInit(0.U(25.W))
    cnt := cnt + 1.U
    cnt(20).asClock // ~95 Hz (for a visible blink)
  }

  // --- CORE INSTANTIATION ---
  // The Core uses the 'slowClock' but still uses the normal external 'reset'.
  // Now, when you press Reset, the clock is still running, so the Core 
  // will correctly reset PC to 0.
  withClock(slowClock) {
    val core = Module(new Core())
    io.led := core.io.led
  }
}

object Top extends App {
  // Generate Verilog for the 'Top' module
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    args = Array("--target-dir", "generated")
  )
}