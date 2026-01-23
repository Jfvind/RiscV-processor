// This instantiates the ALU, Register File, Control Unit, MemoryMapping, and InstructionFetch and wires them together.
/*package core

import chisel3._
import chisel3.util._

class Core(program: Seq[UInt] = Seq(), programFile: String = "") extends Module {
  val io = IO(new Bundle {
    // Debug outputs for simulation
    val pc_out      = Output(UInt(32.W))
    val instruction = Output(UInt(32.W)) // Helpful to see what we fetched
    val alu_res     = Output(UInt(32.W))
    val led         = Output(UInt(1.W))
    // Uart debug
    val uartData    = Output(UInt(8.W))
    val uartAddr    = Output(UInt(32.W))
    val uartValid   = Output(Bool())

    // Uart prod
    val tx          = Output(Bool())

    // ==================================================================
    // Debug Access Ports (Required for Diagnostic Tests)
    // ==================================================================
    val debug_x1    = Output(UInt(32.W)) // ra
    val debug_x2    = Output(UInt(32.W)) // sp
    val debug_x3    = Output(UInt(32.W))
    val debug_x4    = Output(UInt(32.W))
    val debug_x10   = Output(UInt(32.W)) // a0
    val debug_x11   = Output(UInt(32.W)) // a1
    val debug_x12   = Output(UInt(32.W)) // a2
    val debug_x13   = Output(UInt(32.W)) // a3
    val debug_x14   = Output(UInt(32.W)) // a4

    // Debug Access to Pipeline State
    val debug_stall        = Output(Bool())
    val debug_flush        = Output(Bool())
    val debug_forwardA     = Output(UInt(2.W))
    val debug_forwardB     = Output(UInt(2.W))
    val debug_branch_taken = Output(Bool())

    // Debug Access to CSRs
    val debug_mcycle       = Output(UInt(32.W))
    // ==================================================================
  })

  // Instantiate Modules
  val fetch      = Module(new InstructionFetch(program, programFile))
  val decode     = Module(new ControlUnit())   // Contains ImmGen
  val regFile    = Module(new RegisterFile())
  val alu        = Module(new ALU())           // Contains ALUConstants
  //val aluDecoder = Module(new ALUDecoder())
  val memIO      = Module(new MemoryMapping()) // Decides RAM or LED (Contains DataMemory)
  val forwarding = Module(new ForwardingUnit())
  val hazard     = Module(new HazardUnit())
  //val serialPort = Module(new Serialport())
  //val uart       = Module(new BufferedTx(100000000, 115200))
  val uart         = Module(new BufferedTx(25000000, 115200))
  val csrModule  = Module(new CSRModule())
  val branchPredictor = Module(new BranchPredictor())

  // ==============================================================================
  // IF STAGE (Instruction Fetch)
  // ==============================================================================

  // IF/ID Pipeline Register
  val if_id_pc    = RegInit(0.U(32.W))
  val if_id_instr = RegInit(0.U(32.W))

  // Update IF/ID Register
  when(hazard.io.flush) {
    if_id_pc    := 0.U
    if_id_instr := 0x00000013.U//NOP
  }.elsewhen(!hazard.io.stall) {
    // Only update if NOT stalling
    if_id_pc    := fetch.io.pc
    if_id_instr := fetch.io.instruction
  }
  // If stalling, if_id registers keep their current values (no assignment)


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
    val alu_op   = UInt(4.W) // Output of ALUDecoder
    //val tx       = Bool()
    // Control Signals
    val regWrite = Bool()
    val memWrite = Bool()
    val branch   = Bool()
    val aluSrc   = Bool()
    //val aluOp    = UInt(4.W) // Input of
    val funct3   = UInt(3.W)
    val funct7   = UInt(7.W)
    val memToReg = Bool()
    val jump     = Bool()
    val jumpReg  = Bool()
    val auipc    = Bool()
    val halt     = Bool()
    // CSR Signals
    val csr_op       = UInt(3.W)   //
    val csr_src_imm  = Bool()      //
    val csr_addr     = UInt(12.W)  //
    // Load/Store Signals
    val loadType      = UInt(3.W)
    val loadUnsigned  = Bool()
    val storeType     = UInt(3.W)
  }
  val id_ex = RegInit(0.U.asTypeOf(new ID_EX_Bundle))

  // Update ID/EX Register
  when(hazard.io.flush || hazard.io.stall) {
    id_ex := 0.U.asTypeOf(new ID_EX_Bundle)
  } .otherwise {
    id_ex.pc       := if_id_pc
    id_ex.rs1_data := regFile.io.rs1_data
    id_ex.rs2_data := regFile.io.rs2_data
    id_ex.imm      := decode.io.imm
    id_ex.rs1_addr := if_id_instr(19, 15)
    id_ex.rs2_addr := if_id_instr(24, 20)
    id_ex.rd_addr  := if_id_instr(11, 7)
    id_ex.alu_op   := decode.io.aluOp
    //id_ex.tx       := serialPort.io.tx
    // Control Signals
    id_ex.regWrite := decode.io.regWrite
    id_ex.memWrite := decode.io.memWrite
    id_ex.branch   := decode.io.branch
    id_ex.aluSrc   := decode.io.aluSrc
    //id_ex.aluOp    := decode.io.aluOp
    id_ex.funct3   := if_id_instr(14, 12)
    id_ex.funct7   := if_id_instr(31, 25)
    id_ex.memToReg := decode.io.memToReg
    id_ex.jump     := decode.io.jump
    id_ex.jumpReg := decode.io.jumpReg
    id_ex.auipc    := decode.io.auipc
    id_ex.halt     := decode.io.halt
    // CSR Signals
    id_ex.csr_op      := decode.io.csr_op
    id_ex.csr_src_imm := decode.io.csr_src_imm
    id_ex.csr_addr    := if_id_instr(31, 20)    // (CSR address from instruction)
    // Load/Store Signals
    id_ex.loadType     := decode.io.loadType
    id_ex.loadUnsigned := decode.io.loadUnsigned
    id_ex.storeType    := decode.io.storeType
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
    //val tx         = Bool()
    val memToReg   = Bool()
    val pc_plus_4  = UInt(32.W)
    val jump       = Bool()
    val jumpReg = Bool()
    val pc         = UInt(32.W)
    val imm        = UInt(32.W)
    val auipc      = Bool()
    val csr_data   = UInt(32.W)
    val is_csr     = Bool()
    // Load/Store Signals
    val loadType      = UInt(3.W)
    val loadUnsigned  = Bool()
    val storeType     = UInt(3.W)
  }
  val ex_mem = RegInit(0.U.asTypeOf(new EX_MEM_Bundle))

  // MEM/WB Pipeline Register Bundle (Defined here for Forwarding reference)
  class MEM_WB_Bundle extends Bundle {
    val result   = UInt(32.W)
    val rd_addr  = UInt(5.W)
    val regWrite = Bool()
  }
  val mem_wb = RegInit(0.U.asTypeOf(new MEM_WB_Bundle))

  val wb_data = Wire(UInt(32.W))

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
    2.U -> wb_data   // Forward from MEM
  ))

  val forwardB_data = MuxLookup(forwarding.io.forwardB, id_ex.rs2_data)(Seq(
    0.U -> id_ex.rs2_data,
    1.U -> mem_wb.result,
    2.U -> wb_data
  ))

  // ALU Connections
  alu.io.alu_op := id_ex.alu_op
  alu.io.alu_a  := Mux(id_ex.auipc, id_ex.pc, forwardA_data)
  alu.io.alu_b  := Mux(id_ex.aluSrc, id_ex.imm, forwardB_data)

  // === CSR Module Connections ===
  csrModule.io.csr_addr    := id_ex.csr_addr
  csrModule.io.csr_op      := id_ex.csr_op

  // CSR data input: Vælg mellem register (rs1) eller immediate (zimm)
  csrModule.io.csr_data_in := Mux(id_ex.csr_src_imm,
    id_ex.imm,       // Immediate variant (CSRRWI, etc.)
    forwardA_data    // Register variant (CSRRW, etc.) - use forwarded rs1
  )

  // Core events for CSR updates
  csrModule.io.inst_retire  := mem_wb.regWrite  // Count retired instructions
  csrModule.io.pc_in        := id_ex.pc
  csrModule.io.cause_in     := 0.U  // No exceptions yet
  csrModule.io.trap_trigger := false.B  // No traps yet

  // Branch Logic (Resolved in EX stage)
  // Vi bruger funct3 til at vælge præcis hvilket flag (Zero, Less, etc.) vi skal reagere på
  val branchConditionMet = MuxLookup(id_ex.funct3, false.B)(Seq(
    "b000".U -> alu.io.zero,         // BEQ
    "b001".U -> !alu.io.zero,        // BNE
    "b100".U -> alu.io.less_signed,  // BLT
    "b101".U -> !alu.io.less_signed, // BGE
    "b110".U -> alu.io.less_unsigned,// BLTU
    "b111".U -> !alu.io.less_unsigned// BGEU
  ))

  val branch_taken = id_ex.branch && branchConditionMet
  val jump_taken = id_ex.jump

  // Combined: branch OR jump causes PC to change
  val pc_change = branch_taken || jump_taken

  // Jump target (same calculation for both branch and JAL)
  val jump_target = Mux(id_ex.jumpReg,
    (forwardA_data + id_ex.imm) & ~1.U,  // JALR: (rs1 + imm) & ~1
    id_ex.pc + id_ex.imm                  // JAL:  PC + imm
  )

  // Update Branch Predictor
  branchPredictor.io.update_valid  := true.B
  branchPredictor.io.update_pc     := id_ex.pc
  branchPredictor.io.update_taken  := pc_change
  branchPredictor.io.update_target := jump_target
  branchPredictor.io.is_branch     := id_ex.branch  // Only update for branches, not jumps


  // Update Fetch Unit
  fetch.io.branch_taken   := pc_change
  fetch.io.jump_target_pc := jump_target
  fetch.io.stall          := hazard.io.stall
  fetch.io.halt           := id_ex.halt
  fetch.io.write_en       := memIO.io.imemWriteEn // Signal from MemoryMapping that tells if we are writing to 0x8000+
  fetch.io.write_addr     := memIO.io.imemWriteAddr // The calibrated adress (without 0x8000 offset)
  fetch.io.write_data     := ex_mem.rs2_data // Data that needs to be stored (from sw instr)
  fetch.io.predict_taken     := branchPredictor.io.predict_taken
  fetch.io.predicted_target  := branchPredictor.io.predicted_target

  //BRanch precdiction update
  branchPredictor.io.fetch_pc := fetch.io.pc
  // Update Hazard Unit
  hazard.io.branch_taken := pc_change
  hazard.io.predicted_taken := branchPredictor.io.predict_taken

  // Load-Use Hazard Detection signals:
  hazard.io.id_ex_memToReg   := id_ex.memToReg
  hazard.io.id_ex_rd         := id_ex.rd_addr
  hazard.io.if_id_rs1        := if_id_instr(19, 15)
  hazard.io.if_id_rs2        := if_id_instr(24, 20)

  // Update EX/MEM Register
  ex_mem.alu_result := alu.io.result
  ex_mem.rs2_data   := forwardB_data // Store data (must be forwarded version)
  ex_mem.rd_addr    := id_ex.rd_addr
  ex_mem.regWrite   := id_ex.regWrite
  ex_mem.memWrite   := id_ex.memWrite
  //ex_mem.tx         := id_ex.tx
  ex_mem.memToReg   := id_ex.memToReg
  ex_mem.pc_plus_4  := id_ex.pc + 4.U
  ex_mem.jump       := id_ex.jump
  ex_mem.jumpReg := id_ex.jumpReg
  ex_mem.pc         := id_ex.pc
  ex_mem.imm        := id_ex.imm
  ex_mem.auipc      := id_ex.auipc
  ex_mem.csr_data   := csrModule.io.csr_data_out
  ex_mem.is_csr     := (id_ex.csr_op =/= 0.U)

  // Load/Store Signals
  ex_mem.loadType     := id_ex.loadType
  ex_mem.loadUnsigned := id_ex.loadUnsigned
  ex_mem.storeType    := id_ex.storeType

  // ==============================================================================
  // MEM STAGE (Memory Access)
  // ==============================================================================

  // ======== UART START ========
  uart.io.channel.valid := memIO.io.uartValid
  uart.io.channel.bits  := memIO.io.uartData(7, 0) // Send lowest byte

  io.tx := uart.io.txd
  // ======== UART END ========


  memIO.io.address   := ex_mem.alu_result
  memIO.io.writeData := ex_mem.rs2_data
  memIO.io.memWrite  := ex_mem.memWrite

  memIO.io.loadType     := ex_mem.loadType
  memIO.io.loadUnsigned := ex_mem.loadUnsigned
  memIO.io.storeType    := ex_mem.storeType

  // Read data from memory
  val memReadData = memIO.io.readData

  // --- HANDLE UART STATUS READ ---
  // If reading from 0x1004, return the UART Ready status (Bit 0)
  val is_uart_status = (ex_mem.alu_result === 0x1004.U)
  // Construct status word: Bit 0 is Ready, others 0
  //val uart_status_word = 1.U(32.W) //For testing, always ready
  val uart_status_word = Cat(0.U(31.W), uart.io.channel.ready)

  // Select writeback data:
  // Priority: Jump (PC+4) > AUIPC > CSR > Memory > ALU result
  wb_data := MuxCase(ex_mem.alu_result, Seq(
    ex_mem.jump      -> ex_mem.pc_plus_4,       // JAL/JALR: write PC+4
    ex_mem.auipc     -> (ex_mem.pc + ex_mem.imm), // AUIPC: write PC+imm
    ex_mem.is_csr    -> ex_mem.csr_data,        // ← ADD: CSR: write CSR value
    ex_mem.memToReg  -> Mux(is_uart_status, uart_status_word, memReadData)              // Load: write memory data
  ))


  // Update MEM/WB Register
  mem_wb.result   := wb_data
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

  // ==================================================================
  // Connections for Debug Ports
  // ==================================================================

  // 1. Connect Registers (FIXED: Use IO ports, not internal memory)
  io.debug_x1  := regFile.io.debug_x1
  io.debug_x2  := regFile.io.debug_x2
  io.debug_x3  := regFile.io.debug_x3
  io.debug_x4  := regFile.io.debug_x4
  io.debug_x10 := regFile.io.debug_x10
  io.debug_x11 := regFile.io.debug_x11
  io.debug_x12 := regFile.io.debug_x12
  io.debug_x13 := regFile.io.debug_x13
  io.debug_x14 := regFile.io.debug_x14

  // 2. Connect Pipeline Info
  io.debug_stall        := hazard.io.stall
  io.debug_flush        := hazard.io.flush
  io.debug_forwardA     := forwarding.io.forwardA
  io.debug_forwardB     := forwarding.io.forwardB
  io.debug_branch_taken := pc_change

  // 3. Connect CSR Info
  io.debug_mcycle       := csrModule.io.csr_data_out
}*/

