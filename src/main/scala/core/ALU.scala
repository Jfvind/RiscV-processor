/*
* Arithmetic Logic Unit (ALU):
* Performs 32-bit arithmetic (ADD, SUB) and logical (AND, OR, XOR) operations.
* It takes two operands and an ALU-Opcode from the ControlUnit to determine the operation.
* Outputs the result and branch flags (Zero , Lessthan) used for control flow logic.
* Type: Pure combinational logic.
*/
package core

import chisel3._
import chisel3.util._
import core.ALUConstants._ // ALUConstans is a dict which enable us to use mneomonic codes e.g., ALU_ADD instead of raw "magic numbers".

class ALU extends Module {
  val io = IO(new Bundle {
    // Inputs from Register File / Forwarding
    val alu_a   = Input(UInt(32.W)) 
    val alu_b   = Input(UInt(32.W)) 
    val alu_op = Input(UInt(4.W)) // Opcode from ControlUnit

    // Main result output after calulation
    val result = Output(UInt(32.W)) 

    // Branch flags used by ControlUnit for Logic
    val less_signed = Output(Bool()) // For BLT, BGE
    val less_unsigned = Output(Bool()) // For BLTU, BGEU
    val zero   = Output(Bool()) // Zero Flag for BEQ, BNE
  })

  // Shift Amount (shamt):
  // RISC-V 32-bit spec uses only the lower 5 bits for shift operations (range 0 - 31).
  val shamt = io.alu_b(4, 0)
  // Subtraction Check:
  // Helper from ALUConstants. True if operation is SUB, SLT, or SLTU.
  // Used to configure the adder for substraction (A - B).
  val useSub = isSub(io.alu_op)


  // =============== 1. SHARED ADDER WITH CARRY (ADD, SUB, SLT, SLTU) ===================
  // We reuse the adder for substraction to save area.
  // Logic: A - B is implemented as A + (~B) + 1 (Two's Complement).

  // Logic: If subtraction (useSub), invert B (~B). Else use B as is.
  val operand_b_mod = Mux(useSub, ~io.alu_b, io.alu_b)
  
  // Adder Calculation:
  // 1. "A + operand_b_mod" does the main math.
  // 2. "+ useSub.asUInt" adds 1 if we are subtracting (completing 2's complement).
  // 3. "+&" operator extends the result to 33 bits to capture Carry-Out (needed for SLTU).
  val adder_full = io.alu_a +& operand_b_mod + useSub.asUInt

  val adder_result = adder_full(31,0) // Lower 32 bits are the actual result
  val carry_out = adder_full(32)      // 33rd bit is the Carry-Out / Overflow from addition

  //================ 2. ZERO FLAG ==========================================================
  // Output true if the result is exactly 0. Used for BEQ/BNE branches.
  io.zero := (adder_result === 0.U)


  // =============== 2. COMPARISON UNIT (SLT, SLTU) ===================================

  // Unsigned Comparison (SLTU):
  // Using the carry-out from the adder (configured as subtractor).
  // CarryOut = 1 implies no borrow (A >= B).
  // CarryOut = 0 implies borrow (A < B).
  io.less_unsigned := !carry_out

 // Signed Comparison (SLT):
  // Must handle potential overflow when subtracting numbers with different signs.
  val a_sign = io.alu_a(31)
  val b_sign = io.alu_b(31)
  val res_sign = adder_result(31)

  io.less_signed := Mux(a_sign === b_sign,
    res_sign,   // Same signs: No overflow possible. Result sign determines less than.
    a_sign)     // Different signs: The number with the negative sign (1) is smaller.

  // ============== 3. RESULT MUX ======================================
  // Selects the final 32-bit output based on the alu_op signal.
  io.result := MuxLookup(io.alu_op, 0.U)(Seq(
    ALU_ADD  -> adder_result,
    ALU_SUB  -> adder_result,
    ALU_AND  -> (io.alu_a & io.alu_b),
    ALU_OR   -> (io.alu_a | io.alu_b),
    ALU_XOR  -> (io.alu_a ^ io.alu_b),
    ALU_SLL  -> (io.alu_a << shamt),
    ALU_SRL  -> (io.alu_a >> shamt),
    ALU_SRA  -> (io.alu_a.asSInt >> shamt).asUInt, 
    ALU_SLT  -> io.less_signed.asUInt,   // Free due to adder logic
    ALU_SLTU -> io.less_unsigned.asUInt  // Free due to carry
    ALU_LUI  -> io.alu_b
  ))
}
