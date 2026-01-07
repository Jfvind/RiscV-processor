// The Arithmetic Logic Unit. It takes two operands and an operation code (add, sub, xor, etc.) and outputs the result.
package core

import chisel3._
import chisel3.util._
import core.ALUConstants._

/*
Input A, Input B (efter forwarding MUXes!)
ALUOp control signal
Output: result, zero flag

OPCODE IN ALUConstants:
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

//TODO: Adder chain -> look forward, shifter: Barrel shifter, MUXLookup, 10-way mux...
class ALU extends Module {
  val io = IO(new Bundle {
    val alu_a   = Input(UInt(32.W)) // IN val 1 til ALU
    val alu_b   = Input(UInt(32.W)) // IN val 2 til ALU
    val alu_op = Input(UInt(4.W)) // opcode
    val result = Output(UInt(32.W)) //result after calc
    val zero   = Output(Bool()) // Zero Flag for Branches
  })

  // Shift amount: only bottom 5 bits as of 32-bit arch (RISC-V spec)
  val shamt = io.alu_b(4, 0)
  //Helper from ALUConstants that decides if we need sub op
  val useSub = isSub(io.alu_op)


  // =============== 1. SHARED ADDER WITH CARRY (ADD, SUB, SLT, SLTU) ===================

  // If sub high -> { B' } Else { B }
  val operand_b_mod = Mux(useSub, ~io.alu_b, io.alu_b)
  // Adder: Result = A + (B | ~B) + (1 hvis SUB, 0 hvis ADD)
  // .asUInt casts er necessary as + with Bool is not allowed
  val adder_full = io.alu_a +& operand_b_mod + useSub.asUInt //+& -> plus and extend
  val adder_result = adder_full(31,0) //bottom 32 bit -> res
  val carry_out = adder_full(32)


  // =============== 2. COMPARISON UNIT (SLT, SLTU) ===================================

  //SLTU
  // Adder shared with A-B -> A + ~B + 1
  // If carry out = 0 -> Borrow, thus A < B
  // if Carry = 1 -> No borrow and thus A >= B
  val less_unsigned = !carry_out

  //SLT
  // Logic: (A < B) <=> (SignBit_Result != Overflow_XOR_SignBits)
  //A < B signed true if -> {
  // 1. A negative and B positive}
  // 2. A, B same sign and A-B is negative
  val a_sign = io.alu_a(31)
  val b_sign = io.alu_b(31)
  val res_sign = adder_result(31)
  val less_signed = Mux(a_sign === b_sign,
    res_sign,   // If sign is same: Res sign decides
    a_sign)     // If sign different: A's sign decides (Neg < Pos)

  // ============== 3. RESULT MUX ======================================

  io.result := MuxLookup(io.alu_op, 0.U)(Seq(
    ALU_ADD  -> adder_result,
    ALU_SUB  -> adder_result,
    ALU_AND  -> (io.alu_a & io.alu_b),
    ALU_OR   -> (io.alu_a | io.alu_b),
    ALU_XOR  -> (io.alu_a ^ io.alu_b),
    ALU_SLL  -> (io.alu_a << shamt),
    ALU_SRL  -> (io.alu_a >> shamt),
    ALU_SRA  -> (io.alu_a.asSInt >> shamt).asUInt,
    ALU_SLT  -> less_signed.asUInt,   // Free due to adder logic
    ALU_SLTU -> less_unsigned.asUInt  // Free due to carry
  ))

  // Zero flag for BEQ
  io.zero := (io.result === 0.U)
}
