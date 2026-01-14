package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.ControlConstants._

class ControlUnitTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ControlUnit"

  it should "decode SW (Store Word) correctly" in {
    test(new ControlUnit) { dut =>
      // Opcode for SW is 0100011 (binary) -> 0x23 (hex) for de nederste 7 bits
      // Hele instruktionen: sw x1, 0(x2) -> bitmønster... 
      // Vi snyder og konstruerer bare en instruktion med rigtig opcode
      // Opcode ligger på bit [6:0]
      val swInstruction = "b0000000_00001_00010_010_00000_0100011".U 

      dut.io.instruction.poke(swInstruction)
      
      // Tjek at ControlUnit tænder de rigtige signaler
      dut.io.memWrite.expect(true.B)  // SKAL være true
      dut.io.regWrite.expect(false.B) // Må IKKE skrive til register
      dut.io.branch.expect(false.B)   // Er ikke en branch
      dut.io.aluSrc.expect(true.B)    // Skal bruge immediate til adresse beregning
      dut.io.aluOp.expect(ALU_OP_MEM)
    }
  }

  it should "decode BGE (Branch Greater Equal) correctly" in {
    test(new ControlUnit) { dut =>
      // Opcode for BGE is 1100011
      val bgeInstruction = "b0000000_00001_00010_101_00000_1100011".U

      dut.io.instruction.poke(bgeInstruction)

      dut.io.branch.expect(true.B)    // SKAL være true
      dut.io.memWrite.expect(false.B) // Må ikke skrive til ram
      dut.io.regWrite.expect(false.B) // Må ikke skrive til register
      dut.io.aluOp.expect(ALU_OP_BRANCH)    // Branch IKKE SLT
    }
  }
}