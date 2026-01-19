// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.log2Ceil

class InstructionFetch(program: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    // Inputs from the Control Unit / ALU
    val jump_target_pc = Input(UInt(32.W))  // The address to jump/branch to
    val branch_taken      = Input(Bool())   // 0 = PC+4, 1 = Jump Target
    val stall          = Input(Bool())
    val halt           = Input(Bool())  // Halt signal to stop fetching instructions

    // Outputs to the Decode Stage
    val pc             = Output(UInt(32.W)) // Current PC (needed for jumps)
    val instruction    = Output(UInt(32.W)) // The fetched instruction
  })

  // Program Counter Register
  val pc = RegInit(0.U(32.W))
  // Halt mechanism to prevent PC from running beyond program
  val maxPC = (program.length * 4).U
  val halted = RegInit(false.B)
  
  // Check if we've reached the end of the program
  when(!halted && pc >= maxPC - 4.U) {
    halted := true.B
  }
  
  // PC update logic
  when(!halted && !io.stall) {
    // If branch_taken is true, we jump. Otherwise, we move to the next instruction (PC + 4).
    val next_pc = Mux(io.branch_taken, io.jump_target_pc, pc + 4.U)
    pc := next_pc
  }.elsewhen(io.halt) {
    halted := true.B
  }
  // If halted, PC doesn't change (stays at last instruction)

  // --- PIPELINE & UART TEST ROM ---
  val rom = VecInit(program)
  // Beregn hvor mange bits der faktisk skal bruges til dette specifikke program
  val romSize = program.length
  val indexBits = log2Ceil(romSize.max(1))

  // 1. Tag de relevante bits fra PC (sikrer mod warnings)
  // Vi bruger 'min' for at sikre os, at vi ikke crasher hvis programmet er meget lille
  val safeIndex = (pc >> 2.U)(indexBits - 1, 0)

  // 2. Logic check for "Out of Bounds" (runtime sikkerhed)
  val pc_word_addr = pc >> 2.U
  val is_valid_addr = pc_word_addr < romSize.U(32.W)

  // 3. Fetch
  val fetchedInstr = Mux(is_valid_addr,
    rom(safeIndex),
    0x00000013.U) // NOP
  
  io.instruction := Mux(halted, 0x00000013.U, fetchedInstr)

  // Output the current PC
  io.pc := pc
}