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

    // New ports for bootloader-writing
    val write_en       = Input(Bool())
    val write_addr     = Input(UInt(32.W))
    val write_data     = Input(UInt(32.W))

    // Outputs to the Decode Stage
    val pc             = Output(UInt(32.W)) // Current PC (needed for jumps)
    val instruction    = Output(UInt(32.W)) // The fetched instruction
  })

  // ======= START: NY HUKOMMELSES-LOGIK =======
  val ramSize = 17500
  val ram = SyncReadMem(ramSize, UInt(32.W))

  // 1. Priority: If a file-path is specified, read it to RAM
  if (programFile.nonEmpty) {
    loadMemoryFromFileInline(ram, programFile)
  }

  // 2. Priority: Test-program from Programs.scala or tests (Vektor)
  // This ensures that the old tests still work as intended
  val hasTestProgram = (program.nonEmpty).B
  val testRomContent = if (program.nonEmpty) program else Seq.fill(4)(0.U(32.W))
  val testRom = VecInit(testRomContent)
  
  // romSize bruges til halted-logic and out-of-bounds check
  val currentRomSize = if (program.nonEmpty) testRomContent.length.U else ramSize.U

  // Writelogic (used by the bootloader or by the self-patching test)
  when(io.write_en) {
    ram.write(io.write_addr >> 2.U, io.write_data)
  }
  // ======= END: NY HUKOMMELSES-LOGIK =======

  // Program Counter Register
  val pc = RegInit(0.U(32.W))
  // Halt mechanism to prevent PC from running beyond program
  val maxPC = currentRomSize * 4.U
  val halted = RegInit(false.B)

  // We calculate next_pc, before the PC-register is updated.
  // We need the next adress to start reading from SyncReadMem now, so that the next instr is ready in the next clockcycle.
  // If branch_taken is true, we jump. Otherwise, we move to the next instruction (PC + 4).
  val next_pc = Mux(io.branch_taken, io.jump_target_pc, pc + 4.U)
  
  // Check if we've reached the end of the program
  when(!halted && pc >= maxPC - 4.U) {
    halted := true.B
  }
  
  // PC update logic
  when(!halted && !io.stall) {
    pc := next_pc
  }.elsewhen(io.halt) {
    halted := true.B
  }
  // If halted, PC doesn't change (stays at last instruction)

  // ====== START: FETCH LOGIC ======
  // Check if we are in the RAM-area (0x8000+) or if we are using a test-vector
  val is_in_ram_range = pc >= 0x8000.U

  // Safety index for the test-vector
  val safeIndex = (pc >> 2.U)(log2Ceil(testRomContent.length.max(1)) - 1, 0)

  // 1. Fetch from RAM (used for benchmarks or bootloading)
  // to hit the right adress with syncedmem (1 cycle delay), we can read from the adress vi are
  // on our way to (next_pc).
  // We only read if the processor is not ideling
  val ramReadAddr = (next_pc - 0x8000.U) >> 2.U
  val instrFromRam = ram.read(ramReadAddr, !io.stall && !halted)

  // 2. Fetch from vector (used for old Scala tests)
  val instrFromRom = testRom(safeIndex)

  // 3. Choose the correct source:
  // If we are over 0x8000, we always read from RAM.
  // If a test-program is loaded we use the vector. Otherwise we use the bottom of the RAM (0x000+)
  val fetchedInstr = Mux(is_in_ram_range,
    instrFromRam,
    Mux(hasTestProgram, instrFromRom, ram.read(next_pc >> 2.U, !io.stall))
  )
  // ====== END: FETCH LOGIC ======
  io.instruction := Mux(halted, 0x00000013.U, fetchedInstr)
  io.pc := pc
}