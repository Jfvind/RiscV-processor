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
    // Signals from ID Stage (Current Instruction)
    val rs1      = Input(UInt(5.W))
    val rs2      = Input(UInt(5.W))
    val is_branch = Input(Bool()) // Is it a B-type instruction?
    val is_jalr   = Input(Bool()) // Is it JALR?

    // Signals from EX Stage (Previous Instruction)
    val id_ex_rd       = Input(UInt(5.W))
    val id_ex_regWrite = Input(Bool())
    val id_ex_memToReg = Input(Bool()) // For Load-Use detection

    // Signals from MEM Stage (2nd Previous Instruction)
    val ex_mem_rd       = Input(UInt(5.W))
    val ex_mem_regWrite = Input(Bool())

    // Branch Control (Output from ID)
    val branch_taken = Input(Bool())
    val jump_taken   = Input(Bool())

    // Branch Prediction (Keep inputs to avoid breaking connections, but we won't use them for flushing now)
    val predicted_taken = Input(Bool())
    val mispredicted    = Output(Bool())

    // Outputs
    val flush = Output(Bool()) // Flush IF/ID
    val stall = Output(Bool()) // Stall PC and IF/ID
  })

  // Default
  io.flush := false.B
  io.stall := false.B
  io.mispredicted := false.B

  // --- 1. DATA HAZARD STALL (For Branch/Jump in ID) ---
  // If we branch in ID, we need operands ready. If they are in EX or MEM pipeline, we MUST stall.
  // (Unless we add forwarding to ID, but Stalling is simpler and fixes timing).
  
  // Check RS1 Dependency
  val rs1_conflict_ex  = (io.rs1 === io.id_ex_rd)  && (io.id_ex_rd =/= 0.U) && io.id_ex_regWrite
  val rs1_conflict_mem = (io.rs1 === io.ex_mem_rd) && (io.ex_mem_rd =/= 0.U) && io.ex_mem_regWrite
  val rs1_hazard = (io.is_branch || io.is_jalr) && (rs1_conflict_ex || rs1_conflict_mem)

  // Check RS2 Dependency (Only for Branch)
  val rs2_conflict_ex  = (io.rs2 === io.id_ex_rd)  && (io.id_ex_rd =/= 0.U) && io.id_ex_regWrite
  val rs2_conflict_mem = (io.rs2 === io.ex_mem_rd) && (io.ex_mem_rd =/= 0.U) && io.ex_mem_regWrite
  val rs2_hazard = io.is_branch && (rs2_conflict_ex || rs2_conflict_mem)

  // --- 2. LOAD-USE HAZARD (Standard) ---
  // Standard load-use for non-branch instructions
  val loadUseHazard = io.id_ex_memToReg && (io.id_ex_rd =/= 0.U) &&
    ((io.id_ex_rd === io.rs1) || (io.id_ex_rd === io.rs2)) // Note: Using io.rs1/rs2 from ID directly

  // Combined Stall
  io.stall := rs1_hazard || rs2_hazard || loadUseHazard

  // --- 3. CONTROL HAZARD (Flush) ---
  // If we take a branch or jump in ID, we must flush the instruction currently being fetched (IF).
  // We do NOT need to flush ID/EX because the branch itself is in ID and will "retire" (become a bubble/NOP) into EX.
  when(!io.stall && (io.branch_taken || io.jump_taken)) {
    io.flush := true.B
  }
}