package core

import chisel3._
import chisel3.util._

class Core(program: Seq[UInt] = Seq(), programFile: String = "") extends Module {
  val io = IO(new Bundle {
    val pc_out      = Output(UInt(32.W))
    val instruction = Output(UInt(32.W))
    val alu_res     = Output(UInt(32.W))
    val led         = Output(UInt(1.W))
    val uartData    = Output(UInt(8.W))
    val uartAddr    = Output(UInt(32.W))
    val uartValid   = Output(Bool())
    val tx          = Output(Bool())

    // Debug Ports
    val debug_x1    = Output(UInt(32.W))
    val debug_x2    = Output(UInt(32.W))
    val debug_x3    = Output(UInt(32.W))
    val debug_x4    = Output(UInt(32.W))
    val debug_x10   = Output(UInt(32.W))
    val debug_x11   = Output(UInt(32.W))
    val debug_x12   = Output(UInt(32.W))
    val debug_x13   = Output(UInt(32.W))
    val debug_x14   = Output(UInt(32.W))
    val debug_stall         = Output(Bool())
    val debug_flush         = Output(Bool())
    val debug_forwardA      = Output(UInt(2.W))
    val debug_forwardB      = Output(UInt(2.W))
    val debug_branch_taken  = Output(Bool())
    val debug_mcycle        = Output(UInt(32.W))
  })

