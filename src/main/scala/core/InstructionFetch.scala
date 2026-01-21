// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.log2Ceil
import scala.io.Source

class InstructionFetch(program: Seq[UInt] = Seq(), programFile: String = "") extends Module {
  val io = IO(new Bundle {
    // Inputs from the Control Unit / ALU
    val jump_target_pc = Input(UInt(32.W))  // The address to jump/branch to
    val branch_taken      = Input(Bool())   // 0 = PC+4, 1 = Jump Target
    val stall          = Input(Bool())
    val halt           = Input(Bool())  // Halt signal to stop fetching instructions

    //BRanch prediction inputs
    val predict_taken     = Input(Bool())
    val predicted_target  = Input(UInt(32.W))

    // Outputs to the Decode Stage
    val pc             = Output(UInt(32.W)) // Current PC (needed for jumps)
    val instruction    = Output(UInt(32.W)) // The fetched instruction
  })

   // ======= START: LOAD PROGRAM/TEST LOGIC =======
  val finalProgram = if (program.nonEmpty) {
    // 1. Priority: Test-program from Programs.scala or tests
    program
  } else if (programFile.nonEmpty) {
    // 2. Priority: Load specific file from Top.scala / Benchmarks
    try {
      val source = Source.fromFile(programFile)
      val lines = source.getLines().toList
      source.close()
      lines.map(hexString =>
        if (hexString.trim.nonEmpty) ("h" + hexString.trim).U(32.W) else 0.U(32.W)
        )
    } catch {
      case e: Exception =>
        println(s"CRITICAL ERROR: Could not read the file '$programFile'. ($e)")
        List("00000013".U(32.W)) // NOP if error
    }
  } else {
    // 3. Fallback: if nothing has been specified
    println("WARNING: No specified program-sequence nor file-path. Using an empty ROM")
    List("00000013".U(32.W))
  }

  // Padding to avoid index-errors for very small programs
  val romContent = if (finalProgram.length < 4) finalProgram ++ Seq.fill(4)(0.U(32.W)) else finalProgram
  val rom = VecInit(romContent)
  val romSize = romContent.length

  // ======= END: LOAD PROGRAM/TEST LOGIC =======

  // Program Counter Register
  val pc = RegInit(0.U(32.W))
  val halted = RegInit(false.B)
  // Halt mechanism to prevent PC from running beyond program
  val maxPC = if (program.nonEmpty) (program.length * 4).U else 0x1000.U

  when(io.halt) {
    halted := true.B
  }
  
  // Check if we've reached the end of the program
  when(!halted && pc >= maxPC - 4.U) {
    halted := true.B
  }
  
  // PC update logic
  when(!halted && !io.stall) {
    // MuxCase vælger den første 'true' condition fra toppen af listen.
    val next_pc = MuxCase(pc + 4.U, Seq(
      io.branch_taken  -> io.jump_target_pc,   // 1. Prioritet: Faktisk branch/jump
      io.predict_taken -> io.predicted_target  // 2. Prioritet: Forudsigelse
    ))
    pc := next_pc
  }
  // If halted, PC doesn't change (stays at last instruction)

  // 1. Tag de relevante bits fra PC (sikrer mod warnings)
  // Vi bruger 'min' for at sikre os, at vi ikke crasher hvis programmet er meget lille
  val indexBits = log2Ceil(romSize.max(1))
  val safeIndex = (pc >> 2.U)(indexBits - 1, 0)

  // 2. Logic check for "Out of Bounds" (runtime sikkerhed)
  val pc_word_addr = pc >> 2.U
  val is_valid_addr = pc_word_addr < romSize.U(32.W)

  // 3. Fetch
  val fetchedInstr = Mux(is_valid_addr,
    rom(safeIndex),
    0x00000013.U) // NOP

  // Output the fetched instruction (or NOP if halted)
  io.instruction := Mux(halted, 0x00000013.U, fetchedInstr)

  // Output the current PC
  io.pc := pc
}