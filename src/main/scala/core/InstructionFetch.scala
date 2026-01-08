// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

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

  // This embeds the hex file data directly into the generated circuit logic,
  // which allows the default simulator to read it.
  //loadMemoryFromFileInline(mem, "src/main/resources/program.mem") // for test
  loadMemoryFromFileInline(mem, "program.mem") // for FPGA
/*
  // --- CORRECTED BLINK PROGRAM (Using only addi, sw, bge) ---
  val rom = VecInit(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON Value)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF Value)
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h03200293".U(32.W), // 12: addi x5, x0, 50     (x5 = 50, Delay Limit)

    // --- LOOP START (Turn ON) ---
    "h0011a023".U(32.W), // 16: sw   x1, 0(x3)      (Turn LED ON: Mem[100] = 1)
    
    // --- DELAY 1 ---
    "h00000213".U(32.W), // 20: addi x4, x0, 0      (Reset Counter x4 = 0)
    // Loop Label 1:
    "h00120213".U(32.W), // 24: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 28: bge  x5, x4, -4     (If 50 >= x4, jump back to 24)

    // --- Turn OFF ---
    "h0021a023".U(32.W), // 32: sw   x2, 0(x3)      (Turn LED OFF: Mem[100] = 0)

    // --- DELAY 2 ---
    "h00000213".U(32.W), // 36: addi x4, x0, 0      (Reset Counter x4 = 0)
    // Loop Label 2:
    "h00120213".U(32.W), // 40: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 44: bge  x5, x4, -4     (If 50 >= x4, jump back to 40)

    // --- REPEAT ---
    // We use bge x0, x0, -32 to jump back to instruction 16.
    // Since 0 >= 0 is always true, this is an unconditional jump.
    "hfe0050e3".U(32.W), // 48: bge x0, x0, -32     (Jump back to 16)
    
    // --- PADDING ---
    // Replaced 'nop' with 'addi x0, x0, 0' (0x00000013)
    "h00000013".U(32.W), 
    "h00000013".U(32.W)
  )*/

  // Fetch instruction
  // The PC is in bytes, but memory is indexed by words (32-bit).
  // So we divide PC by 4 (or shift right by 2) to get the index.
  io.instruction := mem(pc >> 2)

  //val romIndex = (pc >> 2)(5,0)
  //io.instruction := rom(romIndex)

  // Output the current PC
  io.pc := pc
}