  // Instantiate Modules
  val fetch       = Module(new InstructionFetch(program, programFile))
  val decode      = Module(new ControlUnit())
  val regFile     = Module(new RegisterFile())
  val alu         = Module(new ALU())
  val memIO       = Module(new MemoryMapping(programFile = programFile))
  val forwarding  = Module(new ForwardingUnit())
  val hazard      = Module(new HazardUnit())
  val uart        = Module(new BufferedTx(25000000, 115200))
  val csrModule   = Module(new CSRModule())
  val branchPredictor = Module(new BranchPredictor())

  // ==============================================================================
  // IF STAGE
  // ==============================================================================
  val if_id_pc    = RegInit(0.U(32.W))
  val if_id_instr = RegInit(0x00000013.U(32.W)) // Init with NOP

  when(hazard.io.flush) {
    if_id_instr := 0x00000013.U 
  }.elsewhen(!hazard.io.stall) {
    if_id_pc    := fetch.io.pc
    if_id_instr := fetch.io.instruction
  }

  // ==============================================================================
  // ID STAGE
  // ==============================================================================
  decode.io.instruction := if_id_instr

  regFile.io.rs1_addr := if_id_instr(19, 15)
  regFile.io.rs2_addr := if_id_instr(24, 20)

  // --- BRANCH RESOLUTION IN ID ---
  val rs1_val = regFile.io.rs1_data
  val rs2_val = regFile.io.rs2_data

