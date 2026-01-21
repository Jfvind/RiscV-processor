package core

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
}