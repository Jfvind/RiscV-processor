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
  
  // --- CLOCK DIVIDER (Fixer -8ns slack problemet) ---
  // Vi tæller op: 00 -> 01 -> 10 -> 11 ...
  // Bit 1 (counter(1)) skifter hver 4. cyklus -> 100 MHz / 4 = 25 MHz
  val counter = RegInit(0.U(2.W))
  counter := counter + 1.U
  
  val slowClock = counter(1).asClock

  // --- CORE INSTANTIATION ---
  // Vi pakker Core ind i 'withClock', så den kører på 25 MHz
  withClock(slowClock) {
    val core = Module(new Core(programFile = "prime_bench.mem"))
    
    // Forbindelser
    io.tx := core.io.tx
    io.led := core.io.led
  }
}

/*
  // --- CORE INSTANTIATION ---
  // Otherwise we use a filepath: e.g. when running the prime_benchmark
  val core = Module(new Core(programFile = "prime_bench.mem"))
  
  // --- LED INSTANTIATION/CONNECTION ---
  io.led := core.io.led

  // --- UART INSTANTIATION ---
  io.tx := core.io.tx
}*/

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    Array("--target-dir", "generated")
  )
}