  val is_equal = (rs1_val === rs2_val)
  val is_less_signed = (rs1_val.asSInt < rs2_val.asSInt)
  val is_less_unsigned = (rs1_val < rs2_val)

  val branchConditionMet = MuxLookup(if_id_instr(14, 12), false.B)(Seq(
    "b000".U -> is_equal,              // BEQ
    "b001".U -> !is_equal,             // BNE
    "b100".U -> is_less_signed,        // BLT
    "b101".U -> !is_less_signed,       // BGE
    "b110".U -> is_less_unsigned,      // BLTU
    "b111".U -> !is_less_unsigned      // BGEU
  ))

  // Branch depends on !stall to ensure we have valid register data
  val branch_taken = decode.io.branch && branchConditionMet && !hazard.io.stall
  val jump_taken   = decode.io.jump && !hazard.io.stall
  val pc_change    = branch_taken || jump_taken

  val jump_target = if_id_pc + decode.io.imm 

  // --- CONNECT FETCH UNIT (Partial) ---
  fetch.io.branch_taken   := pc_change
  fetch.io.jump_target_pc := jump_target
  fetch.io.stall          := hazard.io.stall
  fetch.io.write_en       := memIO.io.imemWriteEn
  fetch.io.write_addr     := memIO.io.imemWriteAddr
  fetch.io.predict_taken     := branchPredictor.io.predict_taken
  fetch.io.predicted_target  := branchPredictor.io.predicted_target
  
