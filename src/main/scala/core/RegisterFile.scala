// Contains the 32 general-purpose registers (x0-x31). It handles reading two registers and writing to one.
package core

import chisel3._

/*
32 registers (x0-x31)
2 read ports (rs1, rs2)
1 write port (rd)
x0 hardcoded til 0
 */

class RegisterFile extends Module {

  val io = IO(new Bundle {

    //Read ports
    val rs1_addr = Input(UInt(5.W))
    val rs2_addr = Input(UInt(5.W))
    //Output data
    val rs1_data = Output(UInt(32.W))
    val rs2_data = Output(UInt(32.W))
    // Write port
    val rd_addr = Input(UInt(5.W))
    val rd_data = Input(UInt(32.W))
    val reg_write = Input(Bool())
    //Debugging output
    val debug_x1  = Output(UInt(32.W)) // ra
    val debug_x2  = Output(UInt(32.W)) // sp
    val debug_x3  = Output(UInt(32.W))
    val debug_x4  = Output(UInt(32.W))
    val debug_x10 = Output(UInt(32.W)) // a0
    val debug_x11 = Output(UInt(32.W)) // a1
    val debug_x12 = Output(UInt(32.W)) // a2
    val debug_x13 = Output(UInt(32.W)) // a3
    val debug_x14 = Output(UInt(32.W)) // a4
  })


  // --- STORAGE IMPLEMENTATION ---
  //32 regs for debugging purposes when ready do MEM for FPGA LUT RAM as this will be huge in.
  //flip flops.
  val registers = Mem(32, UInt(32.W))  //RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  // PROD / SYNTHESIS MODE:
  // val registers = Mem(32, UInt(32.W))

  // --- WRITE LOGIC ---
  // Write happens on the rising edge of the clock
  // We guard the write with two conditions:
  // 1. reg_write is high (write enable).
  // 2. rd_addr is NOT 0 (x0 = 0)
  when(io.reg_write && io.rd_addr =/= 0.U) {
    registers(io.rd_addr) := io.rd_data
  }

  // --- READ LOGIC (WITH BYPASS) ---
  // Asynchronous read (combinational logic). (Should it be syncronous????? ==!!!==)

  // BYPASS LOGIC FIX:
  // Check if we are trying to read from the s ame register that is currently being written to.
  // If yes -> Forward the 'rd_data' directly to the output.
  // If no -> Read from the registers storage as usual.

  val rs1_hazard = io.reg_write && io.rs1_addr =/= 0.U && io.rs1_addr === io.rd_addr
  val rs2_hazard = io.reg_write && io.rs2_addr =/= 0.U && io.rs2_addr === io.rd_addr

  // We use a Multiplexer (Mux) to enforce the x0 constraint AND the hazard bypass:
  // If the address is 0, ALWAYS output 0. Otherwise, read from storage.
  io.rs1_data := Mux(rs1_hazard, io.rd_data, 
                 Mux(io.rs1_addr === 0.U, 0.U, registers(io.rs1_addr)))

  io.rs2_data := Mux(rs2_hazard, io.rd_data, 
                 Mux(io.rs2_addr === 0.U, 0.U, registers(io.rs2_addr)))

  // --- DEBUG OUTPUTS ---
  // This safely exposes the internal memory to the outside world
  io.debug_x1  := registers(1)
  io.debug_x2  := registers(2)
  io.debug_x3  := registers(3)
  io.debug_x4  := registers(4)
  io.debug_x10 := registers(10)
  io.debug_x11 := registers(11)
  io.debug_x12 := registers(12)
  io.debug_x13 := registers(13)
  io.debug_x14 := registers(14)
}