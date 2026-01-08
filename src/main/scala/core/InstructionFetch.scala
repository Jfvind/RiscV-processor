// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

class InstructionFetch extends Module {
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

  // --- BLINK PROGRAM (Using only addi, sw, bge) ---
  val rom = VecInit(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON Value)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF Value)
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h03200293".U(32.W), // 12: addi x5, x0, 50     (x5 = 50, Delay Limit)
    // --- LOOP START (LED ON) ---
    "h0011a023".U(32.W), // 16: sw   x1, 0(x3)      (Turn LED ON: Mem[100] = 1)
    // --- DELAY 1 ---
    "h00000213".U(32.W), // 20: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 24: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 28: bge  x5, x4, -4     (If 50 >= x4, jump back to 24)
    // --- Turn OFF ---
    "h0021a023".U(32.W), // 32: sw   x2, 0(x3)      (Turn LED OFF: Mem[100] = 0)
    // --- DELAY 2 ---
    "h00000213".U(32.W), // 36: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 40: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 44: bge  x5, x4, -4     (If 50 >= x4, jump back to 40)
    // --- REPEAT ---
    // We use bge x0, x0, -32 to jump back to instruction 16.
    "hfe0050e3".U(32.W), // 48: bge x0, x0, -32     (Jump back to 16)
  )

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