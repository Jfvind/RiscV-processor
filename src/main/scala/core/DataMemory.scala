package core

import chisel3._
//import chisel3.util.experimental.loadMemoryFromFile
import chisel3.util.experimental.loadMemoryFromFileInline

class DataMemory extends Module {
  val io = IO(new Bundle {
    val address   = Input(UInt(32.W)) // From ALU
    val writeData = Input(UInt(32.W)) // Data to store (from rs2 in RegisterFile) (Register to RAM)
    val memWrite  = Input(Bool()) // Writing or not (from ControlUnit)
    
    val readData  = Output(UInt(32.W)) // Later for lw etc. (RAM to Register)
  })

  // 16KB memory
  val memory = Mem(17500, UInt(32.W)) // Convert to Syncreadmem????

  // This tells the synthesis tool to preload RAM with your program/data
  //loadMemoryFromFile(memory, "C:/DTU-local/Repos/RiscV-processor/src/main/resources/prime_bench.mem")
  //loadMemoryFromFileInline(memory, "C:/DTU-local/Repos/RiscV-processor/src/main/resources/prime_bench.mem")

  // Read: Happens constantly (async read for Mem) Syncreadmem instead????
  // Dividing address by 4 as memory is in words, while CPU uses byte-addressing
  io.readData := memory(io.address >> 2)

  // Writing if Control signal is high
  when(io.memWrite) {
    memory(io.address >> 2) := io.writeData
  }
}