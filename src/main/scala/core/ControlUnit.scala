package core

import chisel3._
import chisel3.util._
import core.CSRConstants._
import core.ALUConstants._

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))
    val regWrite    = Output(Bool())
    val aluSrc      = Output(Bool())
    val aluOp       = Output(UInt(4.W))
    val imm         = Output(UInt(32.W))
    val memWrite    = Output(Bool())
    val branch      = Output(Bool())
    val memToReg    = Output(Bool())
    val jump        = Output(Bool())
    val jumpReg     = Output(Bool())
    val auipc       = Output(Bool())
    val halt        = Output(Bool())
    val csr_op      = Output(UInt(3.W))
    val csr_src_imm = Output(Bool())
    val loadType      = Output(UInt(3.W))
    val loadUnsigned  = Output(Bool())
    val storeType     = Output(UInt(3.W))
  })

  val opcode = io.instruction(6, 0)
  val funct3 = io.instruction(14, 12)
  val funct7 = io.instruction(31, 25)

  val immGen = Module(new ImmGen())
  immGen.io.instruction := io.instruction

  // Default Control Signals
  io.regWrite    := false.B
  io.aluSrc      := false.B
  io.aluOp       := ALU_ADD
  io.imm         := 0.U
  io.memWrite    := false.B
  io.branch      := false.B
  io.memToReg    := false.B
  io.jump        := false.B
  io.jumpReg     := false.B
  io.auipc       := false.B
  io.halt        := false.B
  io.csr_op      := CSR_OP_NOP
  io.csr_src_imm := false.B
  io.loadType    := funct3
  io.loadUnsigned := false.B
  io.storeType   := funct3
  io.imm         := immGen.io.imm_i

  switch(opcode) {
    // 1. R-Type
    is("b0110011".U) {
      io.regWrite := true.B
      val isSub = funct7(5)
      switch(funct3) {
        is("b000".U) { io.aluOp := Mux(isSub, ALU_SUB, ALU_ADD) }
        is("b001".U) { io.aluOp := ALU_SLL }
        is("b010".U) { io.aluOp := ALU_SLT }
        is("b011".U) { io.aluOp := ALU_SLTU }
        is("b100".U) { io.aluOp := ALU_XOR }
        is("b101".U) { io.aluOp := Mux(isSub, ALU_SRA, ALU_SRL) }
        is("b110".U) { io.aluOp := ALU_OR }
        is("b111".U) { io.aluOp := ALU_AND }
      }
    }
    // 2. I-Type
    is("b0010011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.imm      := immGen.io.imm_i
      val isSRA = funct7(5)
      switch(funct3) {
        is("b000".U) { io.aluOp := ALU_ADD }
        is("b001".U) { io.aluOp := ALU_SLL }
        is("b010".U) { io.aluOp := ALU_SLT }
        is("b011".U) { io.aluOp := ALU_SLTU }
        is("b100".U) { io.aluOp := ALU_XOR }
        is("b101".U) { io.aluOp := Mux(isSRA, ALU_SRA, ALU_SRL) }
        is("b110".U) { io.aluOp := ALU_OR }
        is("b111".U) { io.aluOp := ALU_AND }
      }
    }
    // 3. Load
    is("b0000011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.memToReg := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_i
      when(funct3 === "b100".U || funct3 === "b101".U) {
        io.loadUnsigned := true.B
      }
    }
    // 4. Store
    is("b0100011".U) {
      io.memWrite := true.B
      io.aluSrc   := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_s
    }
    // 5. Branch
    is("b1100011".U) {
      io.branch   := true.B
      io.aluOp    := ALU_SUB
      io.imm      := immGen.io.imm_b
    }
    // 6. LUI
    is("b0110111".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      // FIX: Use COPY_B to ignore rs1 input
      io.aluOp    := ALU_COPY_B
      io.imm      := immGen.io.imm_u
    }
    // 7. AUIPC
    is("b0010111".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.auipc    := true.B
      io.aluOp    := ALU_ADD
      io.imm      := immGen.io.imm_u
    }
    // 8. JAL
    is("b1101111".U) {
      io.regWrite := true.B
      io.jump     := true.B
      io.imm      := immGen.io.imm_j
    }
    // 9. JALR
    is("b1100111".U) {
      io.regWrite := true.B
      io.jump     := true.B
      io.jumpReg  := true.B
      io.aluSrc   := true.B
      io.imm      := immGen.io.imm_i
      io.aluOp    := ALU_ADD
    }
    // 10. FENCE
    is("b0001111".U) { }
    // 11. SYSTEM
    is("b1110011".U) {
      val funct12_bit20 = io.instruction(20)
      when(funct3 === 0.U) {
        when(funct12_bit20 === 0.U) { io.halt := true.B }
          .elsewhen(funct12_bit20 === 1.U) { io.halt := true.B }
      } .otherwise {
        io.regWrite := true.B
        io.memToReg := false.B
        io.aluOp    := ALU_ADD
        io.csr_op   := funct3
        when(funct3 >= 4.U) {
          io.csr_src_imm := true.B
          io.aluSrc := true.B
          io.imm := Cat(0.U(27.W), io.instruction(19,15))
        } .otherwise {
          io.csr_src_imm := false.B
          io.aluSrc := false.B
        }
      }
    }
  }
}