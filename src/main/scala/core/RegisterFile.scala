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
  })


  // --- STORAGE IMPLEMENTATION ---
  //32 regs for debugging purposes when ready do MEM for FPGA LUT RAM as this will be huge in
  //flip flops
  val registers = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

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

  // --- READ LOGIC ---
  // Asynchronous read (combinational logic). (Should it be syncronous????? ==!!!==)
  // We use a Multiplexer (Mux) to enforce the x0 constraint:
  // If the address is 0, ALWAYS output 0. Otherwise, read from storage.
  //NOTE: I do not know if this guard is a MUST, but Mr. Claude said it would be a necessity
  io.rs1_data := Mux(io.rs1_addr === 0.U, 0.U, registers(io.rs1_addr))
  io.rs2_data := Mux(io.rs2_addr === 0.U, 0.U, registers(io.rs2_addr))
}