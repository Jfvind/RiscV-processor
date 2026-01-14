package core

import chisel3._
import chisel3.util._
import core.ALUConstants._

class ALUDecoder extends Module {
  val io = IO(new Bundle {
    //Control
    val aluOp   = Input(UInt(2.W))  // From Main Control (00, 01, 10, 11)
    //instrucs
    val funct3  = Input(UInt(3.W))  // From Instruction [14:12]
    val funct7  = Input(UInt(7.W))  // From Instruction [31:25]

    val op      = Output(UInt(4.W)) // Towards ALU
  })

  // Default: NOP OR ADD.. SHoul dbe safe enough
  io.op := ALU_ADD

  // Bit 30 in Instruction (funct7 bit 5) Is "Modifier bit".
  // Decides between ADD/SUB og SRL/SRA.
  val func7bit5 = io.funct7(5)

  switch(io.aluOp) {

    //===============  1. LW / SW (Memory Access) =======================================================
    is("b00".U) {
      io.op := ALU_ADD // calc addy: Reg + Imm
    }


    //===============  2. BRANCH (BEQ, BNE, BLT, etc.) =================================================

    // FOR ALLU OP ( LOOK AT ALU CONSTANTS.. Couple of hacks here :-)) /Holm
    is("b01".U) {
      switch(io.funct3) {
        // BEQ (000) & BNE (001) -> SUB.
        // Vi tjekker Zero-flaget (Resultat == 0).
        is("b000".U) { io.op := ALU_SUB }
        is("b001".U) { io.op := ALU_SUB }

        // BLT (100) & BGE (101) -> SLT.
        // Check LessSigned-flag
        is("b100".U) { io.op := ALU_SLT }
        is("b101".U) { io.op := ALU_SLT }

        // BLTU (110) & BGEU (111) -> SLTU.
        // Check LessUnsigned-flaget
        is("b110".U) { io.op := ALU_SLTU }
        is("b111".U) { io.op := ALU_SLTU }
      }
    }


    // =============== 3. R-TYPE Instructions (Register-Register) =============================================
    is("b10".U) {
      switch(io.funct3) {
        is("b000".U) { // ADD or SUB
          // If bit 30 HIGH, is SUB. Else ADD.
          io.op := Mux(func7bit5, ALU_SUB, ALU_ADD)
        }
        is("b001".U) { io.op := ALU_SLL } // SLL
        is("b010".U) { io.op := ALU_SLT } // SLT
        is("b011".U) { io.op := ALU_SLTU } // SLTU
        is("b100".U) { io.op := ALU_XOR } // XOR
        is("b101".U) { // SRL eller SRA
          // If bit 30 is HIGH, it's SRA (Arithmetic). Else SRL.
          io.op := Mux(func7bit5, ALU_SRA, ALU_SRL)
        }
        is("b110".U) { io.op := ALU_OR }  // OR
        is("b111".U) { io.op := ALU_AND } // AND
      }
    }

    // 4.===============  I-TYPE Instructions (Register-Immediate) =============================================

    is("b11".U) {
      switch(io.funct3) {
        is("b000".U) { io.op := ALU_ADD }  // ADDI
        is("b001".U) { io.op := ALU_SLL }  // SLLI
        is("b010".U) { io.op := ALU_SLT }  // SLTI
        is("b011".U) { io.op := ALU_SLTU } // SLTIU
        is("b100".U) { io.op := ALU_XOR }  // XORI
        is("b101".U) { // SRLI or SRAI
          // Shift Immediate also uses funct7 til decide logic/arith
          io.op := Mux(func7bit5, ALU_SRA, ALU_SRL)
        }
        is("b110".U) { io.op := ALU_OR }   // ORI
        is("b111".U) { io.op := ALU_AND }  // ANDI
      }
    }
  }
}
