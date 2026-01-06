// The Arithmetic Logic Unit. It takes two operands and an operation code (add, sub, xor, etc.) and outputs the result.
package core

import chisel3._

/*
Input A, Input B (efter forwarding MUXes!)
ALUOp control signal
Output: result, zero flag (til branches senere)

OPCODE:
0 -> ADD
1 -> SUB
2 -> AND
3 -> OR
4 -> XOR
5 -> SLL  (shift left logical)
6 -> SRL  (shift right logical)
7 -> SRA  (shift right arithmetic)
8 -> SLT  (set less than - signed)
9 -> SLTU (set less than - unsigned)
 */

class ALU extends Module {
  val io = IO(new Bundle {
    val op_a   = Input(UInt(32.W))
    val op_b   = Input(UInt(32.W))
    val alu_op = Input(UInt(4.W))
    val result = Output(UInt(32.W))
  })
}
