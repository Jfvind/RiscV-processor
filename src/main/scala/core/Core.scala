// This is the "motherboard." It instantiates the ALU, Register File, and Control Unit and wires them together.
package core

import chisel3._

/*
instantierer:

IF, ControlUnit, RegFile, ALU moduler
Pipeline registers (IF/ID, ID/EX, EX/MEM, MEM/WB)
Forwarding unit
Hazard detection unit (til load-use hazards senere)
Alle MUXes mellem stages
 */

class Core extends Module {
  val io = IO(new Bundle {
    // Debug outputs for simulation
    val pc_out      = Output(UInt(32.W))
    val instruction = Output(UInt(32.W)) // Helpful to see what we fetched
    val alu_res     = Output(UInt(32.W))
    val led      = Output(UInt(1.W))
  })

  // 1. Instantiate Modules
  val fetch   = Module(new InstructionFetch())
  val decode  = Module(new ControlUnit()) // Contains ImmGen
  val regFile = Module(new RegisterFile())
  val alu     = Module(new ALU())

  
  // Core kender ikke forskel på RAM og LED - det styrer dette modul.
  val memIO   = Module(new MemoryMapping())

  // 2. Wiring Instruction Fetch
  // Send instruction from Fetch to Control Unit
  decode.io.instruction := fetch.io.instruction

  // --- BRANCH LOGIC (NYT) ---
  // Vi skal hoppe, hvis ControlUnit siger "branch" OG betingelsen er opfyldt.
  // For BGE (Branch Greater Equal) bruger vi ALU operationen SLT (Set Less Than).
  // Hvis (rs1 < rs2) giver SLT 1.
  // Hvis (rs1 >= rs2) giver SLT 0.
  // Så for BGE skal vi hoppe, hvis ALU resultatet er 0 (dvs. IKKE less than).
  val branch_taken = decode.io.branch && (alu.io.result === 0.U)

  fetch.io.pc_select      := branch_taken
  fetch.io.jump_target_pc := fetch.io.pc + decode.io.imm
  
  // 3. Wiring Register File
  // RS1 is bits [19:15], RS2 is bits [24:20], RD is bits [11:7]
  regFile.io.rs1_addr := fetch.io.instruction(19, 15)
  regFile.io.rs2_addr := fetch.io.instruction(24, 20)
  regFile.io.rd_addr  := fetch.io.instruction(11, 7)
  
  // Connect Control Signals to Register File
  regFile.io.reg_write := decode.io.regWrite
  
  // Write Back: The result of the ALU goes back into the Register File data input
  regFile.io.rd_data   := alu.io.result

  // 4. Wiring ALU
  alu.io.alu_op := decode.io.aluOp
  alu.io.alu_a  := regFile.io.rs1_data // Note: Changed from op_a to alu_a to match your ALU.scala

  // MUX for ALU Operand B (Register vs Immediate)
  // If aluSrc is true, use Immediate. Else use rs2_data.
  alu.io.alu_b  := Mux(decode.io.aluSrc, decode.io.imm, regFile.io.rs2_data)

  // 5. Wiring Memory System (NYT)
  // Vi forbinder ALU og RegisterFile til vores nye MemoryMapping modul
  memIO.io.address   := alu.io.result        // Adressen fra ALU (f.eks. 100 til LED)
  memIO.io.writeData := regFile.io.rs2_data  // Data fra register (til sw)
  memIO.io.memWrite  := decode.io.memWrite   // Control signal

  io.led := memIO.io.led 
  
  // 6. Debug Outputs
  io.pc_out      := fetch.io.pc
  io.instruction := fetch.io.instruction
  io.alu_res     := alu.io.result
}