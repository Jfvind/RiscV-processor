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
import core.ALUConstants._
import core.ControlConstants.{ALU_OP_BRANCH, ALU_OP_ITYPE, ALU_OP_MEM, ALU_OP_RTYPE}

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))   // Input: Full 32-bit instruction from the Fetch stage

    val regWrite    = Output(Bool())      // Enables writing to the destination register (rd).
    val aluSrc      = Output(Bool())      // Controls MUX on the input B of the ALU: either (rs2) or (imm)
    val aluOp       = Output(UInt(2.W))  // prev 4-bit now 2 bit, as ALUDecode is incoorperated.
    val imm         = Output(UInt(32.W))  // The extracted immediate from the instruction, now extended to 32 bits via sign extension.
    val memWrite    = Output(Bool())      // Write enable for our data memory (RAM)
    val branch      = Output(Bool())      // Indicates a branch instruction --> Fetch-state (for PC logic). Combined with ALU result to decide if we jump.
    val memToReg    = Output(Bool())  // For Load instructions to select data from memory to write to register

  })

  // ============================================
  // DECODE INSTRUCTIONS HERE
  // Slice the 32-bit intstruction into its components (opcode, funct3, etc.)
    val opcode = io.instruction(6, 0)
    //val funct3 = io.instruction(14,12) // for ADD/SUB
    //val funct7 = io.instruction(31,25)
  // ============================================

  val immGen = Module(new ImmGen()) // Instantiating the immediate generator
  immGen.io.instruction := io.instruction // Connecting instruction input to the ImmGen module

  // Default signals: for defined behaviour when being synthesized
  io.regWrite := false.B
  io.aluSrc   := false.B
  io.aluOp    := ALU_OP_MEM
  io.memWrite := false.B
  io.branch   := false.B
  io.memToReg := false.B

  io.imm      := immGen.io.imm_i

  switch(opcode) {
    // 1. R-Type (add, sub, xor, etc.)
    is("b0110011".U) {
      io.aluOp    := ALU_OP_RTYPE // Fortæl ALUDecoder at det er R-type
      io.regWrite := true.B
      io.aluSrc   := false.B      // Brug Register (rs2) som input B
      io.memToReg := false.B
    }

    // 2. I-Type Arithmetic (addi, etc.)
    is("b0010011".U) {
      io.aluOp    := ALU_OP_ITYPE // ALUDecode
      io.regWrite := true.B
      io.aluSrc   := true.B       // Brug Immediate som input B
      io.memToReg := false.B
      io.imm      := immGen.io.imm_i
    }

    // 3. Load (lw)
    is("b0000011".U) {
      io.aluOp    := ALU_OP_MEM   // Vi skal lægge sammen (Base + Offset)
      io.regWrite := true.B       // Vi skriver data fra memory til register
      io.aluSrc   := true.B       // Adresse offset er immediate
      io.memToReg := true.B       //Mem not ALU
      io.imm      := immGen.io.imm_i
      //Memread
    }

    // 4. Store (sw) - S-Type
    is("b0100011".U) {
      io.aluOp    := ALU_OP_MEM   // Vi skal lægge sammen (Base + Offset)
      io.memWrite := true.B
      io.aluSrc   := true.B       // Adresse offset er immediate
      io.regWrite := false.B      // Stores returnerer ikke noget til register
      io.memToReg := false.B
      io.imm      := immGen.io.imm_s
    }

    // 5. Branch (beq, bne, etc.) - B-Type
    is("b1100011".U) {
      io.aluOp    := ALU_OP_BRANCH // Fortæl ALUDecoder at lave sammenligning
      io.branch   := true.B
      io.aluSrc   := false.B       // Branches sammenligner Reg vs Reg
      io.regWrite := false.B
      io.memToReg := false.B
      io.imm      := immGen.io.imm_b
    }

    // 6. LUI (Load Upper Immediate)
    is("b0110111".U) {
      io.aluOp    := ALU_OP_MEM
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.memToReg := false.B
      io.imm      := immGen.io.imm_u
    }
  }
}