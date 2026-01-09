// This instantiates the ALU, Register File, Control Unit, MemoryMapping, and InstructionFetch and wires them together.
package core

import chisel3._

class Core extends Module {
  val io = IO(new Bundle {
    // Debug outputs for simulation
    val pc_out      = Output(UInt(32.W))
    val instruction = Output(UInt(32.W)) // Helpful to see what we fetched
    val alu_res     = Output(UInt(32.W))
    val led         = Output(UInt(1.W))
  })

  // Instantiate Modules
  val fetch   = Module(new InstructionFetch())
  val decode  = Module(new ControlUnit())   // Contains ImmGen
  val regFile = Module(new RegisterFile())
  val alu     = Module(new ALU())           // Contains ALUConstants
  val memIO   = Module(new MemoryMapping()) // Decides RAM or LED (Contains DataMemory)

  // Wiring Instruction Fetch
  decode.io.instruction := fetch.io.instruction // ControlUnit recieves instruction from fetch

  // --- BRANCH LOGIC ---
  // Branch if ControlUnit says "branch" AND the given condition is true.
  // BGE uses ALU operation SLT (Set Less Than).
  // If (rs1 < rs2) then SLT = 1.
  // If (rs1 >= rs2) then SLT = 0. (we wan't this since we count up)
  val branch_taken = decode.io.branch && !alu.io.less_signed
  fetch.io.branch_taken      := branch_taken             // Fetch is told whether to branch or not
  fetch.io.jump_target_pc := fetch.io.pc + decode.io.imm // Branch adress
  
  // Wiring Register File
  regFile.io.rs1_addr := fetch.io.instruction(19, 15) // RS1 is bits [19:15]
  regFile.io.rs2_addr := fetch.io.instruction(24, 20) // RS2 is bits [24:20]
  regFile.io.rd_addr  := fetch.io.instruction(11, 7)  // RD is bits [11:7]
  
  regFile.io.reg_write := decode.io.regWrite // Are we writing to a register
  
  regFile.io.rd_data   := alu.io.result      // Write Back: The result of the ALU goes back into the Register File data input

  // Wiring ALU
  alu.io.alu_op := decode.io.aluOp     // Opcode
  alu.io.alu_a  := regFile.io.rs1_data // Operand 1
  // If aluSrc is true, use Immediate. Else use rs2_data.
  alu.io.alu_b  := Mux(decode.io.aluSrc, decode.io.imm, regFile.io.rs2_data) // MUX for ALU Operand B (Register vs Immediate)

  // Wiring Memory System
  // Connecting ALU and RegisterFile with MemoryMapping module
  memIO.io.address   := alu.io.result        // Adress from ALU (e.g. 100 for LED)
  memIO.io.writeData := regFile.io.rs2_data  // Data from register (for sw)
  memIO.io.memWrite  := decode.io.memWrite   // Control signal

  io.led := memIO.io.led // Value of LED address
  
  // Debug Outputs
  io.pc_out      := fetch.io.pc
  io.instruction := fetch.io.instruction
  io.alu_res     := alu.io.result
}