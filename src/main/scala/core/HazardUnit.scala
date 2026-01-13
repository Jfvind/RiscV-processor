package core

import chisel3._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    // Branch Control
    val branch_taken = Input(Bool())

    // Stall / Flush Signals
    val flush = Output(Bool()) // Flush IF/ID and ID/EX
    val stall = Output(Bool()) // Stall PC and IF/ID (unused for now)
  })

  // Default
  io.flush := false.B
  io.stall := false.B

  // Control Hazard: Branch Taken
  // If branch is taken, we must flush the instructions in IF and ID stages
  when(io.branch_taken) {
    io.flush := true.B
  }
}