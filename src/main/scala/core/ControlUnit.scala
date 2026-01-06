// Decodes the instruction (opcode, funct3, funct7) and generates control signals (e.g., aluOp, memWrite, regWrite).
package core

import chisel3._
import chisel3.util._
import core.ALUConstants._

/*
Input: instruction[31:0] (fra IF/ID register)
Output: Alle control signals (RegWrite, ALUOp, ALUSrc, MemRead, MemWrite, MemToReg, etc.)
Kombinatorisk logic der decoder opcode, funct3, funct7
 */

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))

    val regWrite    = Output(Bool())
    val aluSrc      = Output(Bool())
    val aluOp       = Output(UInt(4.W))
    val imm         = Output(UInt(32.W)) // The FINAL selected immediate
    val memWrite    = Output(Bool())
    val branch      = Output(Bool())
  })

  val opcode = io.instruction(6, 0)

  // Instantiate ImmGen
  val immGen = Module(new ImmGen())
  immGen.io.instruction := io.instruction

  // Default signals
  io.regWrite := false.B
  io.aluSrc   := false.B
  io.aluOp    := ALU_ADD
  io.imm      := 0.U
  io.memWrite := false.B
  io.branch   := false.B

  switch(opcode) {
    // I-Type Arithmetic (ADDI)
    is("b0010011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_i // Select I-Type from ImmGen
    }
    // Add other cases here...

    // S-Type (Store Word - SW) - Opcode: 0100011
    // Vi skal bruge ALU til at beregne adressen (rs1 + imm_s).
    // Vi skal IKKE skrive til et register (regWrite = false).
    // Vi SKAL skrive til hukommelsen (memWrite = true).
    is("b0100011".U) {
      io.regWrite := false.B
      io.aluSrc   := true.B       // Brug immediate til adresse-offset
      io.aluOp    := ALU_ADD      // Beregn adresse: rs1 + offset
      io.imm      := immGen.io.imm_s
      io.memWrite := true.B       // VIGTIGT: Skriv til hukommelsen
    }

    // B-Type (Branch Greater or Equal - BGE) - Opcode: 1100011
    // Vi sammenligner to registre.
    // Vi bruger ALU'ens SLT (Set Less Than) logik.
    // Hvis rs1 < rs2, bliver resultatet 1. Hvis rs1 >= rs2, bliver resultatet 0.
    is("b1100011".U) {
      io.regWrite := false.B
      io.aluSrc   := false.B      // Sammenlign to registre (ikke immediate)
      io.aluOp    := ALU_SLT      // Check om rs1 < rs2
      io.imm      := immGen.io.imm_b
      io.branch   := true.B       // Signalér at dette er en branch
    }
  }
}