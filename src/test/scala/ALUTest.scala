package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.ALU
import core.ALUConstants._ // <--- VIGTIGT: Importerer dine konstanter (ALU_ADD, ALU_SUB osv.)

class ALUTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALU"

  it should "perform ADD operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(15.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(ALU_ADD)
      dut.io.result.expect(25.U)

      // Test overflow/wrap-around
      dut.io.alu_a.poke("hFFFFFFFF".U)
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "perform SUB operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(20.U)
      dut.io.alu_b.poke(8.U)
      dut.io.alu_op.poke(ALU_SUB)
      dut.io.result.expect(12.U)
    }
  }

  it should "perform AND operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(ALU_AND)
      dut.io.result.expect("b1000".U)
    }
  }

  it should "perform OR operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(ALU_OR)
      dut.io.result.expect("b1110".U)
    }
  }

  it should "perform XOR operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(ALU_XOR)
      dut.io.result.expect("b0110".U)

      // XOR med sig selv skal give 0
      dut.io.alu_a.poke(12345.U)
      dut.io.alu_b.poke(12345.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "perform SLL (shift left logical)" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(1.U)
      dut.io.alu_b.poke(2.U)  // Shift amount
      dut.io.alu_op.poke(ALU_SLL)
      dut.io.result.expect(4.U) // 1 << 2 = 4
    }
  }

  it should "perform SRL (shift right logical)" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(8.U)
      dut.io.alu_b.poke(2.U)
      dut.io.alu_op.poke(ALU_SRL)
      dut.io.result.expect(2.U) // 8 >> 2 = 2

      // Test at den ikke beholder fortegn (Logical shift)
      // -1 (alle 1'ere) shiftet 1 gang skal give 0 i MSB
      dut.io.alu_a.poke("hFFFFFFFF".U)
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect("h7FFFFFFF".U)
    }
  }

  it should "perform SRA (shift right arithmetic)" in {
    test(new ALU) { dut =>
      // Test at den BEHOLDER fortegn (Arithmetic shift)
      // -4 (11...1100) shiftet 1 gang skal blive -2 (11...1110)
      dut.io.alu_a.poke("hFFFFFFFC".U) // -4
      dut.io.alu_b.poke(1.U)
      dut.io.alu_op.poke(ALU_SRA)
      dut.io.result.expect("hFFFFFFFE".U) // -2
    }
  }

  it should "perform SLT (set less than - signed)" in {
    test(new ALU) { dut =>
      // 5 < 10 = true (1)
      dut.io.alu_a.poke(5.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(ALU_SLT)
      dut.io.result.expect(1.U)
      dut.io.less_signed.expect(true.B)

      // 10 < 5 = false (0)
      dut.io.alu_a.poke(10.U)
      dut.io.alu_b.poke(5.U)
      dut.io.result.expect(0.U)
      dut.io.less_signed.expect(false.B)

      // Negativ test: -1 < 0 = true
      dut.io.alu_a.poke("hFFFFFFFF".U) // -1
      dut.io.alu_b.poke(0.U)
      dut.io.result.expect(1.U)
    }
  }

  it should "perform SLTU (set less than - unsigned)" in {
    test(new ALU) { dut =>
      // 5 < 10 = true
      dut.io.alu_a.poke(5.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(ALU_SLTU)
      dut.io.result.expect(1.U)

      // Unsigned check:
      // "minus 1" (0xFFFFFFFF) er et KÆMPE tal i unsigned.
      // Så: KÆMPE < 0 = false
      dut.io.alu_a.poke("hFFFFFFFF".U)
      dut.io.alu_b.poke(0.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "set Zero flag correctly for BEQ logic" in {
    test(new ALU) { dut =>
      // Vi bruger SUB til at teste lighed
      dut.io.alu_op.poke(ALU_SUB)

      // 10 - 10 = 0 -> Zero Flag skal være True
      dut.io.alu_a.poke(10.U)
      dut.io.alu_b.poke(10.U)
      dut.io.result.expect(0.U)
      dut.io.zero.expect(true.B)

      // 10 - 5 = 5 -> Zero Flag skal være False
      dut.io.alu_b.poke(5.U)
      dut.io.zero.expect(false.B)
    }
  }
}