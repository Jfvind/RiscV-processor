package core

import chisel3._
import chisel3.util._

class MemoryMapping extends Module {
  val io = IO(new Bundle {
    // Signals from Core
    val address   = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val memWrite  = Input(Bool())
    
    val readData  = Output(UInt(32.W)) // Read data from RAM
    val led    = Output(UInt(1.W))    // State of LED for FPGA
  })

  // Instantiate real DataMemory (RAM)
  val dataMem = Module(new DataMemory())

  // Define IO-address (LED)
  val mmioBase = 100.U(32.W) // Maybe change to higher address when full implementation is done  !!==!!

  // LED or DataMemory
  val isLed = (io.address === mmioBase)
  
  val isRam = !isLed // If not an IO then it is RAM (DataMemory)

  // --- RAM LOGIK ---
  dataMem.io.address   := io.address
  dataMem.io.writeData := io.writeData
  dataMem.io.memWrite  := io.memWrite && isRam // Only writing to RAM if writing is asked and the address is in RAM
  io.readData := dataMem.io.readData // Data from the address is available as output

  // --- LED LOGIK ---
  val ledReg = RegInit(0.U(1.W))
  when(io.memWrite && isLed) {  // When writing is enabled and address is the LED, the LED value is updated
    ledReg := io.writeData(0)
  }

  io.led := ledReg // Connecting output
}