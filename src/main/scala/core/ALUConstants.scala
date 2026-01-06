package core

import chisel3._

object ALUConstants {
  val OP_WIDTH = 4.W

  // U(OP_WIDTH) for locking bit width.
  val ALU_ADD  = 0.U(OP_WIDTH)
  val ALU_SUB  = 1.U(OP_WIDTH)
  val ALU_AND  = 2.U(OP_WIDTH)
  val ALU_OR   = 3.U(OP_WIDTH)
  val ALU_XOR  = 4.U(OP_WIDTH)
  val ALU_SLL  = 5.U(OP_WIDTH)
  val ALU_SRL  = 6.U(OP_WIDTH)
  val ALU_SRA  = 7.U(OP_WIDTH)
  val ALU_SLT  = 8.U(OP_WIDTH)
  val ALU_SLTU = 9.U(OP_WIDTH)

  // Helper func: Do we need to substract?
  //This is used to reduce area of the ALU utilizing boolean alg. IE: A + B and A - B can utilize same adder,
  //as A - B = A + (B')
  def isSub(op: UInt): Bool = {
    op === ALU_SUB || op === ALU_SLT || op === ALU_SLTU
  }
}