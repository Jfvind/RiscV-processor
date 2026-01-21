package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DataMemoryTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "DataMemory"

  // Hjælpefunktioner til load/store types (funct3 koder)
  // Baseret på din switch case implementation
  val LB  = "b000".U(3.W)
  val LH  = "b001".U(3.W)
  val LW  = "b010".U(3.W)
  val LBU = "b100".U(3.W)
  val LHU = "b101".U(3.W)

  val SB  = "b000".U(3.W)
  val SH  = "b001".U(3.W)
  val SW  = "b010".U(3.W)

  it should "store and load full words (SW -> LW)" in {
    test(new DataMemory) { dut =>
      // 1. Skriv 0xDEADBEEF til adresse 100
      dut.io.address.poke(100.U)
      dut.io.writeData.poke("hDEADBEEF".U)
      dut.io.memWrite.poke(true.B)
      dut.io.storeType.poke(SW)
      dut.clock.step(1) // Write er synkron (kræver clock edge)

      // 2. Sluk for write enable
      dut.io.memWrite.poke(false.B)

      // 3. Læs fra samme adresse
      dut.io.address.poke(100.U)
      dut.io.loadType.poke(LW)

      // Async read (hvis Mem) opdaterer med det samme (eller efter kort delay).
      // Hvis du bruger SyncReadMem, skal du bruge dut.clock.step(1) her.
      dut.io.readData.expect("hDEADBEEF".U)
    }
  }

  it should "handle byte stores without overwriting neighbors (SB)" in {
    test(new DataMemory) { dut =>
      // Mål: Skriv 4 forskellige bytes til samme word-adresse (offset 0, 1, 2, 3)
      // og se om de samles korrekt til et word (Little Endian).

      val baseAddr = 200 // Word aligned (200 % 4 == 0)

      // Skriv Byte 0: 0xAA
      dut.io.memWrite.poke(true.B)
      dut.io.storeType.poke(SB)

      dut.io.address.poke((baseAddr + 0).U)
      dut.io.writeData.poke(0xAA.U)
      dut.clock.step(1)

      // Skriv Byte 1: 0xBB
      dut.io.address.poke((baseAddr + 1).U)
      dut.io.writeData.poke(0xBB.U)
      dut.clock.step(1)

      // Skriv Byte 2: 0xCC
      dut.io.address.poke((baseAddr + 2).U)
      dut.io.writeData.poke(0xCC.U)
      dut.clock.step(1)

      // Skriv Byte 3: 0xDD
      dut.io.address.poke((baseAddr + 3).U)
      dut.io.writeData.poke(0xDD.U)
      dut.clock.step(1)

      // Sluk write
      dut.io.memWrite.poke(false.B)

      // Læs hele wordet med LW. Forvent 0xDDCCBBAA (fordi RISC-V er Little Endian)
      dut.io.address.poke(baseAddr.U)
      dut.io.loadType.poke(LW)
      dut.io.readData.expect("hDDCCBBAA".U)
    }
  }

  it should "handle sign extension correctly (LB vs LBU)" in {
    test(new DataMemory) { dut =>
      // Skriv 0xFF (-1 i 8-bit signed) til en adresse
      dut.io.memWrite.poke(true.B)
      dut.io.storeType.poke(SB)
      dut.io.address.poke(300.U)
      dut.io.writeData.poke(0xFF.U)
      dut.clock.step(1)
      dut.io.memWrite.poke(false.B)

      // Case 1: Load Byte Signed (LB)
      // 0xFF skal blive til 0xFFFFFFFF (-1 i 32-bit)
      dut.io.address.poke(300.U)
      dut.io.loadType.poke(LB)
      dut.io.loadUnsigned.poke(false.B) // Hvis din kode bruger denne
      dut.io.readData.expect("hFFFFFFFF".U)

      // Case 2: Load Byte Unsigned (LBU)
      // 0xFF skal blive til 0x000000FF (255 i 32-bit)
      dut.io.loadType.poke(LBU)
      dut.io.loadUnsigned.poke(true.B) // Hvis din kode bruger denne
      dut.io.readData.expect("h000000FF".U)
    }
  }
}