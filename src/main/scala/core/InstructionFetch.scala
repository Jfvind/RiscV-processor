// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class InstructionFetch extends Module {
  val io = IO(new Bundle {
    // Inputs from the Control Unit / ALU (Backend)
    val jump_target_pc = Input(UInt(32.W)) // The address to jump/branch to
    val pc_select      = Input(Bool())     // 0 = PC+4, 1 = Jump Target

    // Outputs to the Decode Stage
    val pc             = Output(UInt(32.W)) // Current PC (needed for relative jumps)
    val instruction    = Output(UInt(32.W)) // The fetched instruction
  })

  // 1. Program Counter Register
  // Init to 0. In real hardware, this might reset to a specific boot address.
  val pc = RegInit(0.U(32.W))

  // 2. Next PC Logic
  // If pc_select is true, we jump. Otherwise, we move to the next instruction (PC + 4).
  val next_pc = Mux(io.pc_select, io.jump_target_pc, pc + 4.U)
  pc := next_pc

  // 3. Instruction Memory (16KB size example)
  // We use a synchronous memory (SyncReadMem) or combinational (Mem).
  // For simple single-cycle/5-stage, combinational read (Mem) is easier to reason about initially.
  // 16KB = 4096 words of 32 bits
  val mem = Mem(4096, UInt(32.W))

  // Load hex file for simulation/FPGA initialization
  // You must place a file named "program.hex" in your root directory or src/main/resources
  loadMemoryFromFile(mem, "src/main/resources/program.hex")

  // Fetch instruction
  // The PC is in bytes, but memory is indexed by words (32-bit).
  // So we divide PC by 4 (or shift right by 2) to get the index.
  io.instruction := mem(pc >> 2)

  // Output the current PC
  io.pc := pc
}