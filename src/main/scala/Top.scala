/*import chisel3._
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
  // Bit 1 (counter(1)) skifter hver 4. cyklus -> 100 MHz / 2 = 50 MHz
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
  /*
  // --- CORE INSTANTIATION ---
  // Otherwise we use a filepath: e.g. when running the prime_benchmark
  //val core = Module(new Core(programFile = "prime_bench.mem"))
  val core = Module(new Core(programFile = "prime_bench.mem"))
  // --- LED INSTANTIATION/CONNECTION ---
  io.led := core.io.led

  // --- UART INSTANTIATION ---
  io.tx := core.io.tx*/
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    Array("--target-dir", "generated")
  )
}*/

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage
import core._

/**
  * 1. BlackBox Definition
  * Fortæller Chisel, at der findes et eksternt Verilog-modul kaldet 'ClockWizard'.
  * Navnene på portene skal matche præcis med dem, du vælger i Vivado IP Catalog.
  */
class ClockWizard extends BlackBox {
  val io = IO(new Bundle {
    val clk_in1  = Input(Clock())  // 100 MHz input (W5 pin)
    val clk_out1 = Output(Clock()) // 25 MHz output til CPU
    val reset    = Input(Bool())   // Reset knap
    val locked   = Output(Bool())  // Status signal (valgfrit)
  })
}

// A wrapper module that hides debug signals and only exposes the LED and TX
class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W)) // Mapper til "io_led" i XDC
    val tx  = Output(Bool())    // Mapper til "io_tx" i XDC
  })

  // --- CLOCK WIZARD INSTANTIATION ---
  // Dette erstatter tæller-divideren og giver et stabilt signal uden slack-fejl
  val clkWiz = Module(new ClockWizard)
  clkWiz.io.clk_in1 := clock         // Forbinder boardets 100MHz klok
  clkWiz.io.reset   := reset.asBool  // Forbinder til reset-knappen

  // --- CORE INSTANTIATION ---
  // Vi bruger 'withClock' så Core kører på 25 MHz udgangen fra Clock Wizard
  withClock(clkWiz.io.clk_out1) {
    // Sørg for at filnavnet her matcher din rettede .mem fil (0x8000 stak)
    val core = Module(new Core(programFile = "prime_bench.mem"))
    
    // Forbindelser fra Core til Top-niveauets IO
    io.led := core.io.led
    io.tx  := core.io.tx 
  }
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    Array("--target-dir", "generated")
  )
}