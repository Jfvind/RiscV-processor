package core

import chisel3._

class ForwardingUnit extends Module {
  val io = IO(new Bundle {
    // Inputs from ID/EX stage (Source Registers)
    val id_ex_rs1 = Input(UInt(5.W))
    val id_ex_rs2 = Input(UInt(5.W))

    // Inputs from EX/MEM stage (Destination Register)
    val ex_mem_rd = Input(UInt(5.W))
    val ex_mem_regWrite = Input(Bool())

    // Inputs from MEM/WB stage (Destination Register)
    val mem_wb_rd = Input(UInt(5.W))
    val mem_wb_regWrite = Input(Bool())

    // Forwarding Control Signals
    // 00: No forwarding (use ID/EX data)
    // 01: Forward from WB
    // 10: Forward from MEM
    val forwardA = Output(UInt(2.W))
    val forwardB = Output(UInt(2.W))
  })

  // Default: No forwarding
  io.forwardA := 0.U
  io.forwardB := 0.U

  // ============================== FORWARD A (for rs1) ============================== //
  when(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs1)) {
    // EX/MEM hazard: Forward from MEM stage (most recent data)
    io.forwardA := 2.U
  } .elsewhen(io.mem_wb_regWrite && (io.mem_wb_rd =/= 0.U) && (io.mem_wb_rd === io.id_ex_rs1)) {
    // MEM/WB hazard: Forward from WB stage (only if MEM didn't match)
    io.forwardA := 1.U
  } .otherwise {
    // No hazard: Use data from ID/EX pipeline register
    io.forwardA := 0.U
  }


  //============================= FORWARD B (for rs2) ============================== //
  when(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs2)) {
    io.forwardB := 2.U
  } .elsewhen(io.mem_wb_regWrite && (io.mem_wb_rd =/= 0.U) && (io.mem_wb_rd === io.id_ex_rs2)) {
    io.forwardB := 1.U
  } .otherwise {
    io.forwardB := 0.U
  }
}







/*

############################# LEGACY CODE #############################
  // EX Hazard (Forward from MEM stage)
  when(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs1)) {
    io.forwardA := 2.U
  }
  when(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs2)) {
    io.forwardB := 2.U
  }

  // MEM Hazard (Forward from WB stage)
  // Only forward if EX hazard didn't already handle it
  when(io.mem_wb_regWrite && (io.mem_wb_rd =/= 0.U) && (io.mem_wb_rd === io.id_ex_rs1) &&
    !(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs1))) {
    io.forwardA := 1.U
  }
  when(io.mem_wb_regWrite && (io.mem_wb_rd =/= 0.U) && (io.mem_wb_rd === io.id_ex_rs2) &&
    !(io.ex_mem_regWrite && (io.ex_mem_rd =/= 0.U) && (io.ex_mem_rd === io.id_ex_rs2))) {
    io.forwardB := 1.U
  }
}

 */