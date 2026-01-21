package core
import chisel3._

object ALUConstants {
  val OP_WIDTH = 4.W
  val ALU_ADD  = "b0000".U
  val ALU_AND  = "b0001".U
  val ALU_OR   = "b0010".U
  val ALU_XOR  = "b0011".U
  val ALU_SLL  = "b0100".U
  val ALU_SRL  = "b0101".U
  val ALU_SRA  = "b0110".U

  val ALU_COPY_B = "b0111".U

  val ALU_SUB  = "b1000".U
  val ALU_SLT  = "b1100".U
  val ALU_SLTU = "b1101".U

  def isSub(op: UInt): Bool = op(3)
}