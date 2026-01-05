package riscv

import chisel3._

/**
 * A simple pass-through module to verify Chisel setup
 */
class Example extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })
  
  io.out := io.in
}
