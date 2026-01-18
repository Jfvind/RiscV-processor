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
    val led       = Output(UInt(1.W))  // State of LED for FPGA

    // UART debug
    val uartData  = Output(UInt(32.W))
    val uartAddr  = Output(UInt(32.W))

    // Uart prod
    val uartValid = Output(Bool())
  })

  // Instantiate real DataMemory (RAM)
  val dataMem = Module(new DataMemory())

  // --- ADDRESS MAPPING ---
  val isLed = (io.address === 100.U)
  val isUart = (io.address >= 200.U) && (io.address < 400.U) // x00 = 200, x01 = 204, x31 = 324
  val isRam = !isLed && !isUart // If not an IO then it is RAM (DataMemory)

  // --- RAM LOGIC ---
  dataMem.io.address   := io.address
  dataMem.io.writeData := io.writeData
  dataMem.io.memWrite  := io.memWrite && isRam // Only writing to RAM if writing is asked and the address is in RAM
  io.readData := dataMem.io.readData // Data from the address is available as output

  // --- LED LOGIC ---
  val ledReg = RegInit(0.U(1.W))
  when(io.memWrite && isLed) {  // When writing is enabled and address is the LED, the LED value is updated
    ledReg := io.writeData(0)
  }

  io.led := ledReg // Connecting output

  // --- UART LOGIC ---
  io.uartValid := io.memWrite && isUart
  io.uartAddr  := io.address
  io.uartData  := io.writeData
}