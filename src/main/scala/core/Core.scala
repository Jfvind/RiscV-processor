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
    // Uart debug
    val uartData    = Output(UInt(8.W))
    val uartAddr    = Output(UInt(32.W))
    val uartValid   = Output(Bool())

    // Uart prod
    val tx          = Output(Bool())
  })

  // Instantiate Modules
  val fetch      = Module(new InstructionFetch(program))
  val decode     = Module(new ControlUnit())   // Contains ImmGen
  val regFile    = Module(new RegisterFile())
  val alu        = Module(new ALU())           // Contains ALUConstants
  val memIO      = Module(new MemoryMapping()) // Decides RAM or LED (Contains DataMemory)
  val forwarding = Module(new ForwardingUnit())
  val hazard     = Module(new HazardUnit())
  val serialPort = Module(new Serialport())

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
    val tx       = Bool()
    // Control Signals
    val regWrite = Bool()
    val memWrite = Bool()
    val branch   = Bool()
    val aluSrc   = Bool()
    val aluOp    = UInt(4.W)
    val funct3   = UInt(3.W)
    val funct7   = UInt(7.W)
    val memToReg = Bool()
    val jump     = Bool()
    val jumpReg  = Bool()
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
    id_ex.tx       := serialPort.io.tx
    // Control Signals
    id_ex.regWrite := decode.io.regWrite
    id_ex.memWrite := decode.io.memWrite
    id_ex.branch   := decode.io.branch
    id_ex.aluSrc   := decode.io.aluSrc
    id_ex.aluOp    := decode.io.aluOp
    id_ex.funct3   := if_id_instr(14, 12)
    id_ex.funct7   := if_id_instr(31, 25)
    id_ex.memToReg := decode.io.memToReg
    id_ex.jump     := decode.io.jump
    id_ex.jumpReg := decode.io.jumpReg
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
    val tx         = Bool()
    val memToReg   = Bool()
    val pc_plus_4  = UInt(32.W)
    val jump       = Bool()
    val jumpReg = Bool()
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
//instantiate ALUDecode
  val aluDecoder = Module(new ALUDecoder())
  // wire in
  aluDecoder.io.aluOp  := id_ex.aluOp   // 2-bit fra Control Unit
  aluDecoder.io.funct3 := id_ex.funct3  // 3-bit fra instruktion
  aluDecoder.io.funct7 := id_ex.funct7  // 7-bit fra instruktion

  // ALU Connections
  alu.io.alu_op := aluDecoder.io.op
  alu.io.alu_a  := forwardA_data
  alu.io.alu_b  := Mux(id_ex.aluSrc, id_ex.imm, forwardB_data)

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


  // Update Fetch Unit
  fetch.io.branch_taken   := pc_change
  fetch.io.jump_target_pc := jump_target
  fetch.io.stall          := hazard.io.stall

  // Update Hazard Unit
  hazard.io.branch_taken := pc_change

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
  ex_mem.tx         := id_ex.tx
  ex_mem.memToReg   := id_ex.memToReg
  ex_mem.pc_plus_4  := id_ex.pc + 4.U
  ex_mem.jump       := id_ex.jump
  ex_mem.jumpReg := id_ex.jumpReg

  // ==============================================================================
  // MEM STAGE (Memory Access)
  // ==============================================================================

  // ======== UART START ========
  // --- HELPER: 4-bit Hex to ASCII ---
  def nibbleToChar(nibble: UInt): UInt = {
    val n = (nibble & 0xf.U)(3, 0)
    val tmp = Mux(n < 10.U(4.W), n + 48.U(8.W), n + 55.U(8.W))
    val char = WireDefault(0.U(8.W)) // cast to 8 bit wide for uart
    char := tmp
    char
  }
  // --- HELPER: division by 10 and modulo 10 ---
  def div10(n: UInt): (UInt, UInt) = {
    val w = n.getWidth
    require(w > 0)
    val k = w + 3
    val M = (((1 << k) + 9) / 10) // computed in Scala at elaboration time
    val q  = (n * M.U) >> k       // quotient = n / 10
    val r  = n - q * 10.U         // remainder = n % 10
    (q, r)
  }

  // --- INPUTS FROM CORE ---
  val dataReg = RegInit(0.U(32.W))
  val addrReg = RegInit(0.U(32.W))
  val triggerReg = RegInit(false.B)

  when (memIO.io.uartValid) {
    dataReg := ex_mem.rs2_data
    addrReg := ex_mem.alu_result
    triggerReg := true.B // Set trigger for next cycle (Delays data by one cycle so dataReg is updated)
  } .otherwise {
    triggerReg := false.B //Clear after one cycle
  }

  // --- CALCULATE REGISTER INDEX ---
  // Address 200 -> Index 0. Address 204 -> Index 1.
  // Formula: (Address - 200) / 4
  val regIndex = (addrReg - 200.U) >> 2

  // Split Index into Two Decimal Digits (e.g., 31 -> '3', '1')
  val (tens, ones) = div10(regIndex)

  // --- CONSTRUCT STRING ---
  // Format: "x00: 00000000\r\n"
  val asciiVec = Reg(Vec(32, UInt(8.W)))

  asciiVec(0) := 'x'.U
  asciiVec(1) := nibbleToChar(tens) // Reusing nibbleToChar works for 0-3
  asciiVec(2) := nibbleToChar(ones) // Reusing nibbleToChar works for 0-9
  asciiVec(3) := ':'.U
  asciiVec(4) := ' '.U

  // Standard Hex Data (8 chars)
  asciiVec(5)  := nibbleToChar(dataReg(31, 28))
  asciiVec(6)  := nibbleToChar(dataReg(27, 24))
  asciiVec(7)  := nibbleToChar(dataReg(23, 20))
  asciiVec(8)  := nibbleToChar(dataReg(19, 16))
  asciiVec(9)  := nibbleToChar(dataReg(15, 12))
  asciiVec(10) := nibbleToChar(dataReg(11, 8))
  asciiVec(11) := nibbleToChar(dataReg(7, 4))
  asciiVec(12) := nibbleToChar(dataReg(3, 0))

  asciiVec(13) := '\r'.U
  asciiVec(14) := '\n'.U
  
  // Padding
  for (i <- 15 until 32) { asciiVec(i) := 0.U }

  serialPort.io.inputString := asciiVec
  serialPort.io.sendTrigger := triggerReg
  io.tx                     := ex_mem.tx
  // ======== UART END ========


  memIO.io.address   := ex_mem.alu_result
  memIO.io.writeData := ex_mem.rs2_data
  memIO.io.memWrite  := ex_mem.memWrite

  // Read data from memory
  val memReadData = memIO.io.readData

  // Select writeback data:
  // Priority: Jump (PC+4) > Memory > ALU result
  val wb_data = MuxCase(ex_mem.alu_result, Seq(
    ex_mem.jump      -> ex_mem.pc_plus_4,  // JAL/JALR: write PC+4
    ex_mem.memToReg  -> memReadData         // Load: write memory data
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
}