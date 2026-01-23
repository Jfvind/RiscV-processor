/*package core

import chisel3._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    // Branch Control
    val branch_taken = Input(Bool())

    // Branch Prediction
    val predicted_taken = Input(Bool())
    val mispredicted    = Output(Bool())

    // Load-Use Hazard Detection (NEW)
    val id_ex_memToReg = Input(Bool())  // Indicates if the instruction in EX stage is a load
    val id_ex_rd       = Input(UInt(5.W))
    val if_id_rs1      = Input(UInt(5.W))
    val if_id_rs2      = Input(UInt(5.W))

    // Stall / Flush Signals
    val flush = Output(Bool()) // Flush IF/ID and ID/EX
    val stall = Output(Bool()) // Stall PC and IF/ID (unused for now)
  })

  // Default
  io.flush := false.B
  io.stall := false.B
  io.mispredicted := false.B

  // Load-Use Hazard Detection
  // If the instruction in EX is a load (memToReg = true)
  // AND the instruction in ID uses the load's destination register
  // THEN we must stall
  val loadUseHazard = io.id_ex_memToReg &&
    (io.id_ex_rd =/= 0.U) &&
    ((io.id_ex_rd === io.if_id_rs1 && io.if_id_rs1 =/= 0.U) ||
      (io.id_ex_rd === io.if_id_rs2 && io.if_id_rs2 =/= 0.U))

  io.stall := loadUseHazard

  // Misprediction Detection
  val misprediction = (io.predicted_taken =/= io.branch_taken)
  io.mispredicted := misprediction

  // Control Hazard: Branch Taken
  when(io.branch_taken || misprediction) {
    io.flush := true.B
  }
}*/

package core

import chisel3._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    // ID Stage Signals (Current Instruction)
    val rs1       = Input(UInt(5.W))
    val rs2       = Input(UInt(5.W))
    val is_branch = Input(Bool())
    val is_jalr   = Input(Bool())

    // Pipeline Hazards (Previous Instructions)
    val id_ex_rd       = Input(UInt(5.W))
    val id_ex_regWrite = Input(Bool())
    val id_ex_memToReg = Input(Bool())

    val ex_mem_rd       = Input(UInt(5.W))
    val ex_mem_regWrite = Input(Bool())

    // Control Signals
    val branch_taken = Input(Bool())
    val jump_taken   = Input(Bool())
    val predicted_taken = Input(Bool()) // Keep for compatibility

    val flush = Output(Bool())
    val stall = Output(Bool())
    val mispredicted = Output(Bool())
  })

  // Default
  io.flush := false.B
  io.stall := false.B
  io.mispredicted := false.B

  // --- 1. BRANCH DATA HAZARD (Stall) ---
  // If Branch in ID needs a register currently in EX or MEM, we MUST stall.
  val rs1_hazard_ex  = (io.rs1 === io.id_ex_rd)  && (io.id_ex_rd =/= 0.U) && io.id_ex_regWrite
  val rs1_hazard_mem = (io.rs1 === io.ex_mem_rd) && (io.ex_mem_rd =/= 0.U) && io.ex_mem_regWrite
  val rs1_hazard     = (io.is_branch || io.is_jalr) && (rs1_hazard_ex || rs1_hazard_mem)

  val rs2_hazard_ex  = (io.rs2 === io.id_ex_rd)  && (io.id_ex_rd =/= 0.U) && io.id_ex_regWrite
  val rs2_hazard_mem = (io.rs2 === io.ex_mem_rd) && (io.ex_mem_rd =/= 0.U) && io.ex_mem_regWrite
  val rs2_hazard     = io.is_branch && (rs2_hazard_ex || rs2_hazard_mem)

  // --- 2. LOAD-USE HAZARD (Stall) ---
  val loadUseHazard = io.id_ex_memToReg && (io.id_ex_rd =/= 0.U) &&
    ((io.id_ex_rd === io.rs1) || (io.id_ex_rd === io.rs2))

  // Combine Stalls
  io.stall := rs1_hazard || rs2_hazard || loadUseHazard

  // --- 3. CONTROL HAZARD (Flush) ---
  // If we take a branch, we flush the Instruction currently being fetched
  when(!io.stall && (io.branch_taken || io.jump_taken)) {
    io.flush := true.B
  }
}