  branchPredictor.io.fetch_pc      := fetch.io.pc
  branchPredictor.io.update_valid  := true.B
  branchPredictor.io.update_pc     := if_id_pc
  branchPredictor.io.update_taken  := pc_change
  branchPredictor.io.update_target := jump_target
  branchPredictor.io.is_branch     := decode.io.branch

  // ==============================================================================
  // ID/EX PIPELINE REGISTER
  // ==============================================================================
  class ID_EX_Bundle extends Bundle {
    val pc       = UInt(32.W)
    val rs1_data = UInt(32.W)
    val rs2_data = UInt(32.W)
    val imm      = UInt(32.W)
    val rs1_addr = UInt(5.W)
    val rs2_addr = UInt(5.W)
    val rd_addr  = UInt(5.W)
    val alu_op   = UInt(4.W)
    val regWrite = Bool()
    val memWrite = Bool()
    val branch   = Bool()
    val aluSrc   = Bool()
    val funct3   = UInt(3.W)
    val funct7   = UInt(7.W)
    val memToReg = Bool()
    val jump     = Bool()
    val jumpReg  = Bool()
    val auipc    = Bool()
    val halt     = Bool()
    val csr_op      = UInt(3.W)
    val csr_src_imm = Bool()
    val csr_addr    = UInt(12.W)
    val loadType      = UInt(3.W)
    val loadUnsigned  = Bool()
    val storeType     = UInt(3.W)
  }
  val id_ex = RegInit(0.U.asTypeOf(new ID_EX_Bundle))

