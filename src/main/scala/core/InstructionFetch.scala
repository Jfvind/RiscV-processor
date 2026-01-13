// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

class InstructionFetch(program: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    // Inputs from the Control Unit / ALU
    val jump_target_pc = Input(UInt(32.W))  // The address to jump/branch to
    val branch_taken      = Input(Bool())   // 0 = PC+4, 1 = Jump Target

    // Outputs to the Decode Stage
    val pc             = Output(UInt(32.W)) // Current PC (needed for jumps)
    val instruction    = Output(UInt(32.W)) // The fetched instruction
  })

  // Program Counter Register
  val pc = RegInit(0.U(32.W))
  // If branch_taken is true, we jump. Otherwise, we move to the next instruction (PC + 4).
  val next_pc = Mux(io.branch_taken, io.jump_target_pc, pc + 4.U)
  pc := next_pc

  //============= Work in progress!!===================================================================
  // Instruction Memory (16KB size example)
  // We use a synchronous memory (SyncReadMem) or combinational (Mem).
  // 16KB = 4096 words of 32 bits
  //val mem = Mem(4096, UInt(32.W))
  //loadMemoryFromFileInline(mem, "src/main/resources/program.mem") // for test
  //===================================================================================================

  // --- PIPELINE & UART TEST ROM ---
  val rom = VecInit(program)

  //================= Work in progress!!===================================
  // Fetch instruction
  // Divide PC by 4 to get the index.
  //io.instruction := mem(pc >> 2)
  //=======================================================================

  val romIndex = (pc >> 2)(3,0) // Choose relavant bits to avoid index out of bounds
  io.instruction := rom(romIndex)

  // Output the current PC
  io.pc := pc
}