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

  switch(opcode) {
    // I-Type Arithmetic (ADDI)
    is("b0010011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_i // Select I-Type from ImmGen
    }
    // Add other cases here...
  }
}