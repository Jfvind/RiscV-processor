package core

import chisel3._
//import chisel3.util.experimental.loadMemoryFromFile
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util._

class DataMemory extends Module {
  val io = IO(new Bundle {
    val address   = Input(UInt(32.W)) // From ALU
    val writeData = Input(UInt(32.W)) // Data to store (from rs2 in RegisterFile) (Register to RAM)
    val memWrite  = Input(Bool()) // Writing or not (from ControlUnit)
    
    val readData  = Output(UInt(32.W)) // Later for lw etc. (RAM to Register)

    val loadType      = Input(UInt(3.W))
    val loadUnsigned  = Input(Bool())
    val storeType     = Input(UInt(3.W))
  })

  // 32KB memory
  val memory = Mem(8192, UInt(32.W)) // Convert to Syncreadmem????

  // This tells the synthesis tool to preload RAM with your program/data
  loadMemoryFromFileInline(memory, "src/main/resources/prime_bench.mem")
  
  val wordAddr = io.address >> 2
  val byteOffset = io.address(1, 0) // 0, 1, 2, 3
  val bitOffset = byteOffset * 8.U  // 0, 8, 16, 24

  // --- READ LOGIC ---
  val word = memory.read(wordAddr)
  val readDataRaw = Wire(UInt(32.W))
  readDataRaw := 0.U

  // I stedet for slicing: Shift wordet ned, så de data vi vil have, ligger på index 0
  // F.eks: Hvis vi vil læse byte 1 (bits 15:8), shifter vi wordet 8 pladser til højre.
  val shiftedWord = word >> bitOffset

  switch(io.loadType) {
    is("b000".U) {  // LB
      val byte = shiftedWord(7, 0) // Nu ligger den ønskede byte altid nederst
      readDataRaw := Mux(io.loadUnsigned, byte, Cat(Fill(24, byte(7)), byte))
    }
    is("b001".U) {  // LH
      // Bemærk: LH kræver alignment. Antager her byteOffset er 0 eller 2.
      val half = shiftedWord(15, 0)
      readDataRaw := Mux(io.loadUnsigned, half, Cat(Fill(16, half(15)), half))
    }
    is("b010".U) {  // LW
      readDataRaw := word
    }
    is("b100".U) {  // LBU
      val byte = shiftedWord(7, 0)
      readDataRaw := byte // Zero-extended automatisk da UInt(32.W) := UInt(8.W)
    }
    is("b101".U) {  // LHU
      val half = shiftedWord(15, 0)
      readDataRaw := half
    }
  }
  io.readData := readDataRaw

  // --- WRITE LOGIC ---
  when(io.memWrite) {
    val currentWord = memory.read(wordAddr)
    val newWord = Wire(UInt(32.W))
    newWord := currentWord // Default

    switch(io.storeType) {
      is("b000".U) { // SB
        // Logik: Masker den gamle byte ud, sæt den nye ind.
        // Eks: word = 0xAABBCCDD, skriv EE på byte 1 (CC).
        // Mask: ~(0xFF << 8) -> 0xFFFF00FF
        // Data: 0xEE << 8    -> 0x0000EE00
        // Result: (0xAABBCCDD & 0xFFFF00FF) | 0x0000EE00 -> 0xAABBEEDD

        val mask = ~(255.U(32.W) << bitOffset) // 255 = 0xFF
        val data = (io.writeData(7, 0) << bitOffset)
        newWord := (currentWord & mask) | data
      }
      is("b001".U) { // SH
        val mask = ~(65535.U(32.W) << bitOffset) // 65535 = 0xFFFF
        val data = (io.writeData(15, 0) << bitOffset)
        newWord := (currentWord & mask) | data
      }
      is("b010".U) { // SW
        newWord := io.writeData
      }
    }
    memory.write(wordAddr, newWord)
  }
}