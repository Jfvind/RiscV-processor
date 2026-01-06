import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.ALU

class ALUTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALU"

  it should "perform ADD operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(15.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(0.U)  // ADD
      dut.io.result.expect(25.U)

      // Edge case: overflow wraps around
      dut.io.alu_a.poke("hFFFFFFFF".U)
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "perform SUB operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke(20.U)
      dut.io.alu_b.poke(8.U)
      dut.io.alu_op.poke(1.U)  // SUB
      dut.io.result.expect(12.U)

      // Negative result (wraps around in unsigned)
      dut.io.alu_a.poke(5.U)
      dut.io.alu_b.poke(10.U)
      dut.io.result.expect("hFFFFFFFB".U)  // -5 in two's complement
    }
  }

  it should "perform AND operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(2.U)  // AND
      dut.io.result.expect("b1000".U)

      dut.io.alu_a.poke("hFF00FF00".U)
      dut.io.alu_b.poke("h00FFFF00".U)
      dut.io.result.expect("h0000FF00".U)
    }
  }

  it should "perform OR operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(3.U)  // OR
      dut.io.result.expect("b1110".U)

      dut.io.alu_a.poke("hFF00FF00".U)
      dut.io.alu_b.poke("h00FFFF00".U)
      dut.io.result.expect("hFFFFFF00".U)
    }
  }

  it should "perform XOR operation" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1010".U)
      dut.io.alu_b.poke("b1100".U)
      dut.io.alu_op.poke(4.U)  // XOR
      dut.io.result.expect("b0110".U)

      // XOR with self = 0
      dut.io.alu_a.poke(12345.U)
      dut.io.alu_b.poke(12345.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "perform SLL (shift left logical)" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b0001".U)
      dut.io.alu_b.poke(2.U)  // Shift amount
      dut.io.alu_op.poke(5.U)  // SLL
      dut.io.result.expect("b0100".U)

      dut.io.alu_a.poke(1.U)
      dut.io.alu_b.poke(31.U)  // Maximum shift
      dut.io.result.expect("h80000000".U)

      // Only lower 5 bits of shift amount used (RISC-V spec)
      dut.io.alu_a.poke(1.U)
      dut.io.alu_b.poke(33.U)  // Should be treated as 1
      dut.io.result.expect(2.U)
    }
  }

  it should "perform SRL (shift right logical)" in {
    test(new ALU) { dut =>
      dut.io.alu_a.poke("b1000".U)
      dut.io.alu_b.poke(2.U)
      dut.io.alu_op.poke(6.U)  // SRL
      dut.io.result.expect("b0010".U)

      // Logical shift fills with zeros
      dut.io.alu_a.poke("h80000000".U)
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect("h40000000".U)
    }
  }

  it should "perform SRA (shift right arithmetic)" in {
    test(new ALU) { dut =>
      // Positive number - same as SRL
      dut.io.alu_a.poke("b0100".U)
      dut.io.alu_b.poke(1.U)
      dut.io.alu_op.poke(7.U)  // SRA
      dut.io.result.expect("b0010".U)

      // Negative number - sign extends
      dut.io.alu_a.poke("h80000000".U)  // -2147483648 in signed
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect("hC0000000".U)  // Sign bit preserved

      dut.io.alu_a.poke("hFFFFFFFC".U)  // -4 in signed
      dut.io.alu_b.poke(1.U)
      dut.io.result.expect("hFFFFFFFE".U)  // -2 in signed
    }
  }

  it should "perform SLT (set less than - signed)" in {
    test(new ALU) { dut =>
      // 5 < 10 = true
      dut.io.alu_a.poke(5.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(8.U)  // SLT
      dut.io.result.expect(1.U)

      // 10 < 5 = false
      dut.io.alu_a.poke(10.U)
      dut.io.alu_b.poke(5.U)
      dut.io.result.expect(0.U)

      // Signed comparison: -1 < 0
      dut.io.alu_a.poke("hFFFFFFFF".U)  // -1 in signed
      dut.io.alu_b.poke(0.U)
      dut.io.result.expect(1.U)

      // Equal values = false
      dut.io.alu_a.poke(42.U)
      dut.io.alu_b.poke(42.U)
      dut.io.result.expect(0.U)
    }
  }

  it should "perform SLTU (set less than - unsigned)" in {
    test(new ALU) { dut =>
      // 5 < 10 = true
      dut.io.alu_a.poke(5.U)
      dut.io.alu_b.poke(10.U)
      dut.io.alu_op.poke(9.U)  // SLTU
      dut.io.result.expect(1.U)

      // Unsigned comparison: 0xFFFFFFFF > 0
      dut.io.alu_a.poke("hFFFFFFFF".U)
      dut.io.alu_b.poke(0.U)
      dut.io.result.expect(0.U)  // 0xFFFFFFFF is NOT less than 0

      // Opposite test
      dut.io.alu_a.poke(0.U)
      dut.io.alu_b.poke("hFFFFFFFF".U)
      dut.io.result.expect(1.U)
    }
  }

  it should "handle all operations in sequence" in {
    test(new ALU) { dut =>
      val testCases = Seq(
        (100.U, 50.U, 0.U, 150.U),           // ADD
        (100.U, 50.U, 1.U, 50.U),            // SUB
        ("hFF".U, "hF0".U, 2.U, "hF0".U),    // AND
        ("hF0".U, "h0F".U, 3.U, "hFF".U),    // OR
        ("hFF".U, "hF0".U, 4.U, "h0F".U),    // XOR
        (1.U, 4.U, 5.U, 16.U),               // SLL
        (16.U, 2.U, 6.U, 4.U),               // SRL
      )

      for ((a, b, op, expected) <- testCases) {
        dut.io.alu_a.poke(a)
        dut.io.alu_b.poke(b)
        dut.io.alu_op.poke(op)
        dut.io.result.expect(expected)
      }
    }
  }
}