  when(hazard.io.flush || hazard.io.stall) {
    val bubble = Wire(new ID_EX_Bundle)
    bubble := 0.U.asTypeOf(new ID_EX_Bundle)
    bubble.pc := if_id_pc 
    id_ex := bubble
  } .otherwise {
    id_ex.pc        := if_id_pc
    id_ex.rs1_data  := regFile.io.rs1_data
    id_ex.rs2_data  := regFile.io.rs2_data
    id_ex.imm       := decode.io.imm
    id_ex.rs1_addr  := if_id_instr(19, 15)
    id_ex.rs2_addr  := if_id_instr(24, 20)
    id_ex.rd_addr   := if_id_instr(11, 7)
    id_ex.alu_op    := decode.io.aluOp
    id_ex.regWrite  := decode.io.regWrite
    id_ex.memWrite  := decode.io.memWrite
    id_ex.branch    := decode.io.branch
    id_ex.aluSrc    := decode.io.aluSrc
    id_ex.funct3    := if_id_instr(14, 12)
    id_ex.funct7    := if_id_instr(31, 25)
    id_ex.memToReg  := decode.io.memToReg
    id_ex.jump      := decode.io.jump
    id_ex.jumpReg   := decode.io.jumpReg
    id_ex.auipc     := decode.io.auipc
    id_ex.halt      := decode.io.halt
    id_ex.csr_op      := decode.io.csr_op
    id_ex.csr_src_imm := decode.io.csr_src_imm
    id_ex.csr_addr    := if_id_instr(31, 20)
    id_ex.loadType      := decode.io.loadType
    id_ex.loadUnsigned  := decode.io.loadUnsigned
    id_ex.storeType     := decode.io.storeType
  }

  // ==============================================================================
  // EX STAGE
  // ==============================================================================
  class EX_MEM_Bundle extends Bundle {
    val alu_result = UInt(32.W)
    val rs2_data   = UInt(32.W)
    val rd_addr    = UInt(5.W)
    val regWrite   = Bool()
    val memWrite   = Bool()
    val memToReg   = Bool()
    val pc         = UInt(32.W)
    val loadType      = UInt(3.W)
    val loadUnsigned  = Bool()
    val storeType     = UInt(3.W)
    // TIMING OPTIMIZATION: Store pre-calculated result here
    val res_val    = UInt(32.W)
  }
  val ex_mem = RegInit(0.U.asTypeOf(new EX_MEM_Bundle))
  
  class MEM_WB_Bundle extends Bundle {
    val result   = UInt(32.W)
    val rd_addr  = UInt(5.W)
    val regWrite = Bool()
  }
  val mem_wb = RegInit(0.U.asTypeOf(new MEM_WB_Bundle))
  val wb_data = Wire(UInt(32.W))

  // Forwarding
  forwarding.io.id_ex_rs1       := id_ex.rs1_addr
  forwarding.io.id_ex_rs2       := id_ex.rs2_addr
  forwarding.io.ex_mem_rd       := ex_mem.rd_addr
  forwarding.io.ex_mem_regWrite := ex_mem.regWrite
  forwarding.io.mem_wb_rd       := mem_wb.rd_addr
  forwarding.io.mem_wb_regWrite := mem_wb.regWrite

  val forwardA_data = MuxLookup(forwarding.io.forwardA, id_ex.rs1_data)(Seq(
    0.U -> id_ex.rs1_data,
    1.U -> mem_wb.result,
    2.U -> ex_mem.res_val // Forward pre-calc result
  ))
  val forwardB_data = MuxLookup(forwarding.io.forwardB, id_ex.rs2_data)(Seq(
    0.U -> id_ex.rs2_data,
    1.U -> mem_wb.result,
    2.U -> ex_mem.res_val
  ))

  alu.io.alu_op := id_ex.alu_op
  alu.io.alu_a  := Mux(id_ex.auipc, id_ex.pc, forwardA_data)
  alu.io.alu_b  := Mux(id_ex.aluSrc, id_ex.imm, forwardB_data)

  csrModule.io.csr_addr    := id_ex.csr_addr
  csrModule.io.csr_op      := id_ex.csr_op
  csrModule.io.csr_data_in := Mux(id_ex.csr_src_imm, id_ex.imm, forwardA_data)
  csrModule.io.inst_retire  := mem_wb.regWrite
  csrModule.io.pc_in        := id_ex.pc
  csrModule.io.cause_in     := 0.U
  csrModule.io.trap_trigger := false.B

  // --- RESULT SELECTION (Pipelined) ---
  val pc_plus_4 = id_ex.pc + 4.U
  val auipc_res = id_ex.pc + id_ex.imm
  val is_csr    = (id_ex.csr_op =/= 0.U)
  
