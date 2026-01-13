// This instantiates the ALU, Register File, Control Unit, MemoryMapping, and InstructionFetch and wires them together.
package core

import chisel3._
import chisel3.util._

class Core(program: Seq[UInt]) extends Module {
  val io = IO(new Bundle {
    // Debug outputs for simulation
    val pc_out      = Output(UInt(32.W))
    val instruction = Output(UInt(32.W)) // Helpful to see what we fetched
    val alu_res     = Output(UInt(32.W))
    val led         = Output(UInt(1.W))

    // Uart
    val uartData    = Output(UInt(32.W))
    val uartAddr    = Output(UInt(32.W))
    val uartValid   = Output(Bool())
  })

  // Instantiate Modules
  val fetch      = Module(new InstructionFetch(program))
  val decode     = Module(new ControlUnit())   // Contains ImmGen
  val regFile    = Module(new RegisterFile())
  val alu        = Module(new ALU())           // Contains ALUConstants
  val memIO      = Module(new MemoryMapping()) // Decides RAM or LED (Contains DataMemory)
  val forwarding = Module(new ForwardingUnit())
  val hazard     = Module(new HazardUnit())

  // ==============================================================================
  // IF STAGE (Instruction Fetch)
  // ==============================================================================
  
  // IF/ID Pipeline Register
  val if_id_pc    = RegInit(0.U(32.W))
  val if_id_instr = RegInit(0.U(32.W))

  // Update IF/ID Register
  when(hazard.io.flush) {
    if_id_pc    := 0.U
    if_id_instr := 0.U // Flush to NOP
  } .otherwise {
    if_id_pc    := fetch.io.pc
    if_id_instr := fetch.io.instruction
  }

  // ==============================================================================
  // ID STAGE (Instruction Decode)
  // ==============================================================================
  
  decode.io.instruction := if_id_instr
  
  // Register File Read
  regFile.io.rs1_addr := if_id_instr(19, 15)
  regFile.io.rs2_addr := if_id_instr(24, 20)
  // rd_addr is if_id_instr(11, 7)

  // ID/EX Pipeline Register Bundle
  class ID_EX_Bundle extends Bundle {
    val pc       = UInt(32.W)
    val rs1_data = UInt(32.W)
    val rs2_data = UInt(32.W)
    val imm      = UInt(32.W)
    val rs1_addr = UInt(5.W)
    val rs2_addr = UInt(5.W)
    val rd_addr  = UInt(5.W)
    // Control Signals
    val regWrite = Bool()
    val memWrite = Bool()
    val branch   = Bool()
    val aluSrc   = Bool()
    val aluOp    = UInt(4.W)
  }
  val id_ex = RegInit(0.U.asTypeOf(new ID_EX_Bundle))

  // Update ID/EX Register
  when(hazard.io.flush) {
    id_ex := 0.U.asTypeOf(new ID_EX_Bundle) // Flush Control Signals
  } .otherwise {
    id_ex.pc       := if_id_pc
    id_ex.rs1_data := regFile.io.rs1_data
    id_ex.rs2_data := regFile.io.rs2_data
    id_ex.imm      := decode.io.imm
    id_ex.rs1_addr := if_id_instr(19, 15)
    id_ex.rs2_addr := if_id_instr(24, 20)
    id_ex.rd_addr  := if_id_instr(11, 7)
    // Control Signals
    id_ex.regWrite := decode.io.regWrite
    id_ex.memWrite := decode.io.memWrite
    id_ex.branch   := decode.io.branch
    id_ex.aluSrc   := decode.io.aluSrc
    id_ex.aluOp    := decode.io.aluOp
  }

  // ==============================================================================
  // EX STAGE (Execute)
  // ==============================================================================

  // EX/MEM Pipeline Register Bundle
  class EX_MEM_Bundle extends Bundle {
    val alu_result = UInt(32.W)
    val rs2_data   = UInt(32.W) // For Store
    val rd_addr    = UInt(5.W)
    val regWrite   = Bool()
    val memWrite   = Bool()
  }
  val ex_mem = RegInit(0.U.asTypeOf(new EX_MEM_Bundle))

  // MEM/WB Pipeline Register Bundle (Defined here for Forwarding reference)
  class MEM_WB_Bundle extends Bundle {
    val result   = UInt(32.W)
    val rd_addr  = UInt(5.W)
    val regWrite = Bool()
  }
  val mem_wb = RegInit(0.U.asTypeOf(new MEM_WB_Bundle))

  // --- Forwarding Logic ---
  forwarding.io.id_ex_rs1       := id_ex.rs1_addr
  forwarding.io.id_ex_rs2       := id_ex.rs2_addr
  forwarding.io.ex_mem_rd       := ex_mem.rd_addr
  forwarding.io.ex_mem_regWrite := ex_mem.regWrite
  forwarding.io.mem_wb_rd       := mem_wb.rd_addr
  forwarding.io.mem_wb_regWrite := mem_wb.regWrite

  // Muxes for ALU Operands (Forwarding)
  val forwardA_data = MuxLookup(forwarding.io.forwardA, id_ex.rs1_data)(Seq(
    0.U -> id_ex.rs1_data,
    1.U -> mem_wb.result,      // Forward from WB
    2.U -> ex_mem.alu_result   // Forward from MEM
  ))

  val forwardB_data = MuxLookup(forwarding.io.forwardB, id_ex.rs2_data)(Seq(
    0.U -> id_ex.rs2_data,
    1.U -> mem_wb.result,
    2.U -> ex_mem.alu_result
  ))

  // ALU Connections
  alu.io.alu_op := id_ex.aluOp
  alu.io.alu_a  := forwardA_data
  alu.io.alu_b  := Mux(id_ex.aluSrc, id_ex.imm, forwardB_data)

  // Branch Logic (Resolved in EX stage)
  val branch_taken = id_ex.branch && !alu.io.less_signed
  
  // Update Fetch Unit
  fetch.io.branch_taken   := branch_taken
  fetch.io.jump_target_pc := id_ex.pc + id_ex.imm

  // Update Hazard Unit
  hazard.io.branch_taken := branch_taken

  // Update EX/MEM Register
  ex_mem.alu_result := alu.io.result
  ex_mem.rs2_data   := forwardB_data // Store data (must be forwarded version)
  ex_mem.rd_addr    := id_ex.rd_addr
  ex_mem.regWrite   := id_ex.regWrite
  ex_mem.memWrite   := id_ex.memWrite

  // ==============================================================================
  // MEM STAGE (Memory Access)
  // ==============================================================================

  memIO.io.address   := ex_mem.alu_result
  memIO.io.writeData := ex_mem.rs2_data
  memIO.io.memWrite  := ex_mem.memWrite

  // Update MEM/WB Register
  mem_wb.result   := ex_mem.alu_result // Pass ALU result (No Load support yet) =!!!!=
  mem_wb.rd_addr  := ex_mem.rd_addr
  mem_wb.regWrite := ex_mem.regWrite

  // ==============================================================================
  // WB STAGE (Write Back)
  // ==============================================================================

  regFile.io.rd_addr   := mem_wb.rd_addr
  regFile.io.rd_data   := mem_wb.result
  regFile.io.reg_write := mem_wb.regWrite

  // ==============================================================================
  // DEBUG OUTPUTS
  // ==============================================================================
  io.pc_out      := if_id_pc
  io.instruction := if_id_instr
  io.alu_res     := ex_mem.alu_result

  io.led         := memIO.io.led
  io.uartData    := memIO.io.uartData
  io.uartAddr    := memIO.io.uartAddr
  io.uartValid   := memIO.io.uartValid
}