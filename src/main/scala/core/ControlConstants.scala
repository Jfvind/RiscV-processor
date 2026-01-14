package core

import chisel3._
import chisel3.util._

object ControlConstants {
  // ALU Op Type -> ALU Decoder)
  val ALU_OP_MEM    = "b00".U // LW/SW (Add)
  val ALU_OP_BRANCH = "b01".U // BEQ/BNE/BLT... (Sub/Slt)
  val ALU_OP_RTYPE  = "b10".U // ADD/SUB/XOR... (Funct3/7)
  val ALU_OP_ITYPE  = "b11".U // ADDI/XORI...   (Funct3)

  // Immediate Selection Sendes til ImmGen i datapath
  val IMM_I = "b000".U
  val IMM_S = "b001".U
  val IMM_B = "b010".U
  val IMM_U = "b011".U
  val IMM_J = "b100".U
}