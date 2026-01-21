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

    // Signlas for instructionFetch
    val imemWriteEn = Output(Bool())
    val imemWriteAddr = Output(UInt(32.W))
  })

  // Instantiate real DataMemory (RAM)
  val dataMem = Module(new DataMemory())

  // --- ADDRESS MAPPING ---
  // We define everything form 0x8000 and up is for Instruction-mem
  val isImemWrite = (io.address >= 0x8000.U)

  val isLed = (io.address === 100.U)
  
  // Fra main: Specifikke adresser til UART (meget mere stabilt end et range)
  val isUartData   = (io.address === 0x1000.U)
  val isUartStatus = (io.address === 0x1004.U)
  val isUart       = isUartData || isUartStatus

  // Fra feature/unified-memory: Find ud af om vi skriver til I-mem (0x8000 og op)
  val isImemWrite  = (io.address >= 0x8000.U)

  // --- RAM LOGIC ---
  // Vi skriver kun til DataMemory (LUT-RAM), hvis det IKKE er LED, UART eller I-MEM
  val isRam = !isLed && !isUart && !isImemWrite

  // --- I-MEM LOGIC (Unified Memory) ---
  io.imemWriteEn   := io.memWrite && isImemWrite
  io.imemWriteAddr := io.address - 0x8000.U

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
  io.uartValid := io.memWrite && isUartData
  io.uartAddr  := io.address
  io.uartData  := io.writeData
}