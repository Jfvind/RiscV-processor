/*
* Control Unit:
* Decodes the 32-bit instruction (opcode, funct3, funct7) into control signals.
* It interprets what the instruction represents and distributes theses signals to the rest
* of the processor to execute the intended opeation.
* Type: Pure Combinational logic (Output depends solely on current Input).
*/
package core

import chisel3._
import chisel3.util._
import core.ALUConstants._ // ALUConstans is a dict which enable us to use mneomonic codes e.g., ALU_ADD instead of raw "magic numbers".

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))   // Input: Full 32-bit instruction from the Fetch stage

    val regWrite    = Output(Bool())      // Enables writing to the destination register (rd).
    val aluSrc      = Output(Bool())      // Controls MUX on the input B of the ALU: either (rs2) or (imm)
    val aluOp       = Output(UInt(4.W))   // 4-bit code for ALU, differentiating and defining operation at hand for the ALU.
    val imm         = Output(UInt(32.W))  // The extracted immediate from the instruction, now extended to 32 bits via sign extension.
    val memWrite    = Output(Bool())      // Write enable for our data memory (RAM)
    val branch      = Output(Bool())      // Indicates a branch instruction --> Fetch-state (for PC logic). Combined with ALU result to decide if we jump.
  })

  // ============================================
  // DECODE INSTRUCTIONS HERE
  // Slice the 32-bit intstruction into its components (opcode, funct3, etc.)
    val opcode = io.instruction(6, 0)
    val funct3 = io.instruction(14,12) // for ADD/SUB
    val funct7 = io.instruction(31,25)
  // ============================================

  val immGen = Module(new ImmGen()) // Instantiating the immediate generator
  immGen.io.instruction := io.instruction // Connecting instruction input to the ImmGen module

  // Default signals: for defined behaviour when being synthesized
  io.regWrite := false.B
  io.aluSrc   := false.B
  io.aluOp    := ALU_ADD
  io.imm      := 0.U
  io.memWrite := false.B
  io.branch   := false.B

  switch(opcode) {
    // I-Type Arithmetic (ADDI) - Opcode: 0010011
    is("b0010011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_i // Select I-Type from ImmGen
    }

    // R-type (ADD (for now)) - Opcode: 0110011
    is("b0110011".U) {
      io.regWrite := true.B
      io.aluSrc   := false.B
      io.aluOp    := ALU_ADD
      io.imm      := 0.U
    }

    // S-Type (Store Word - SW) - Opcode: 0100011
    is("b0100011".U) {
      io.regWrite := false.B
      io.aluSrc   := true.B       // Use immediate for adress calculation
      io.aluOp    := ALU_ADD      // Calculate address: rs1 + offset
      io.imm      := immGen.io.imm_s
      io.memWrite := true.B       // Store data in memory
    }

    // B-type (Branch Greater or Equal - BGE) - Opcode: 1100011
    is("b1100011".U) {
      io.regWrite := false.B
      io.aluSrc   := false.B      // Compare two registers not immediate.
      io.aluOp    := ALU_SLT      // Check if rs1 < rs2 --> if false, then rs1 >= rs2 --> branch taken.
      io.imm      := immGen.io.imm_b
      io.branch   := true.B       // Signal that this is a branch.
    }

    // U-type (Load Upper Immediate - LUI) - Opcode: 0110111
    is("b0110111".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.aluOp    := ALU_LUI
      io.imm      := immGen.io.imm_u
      io.memWrite := false.B
      io.branch   := false.B
    }
  }
}