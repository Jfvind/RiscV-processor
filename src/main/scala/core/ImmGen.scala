package core

import chisel3._
import chisel3.util._

class ImmGen extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))
    val imm_i       = Output(UInt(32.W))
    val imm_s       = Output(UInt(32.W))
    val imm_b       = Output(UInt(32.W))
    val imm_u       = Output(UInt(32.W))
    val imm_j       = Output(UInt(32.W))
  })

  val sign = io.instruction(31)

  // I-Type
  io.imm_i := Cat(Fill(20, sign), io.instruction(31, 20)) // Using sign-extension to match 32-bit length

  // S-Type
  io.imm_s := Cat(Fill(20, sign), io.instruction(31, 25), io.instruction(11, 7))

  // B-Type
  io.imm_b := Cat(Fill(19, sign), io.instruction(31), io.instruction(7), io.instruction(30, 25), io.instruction(11, 8), 0.U(1.W))

  // U-Type
  io.imm_u := Cat(io.instruction(31, 12), 0.U(12.W))

  // J-Type
  io.imm_j := Cat(Fill(11, sign), io.instruction(31), io.instruction(19, 12), io.instruction(20), io.instruction(30, 21), 0.U(1.W))
}