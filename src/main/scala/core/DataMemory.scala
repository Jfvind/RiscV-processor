package core

import chisel3._

class DataMemory extends Module {
  val io = IO(new Bundle {
    // Adresse vi vil læse fra eller skrive til (kommer fra ALU result)
    val address   = Input(UInt(32.W))
    // Data vi vil skrive (kommer fra rs2 i RegisterFile)
    val writeData = Input(UInt(32.W))
    // Skal vi skrive? (kommer fra ControlUnit)
    val memWrite  = Input(Bool())
    
    // Data vi har læst (til senere brug med 'lw')
    val readData  = Output(UInt(32.W))
  })

  // 16KB hukommelse (4096 words af 32 bits)
  val memory = Mem(4096, UInt(32.W)) // Convert to Syncreadmem????

  // Læsning: Sker altid (async read for denne type Mem) Correct????
  // Vi dividerer adressen med 4 (address >> 2) fordi memory er indekseret i words,
  // men CPU'en arbejder med byte-adresser.
  io.readData := memory(io.address >> 2)

  // Skrivning: Sker kun hvis memWrite er høj
  when(io.memWrite) {
    memory(io.address >> 2) := io.writeData
  }
}