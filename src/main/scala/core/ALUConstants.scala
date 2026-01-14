/*
* Dictionary for ALU operations to avoid magic numbers.
* Makes the code readable e.g., using ALU_ADD instead of just '0'
*/
package core

import chisel3._

object ALUConstants {
  val OP_WIDTH = 4.W // Defining the width of the control signal to 4 bits. 

  // Opcode mappings:
  // MSB (bit 3) = subtraction mode
  val ALU_ADD  = "b0000".U  // 0
  val ALU_AND  = "b0001".U  // 1
  val ALU_OR   = "b0010".U  // 2
  val ALU_XOR  = "b0011".U  // 3
  val ALU_SLL  = "b0100".U  // 4
  val ALU_SRL  = "b0101".U  // 5
  val ALU_SRA  = "b0110".U  // 6
  val ALU_LUI  = "b0111".U  // 7

  // Subtraction ops (bit 3 = 1)
  val ALU_SUB  = "b1000".U  // 8
  val ALU_SLT  = "b1100".U  // 12
  val ALU_SLTU = "b1101".U  // 13

  def isSub(op: UInt): Bool = {
    op(3)  // Wiring with this approach, should be faster than with OR gates.
  }
}