  val ex_result = MuxCase(alu.io.result, Seq(
    id_ex.jump  -> pc_plus_4,
    id_ex.auipc -> auipc_res,
    is_csr      -> csrModule.io.csr_data_out
  ))

  // EX/MEM Update
  ex_mem.alu_result := alu.io.result
  ex_mem.rs2_data   := forwardB_data
  ex_mem.rd_addr    := id_ex.rd_addr
  ex_mem.regWrite   := id_ex.regWrite
  ex_mem.memWrite   := id_ex.memWrite
  ex_mem.memToReg   := id_ex.memToReg
  ex_mem.pc         := id_ex.pc
  ex_mem.loadType     := id_ex.loadType
  ex_mem.loadUnsigned := id_ex.loadUnsigned
  ex_mem.storeType    := id_ex.storeType
  ex_mem.res_val      := ex_result

  // ==============================================================================
  // MEM STAGE
  // ==============================================================================
  uart.io.channel.valid := memIO.io.uartValid
  uart.io.channel.bits  := memIO.io.uartData(7, 0)
  io.tx := uart.io.txd

  memIO.io.address   := ex_mem.alu_result
  memIO.io.writeData := ex_mem.rs2_data
  memIO.io.memWrite  := ex_mem.memWrite
  memIO.io.loadType     := ex_mem.loadType
  memIO.io.loadUnsigned := ex_mem.loadUnsigned
  memIO.io.storeType    := ex_mem.storeType

  val memReadData = memIO.io.readData
  val is_uart_status = (ex_mem.alu_result === 0x1004.U)
  val uart_status_word = Cat(0.U(31.W), uart.io.channel.ready)

  // Select between Memory Data (Loads) and Pre-calculated Result
  wb_data := Mux(ex_mem.memToReg, 
                Mux(is_uart_status, uart_status_word, memReadData), 
                ex_mem.res_val)

  mem_wb.result   := wb_data
  mem_wb.rd_addr  := ex_mem.rd_addr
  mem_wb.regWrite := ex_mem.regWrite

  // ==============================================================================
  // WB STAGE
  // ==============================================================================
  regFile.io.rd_addr   := mem_wb.rd_addr
  regFile.io.rd_data   := mem_wb.result
  regFile.io.reg_write := mem_wb.regWrite

  // ==============================================================================
  // WIRING (Consolidated)
  // ==============================================================================
  hazard.io.rs1            := if_id_instr(19, 15)
  hazard.io.rs2            := if_id_instr(24, 20)
  hazard.io.is_branch      := decode.io.branch
  hazard.io.is_jalr        := decode.io.jumpReg
  
  hazard.io.id_ex_rd       := id_ex.rd_addr
  hazard.io.id_ex_regWrite := id_ex.regWrite
  hazard.io.id_ex_memToReg := id_ex.memToReg
  
  hazard.io.ex_mem_rd      := ex_mem.rd_addr
  hazard.io.ex_mem_regWrite:= ex_mem.regWrite
  
  hazard.io.branch_taken   := branch_taken
  hazard.io.jump_taken     := jump_taken
  hazard.io.predicted_taken := branchPredictor.io.predict_taken 

  fetch.io.halt       := id_ex.halt
  fetch.io.write_data := ex_mem.rs2_data

  // Debug
  io.pc_out      := if_id_pc
  io.instruction := if_id_instr
  io.alu_res     := ex_mem.alu_result
  io.led         := memIO.io.led
  io.uartData    := memIO.io.uartData
  io.uartAddr    := memIO.io.uartAddr
  io.uartValid   := memIO.io.uartValid
  io.debug_x1  := regFile.io.debug_x1
  io.debug_x2  := regFile.io.debug_x2
  io.debug_x3  := regFile.io.debug_x3
  io.debug_x4  := regFile.io.debug_x4
  io.debug_x10 := regFile.io.debug_x10
  io.debug_x11 := regFile.io.debug_x11
  io.debug_x12 := regFile.io.debug_x12
  io.debug_x13 := regFile.io.debug_x13
  io.debug_x14 := regFile.io.debug_x14
  io.debug_stall         := hazard.io.stall
  io.debug_flush         := hazard.io.flush
  io.debug_forwardA      := forwarding.io.forwardA
  io.debug_forwardB      := forwarding.io.forwardB
  io.debug_branch_taken  := pc_change
  io.debug_mcycle        := csrModule.io.csr_data_out
}