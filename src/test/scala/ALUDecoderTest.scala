import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.ALUConstants._
import core.ALUDecoder
import core.ControlConstants._

class ALUDecoderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALUDecoder"

  it should "decode memory operations (LW/SW)" in {
    test(new ALUDecoder) { dut =>
      dut.io.aluOp.poke(ALU_OP_MEM)
      dut.io.funct3.poke(0.U)  // Don't care
      dut.io.funct7.poke(0.U)  // Don't care
      dut.io.op.expect(ALU_ADD)
    }
  }

  it should "decode branch operations" in {
    test(new ALUDecoder) { dut =>
      dut.io.aluOp.poke(ALU_OP_BRANCH)

      // BEQ (000) -> SUB
      dut.io.funct3.poke("b000".U)
      dut.io.op.expect(ALU_SUB)

      // BNE (001) -> SUB
      dut.io.funct3.poke("b001".U)
      dut.io.op.expect(ALU_SUB)

      // BLT (100) -> SLT
      dut.io.funct3.poke("b100".U)
      dut.io.op.expect(ALU_SLT)

      // BGE (101) -> SLT
      dut.io.funct3.poke("b101".U)
      dut.io.op.expect(ALU_SLT)

      // BLTU (110) -> SLTU
      dut.io.funct3.poke("b110".U)
      dut.io.op.expect(ALU_SLTU)

      // BGEU (111) -> SLTU
      dut.io.funct3.poke("b111".U)
      dut.io.op.expect(ALU_SLTU)
    }
  }

  it should "decode R-type operations" in {
    test(new ALUDecoder) { dut =>
      dut.io.aluOp.poke(ALU_OP_RTYPE)

      // ADD (funct3=000, funct7[5]=0)
      dut.io.funct3.poke("b000".U)
      dut.io.funct7.poke("b0000000".U)
      dut.io.op.expect(ALU_ADD)

      // SUB (funct3=000, funct7[5]=1)
      dut.io.funct3.poke("b000".U)
      dut.io.funct7.poke("b0100000".U)
      dut.io.op.expect(ALU_SUB)

      // AND (111)
      dut.io.funct3.poke("b111".U)
      dut.io.op.expect(ALU_AND)

      // OR (110)
      dut.io.funct3.poke("b110".U)
      dut.io.op.expect(ALU_OR)

      // XOR (100)
      dut.io.funct3.poke("b100".U)
      dut.io.op.expect(ALU_XOR)

      // SLL (001)
      dut.io.funct3.poke("b001".U)
      dut.io.op.expect(ALU_SLL)

      // SRL (funct3=101, funct7[5]=0)
      dut.io.funct3.poke("b101".U)
      dut.io.funct7.poke("b0000000".U)
      dut.io.op.expect(ALU_SRL)

      // SRA (funct3=101, funct7[5]=1)
      dut.io.funct3.poke("b101".U)
      dut.io.funct7.poke("b0100000".U)
      dut.io.op.expect(ALU_SRA)

      // SLT (010)
      dut.io.funct3.poke("b010".U)
      dut.io.op.expect(ALU_SLT)

      // SLTU (011)
      dut.io.funct3.poke("b011".U)
      dut.io.op.expect(ALU_SLTU)
    }
  }

  it should "decode I-type operations" in {
    test(new ALUDecoder) { dut =>
      dut.io.aluOp.poke(ALU_OP_ITYPE)

      // ADDI (000)
      dut.io.funct3.poke("b000".U)
      dut.io.op.expect(ALU_ADD)

      // ANDI (111)
      dut.io.funct3.poke("b111".U)
      dut.io.op.expect(ALU_AND)

      // ORI (110)
      dut.io.funct3.poke("b110".U)
      dut.io.op.expect(ALU_OR)

      // XORI (100)
      dut.io.funct3.poke("b100".U)
      dut.io.op.expect(ALU_XOR)

      // SLLI (001)
      dut.io.funct3.poke("b001".U)
      dut.io.op.expect(ALU_SLL)

      // SRLI (funct3=101, funct7[5]=0)
      dut.io.funct3.poke("b101".U)
      dut.io.funct7.poke("b0000000".U)
      dut.io.op.expect(ALU_SRL)

      // SRAI (funct3=101, funct7[5]=1)
      dut.io.funct3.poke("b101".U)
      dut.io.funct7.poke("b0100000".U)
      dut.io.op.expect(ALU_SRA)

      // SLTI (010)
      dut.io.funct3.poke("b010".U)
      dut.io.op.expect(ALU_SLT)

      // SLTIU (011)
      dut.io.funct3.poke("b011".U)
      dut.io.op.expect(ALU_SLTU)
    }
  }
}