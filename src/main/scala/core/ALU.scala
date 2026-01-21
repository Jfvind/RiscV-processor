package core
import chisel3._
import chisel3.util._
import core.ALUConstants._

class ALU extends Module {
  val io = IO(new Bundle {
    val alu_a   = Input(UInt(32.W))
    val alu_b   = Input(UInt(32.W))
    val alu_op  = Input(UInt(4.W))
    val result  = Output(UInt(32.W))
    val less_signed   = Output(Bool())
    val less_unsigned = Output(Bool())
    val zero          = Output(Bool())
  })

  val shamt = io.alu_b(4, 0)
  val useSub = isSub(io.alu_op)

  val operand_b_mod = Mux(useSub, ~io.alu_b, io.alu_b)
  val adder_full = io.alu_a +& operand_b_mod + useSub.asUInt
  val adder_result = adder_full(31,0)
  val carry_out = adder_full(32)

  io.less_unsigned := !carry_out
  val a_sign = io.alu_a(31)
  val b_sign = io.alu_b(31)
  val res_sign = adder_result(31)
  io.less_signed := Mux(a_sign === b_sign, res_sign, a_sign)

  io.result := MuxLookup(io.alu_op, 0.U)(Seq(
    ALU_ADD    -> adder_result,
    ALU_SUB    -> adder_result,
    ALU_AND    -> (io.alu_a & io.alu_b),
    ALU_OR     -> (io.alu_a | io.alu_b),
    ALU_XOR    -> (io.alu_a ^ io.alu_b),
    ALU_SLL    -> (io.alu_a << shamt),
    ALU_SRL    -> (io.alu_a >> shamt),
    ALU_SRA    -> (io.alu_a.asSInt >> shamt).asUInt,
    ALU_SLT    -> io.less_signed.asUInt,
    ALU_SLTU   -> io.less_unsigned.asUInt,
    ALU_COPY_B -> io.alu_b // Must be present!
  ))

  io.zero := (io.result === 0.U)
}