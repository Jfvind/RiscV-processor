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
  val core = Module(new Core(Programs.uartPipelineTest)) // DONT DELETE: Programs.??? controls what program is syntezised to FPGA
  io.led := core.io.led

  // --- UART INSTANTIATION ---
  val serialPort = Module(new Serialport())
  io.tx := serialPort.io.tx

  // --- HELPER: 4-bit Hex to ASCII ---
  def nibbleToChar(nibble: UInt): UInt = {
    Mux(nibble < 10.U, nibble + 48.U, nibble + 55.U)
  }

  // --- INPUTS FROM CORE ---
  val dataReg = RegInit(0.U(32.W))
  val addrReg = RegInit(0.U(32.W))
  val triggerReg = RegInit(false.B)

  when (core.io.uartValid) {
    dataReg := core.io.uartData
    addrReg := core.io.uartAddr
    triggerReg := true.B // Set trigger for next cycle (Delays data by one cycle so dataReg is updated)
  } .otherwise {
    triggerReg := false.B //Clear after one cycle
  }

  val data = dataReg
  val addr = addrReg

  // --- CALCULATE REGISTER INDEX ---
  // Address 200 -> Index 0. Address 204 -> Index 1.
  // Formula: (Address - 200) / 4
  val regIndex = (addr - 200.U) >> 2

  // Split Index into Two Decimal Digits (e.g., 31 -> '3', '1')
  // Since we only go up to 31, we can use simple logic
  val tens = regIndex / 10.U
  val ones = regIndex % 10.U

  // --- CONSTRUCT STRING ---
  // Format: "x00: 00000000\r\n"
  val asciiVec = Wire(Vec(32, UInt(8.W)))

  asciiVec(0) := 'x'.U
  asciiVec(1) := nibbleToChar(tens) // Reusing nibbleToChar works for 0-3
  asciiVec(2) := nibbleToChar(ones) // Reusing nibbleToChar works for 0-9
  asciiVec(3) := ':'.U
  asciiVec(4) := ' '.U

  // Standard Hex Data (8 chars)
  asciiVec(5)  := nibbleToChar(data(31, 28))
  asciiVec(6)  := nibbleToChar(data(27, 24))
  asciiVec(7)  := nibbleToChar(data(23, 20))
  asciiVec(8)  := nibbleToChar(data(19, 16))
  asciiVec(9)  := nibbleToChar(data(15, 12))
  asciiVec(10) := nibbleToChar(data(11, 8))
  asciiVec(11) := nibbleToChar(data(7, 4))
  asciiVec(12) := nibbleToChar(data(3, 0))

  asciiVec(13) := '\r'.U
  asciiVec(14) := '\n'.U
  
  // Padding
  for (i <- 15 until 32) { asciiVec(i) := 0.U }

  serialPort.io.inputString := asciiVec
  serialPort.io.sendTrigger := triggerReg
}

object Top extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top(),
    Array("--target-dir", "generated")
  )
}