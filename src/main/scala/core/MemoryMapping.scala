package core

import chisel3._
import chisel3.util._

class MemoryMapping extends Module {
  val io = IO(new Bundle {
    // Signaler fra Core
    val address   = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val memWrite  = Input(Bool())
    
    // Signaler tilbage til Core
    val readData  = Output(UInt(32.W))

    // Fysiske pins til FPGA'en
    val led    = Output(UInt(1.W))
  })

  // 1. Instantiér den rigtige DataMemory
  val dataMem = Module(new DataMemory())

  // 2. Definér Base-adressen for IO
  // HACK: Vi bruger adresse 100 (0x64) til test, da vi kun har 'addi'
  // (Senere skal dette rettes tilbage til "hF0000000".U)
  val mmioBase = 100.U(32.W)

  // 3. Dekoder: Hvor peger adressen hen?
  val isLed = (io.address === mmioBase)
  
  // Hvis vi IKKE snakker med LED, så snakker vi med RAM
  // Vi tilføjer en ekstra sikkerhed: Det er kun RAM, hvis adressen IKKE er LED
  val isRam = !isLed

  // ---------------------------------------------------------
  // RAM LOGIK
  // ---------------------------------------------------------
  dataMem.io.address   := io.address
  dataMem.io.writeData := io.writeData
  
  // Vi må KUN skrive i RAM, hvis 'memWrite' er høj OG det er en RAM-adresse
  dataMem.io.memWrite  := io.memWrite && isRam

  io.readData := dataMem.io.readData

  // ---------------------------------------------------------
  // LED LOGIK
  // ---------------------------------------------------------
  val ledReg = RegInit(0.U(1.W))

  // Hvis vi skriver til adresse 100, opdateres LED'en
  when(io.memWrite && isLed) {
    ledReg := io.writeData(0)
  }

  io.led := ledReg
}