package core

import chisel3._
import chisel3.util._
import core.CSRConstants._

class CSRModule extends Module {
  val io = IO(new Bundle {
    // --- Control Signals ---
    val csr_addr     = Input(UInt(12.W))
    val csr_op       = Input(UInt(3.W))
    val csr_data_in  = Input(UInt(32.W))

    // --- Core Events ---
    val inst_retire  = Input(Bool())     //Høj når instruktion færdiggøres (til instret)
    val pc_in        = Input(UInt(32.W))
    val cause_in     = Input(UInt(32.W))
    val trap_trigger = Input(Bool())

    // --- Outputs ---
    val csr_data_out = Output(UInt(32.W))
    val epc_out      = Output(UInt(32.W))
    val trap_vector  = Output(UInt(32.W))
    val mcycle_out   = Output(UInt(32.W))
  })

  // --- 1. Define Actual Registers ---

  // Counters (64-bit til benchmarks)
  val mcycle    = RegInit(0.U(64.W))
  val minstret  = RegInit(0.U(64.W))

  // Machine Trap Setup & Handling
  val mstatus   = RegInit(0.U(32.W))
  val mie       = RegInit(0.U(32.W))
  val mtvec     = RegInit(0.U(32.W))
  val mscratch  = RegInit(0.U(32.W))
  val mepc      = RegInit(0.U(32.W))
  val mcause    = RegInit(0.U(32.W))
  val mtval     = RegInit(0.U(32.W))
  val mip       = RegInit(0.U(32.W))

  // --- 2. Internal Counters Update Logic ---
  mcycle := mcycle + 1.U

  when(io.inst_retire) {
    minstret := minstret + 1.U
  }

  // --- 3. Read Logic (Output Mux) ---
  io.csr_data_out := 0.U

  switch(io.csr_addr) {
    // --- Performance Counters (User Mode RO - 0xC..) ---
    is(0xC00.U) { io.csr_data_out := mcycle(31,0) }    // cycle
    is(0xC01.U) { io.csr_data_out := mcycle(31,0) }    // time (aliased to cycle)
    is(0xC02.U) { io.csr_data_out := minstret(31,0) }  // instret
    is(0xC80.U) { io.csr_data_out := mcycle(63,32) }   // cycleh
    is(0xC81.U) { io.csr_data_out := mcycle(63,32) }   // timeh (aliased to cycleh)
    is(0xC82.U) { io.csr_data_out := minstret(63,32) } // instreth

    // --- Machine Mode Counters (RW - 0xB..) ---
    is(0xB00.U) { io.csr_data_out := mcycle(31,0) }    // mcycle
    is(0xB02.U) { io.csr_data_out := minstret(31,0) }  // minstret
    is(0xB80.U) { io.csr_data_out := mcycle(63,32) }   // mcycleh
    is(0xB82.U) { io.csr_data_out := minstret(63,32) } // minstreth

    // --- Standard Machine CSRs ---
    is(0x300.U) { io.csr_data_out := mstatus }
    is(0x304.U) { io.csr_data_out := mie }
    is(0x305.U) { io.csr_data_out := mtvec }
    is(0x340.U) { io.csr_data_out := mscratch }
    is(0x341.U) { io.csr_data_out := mepc }
    is(0x342.U) { io.csr_data_out := mcause }
    is(0x343.U) { io.csr_data_out := mtval }
    is(0x344.U) { io.csr_data_out := mip }
  }

  io.epc_out     := mepc
  io.trap_vector := mtvec

  // --- 4. Write Logic Generation ---
  val valid_write = io.csr_op =/= CSR_OP_NOP
  val wdata = Wire(UInt(32.W))
  wdata := 0.U // Default

  // Beregn wdata baseret på CSR operation
  switch(io.csr_op) {
    is(CSR_OP_RRW)  { wdata := io.csr_data_in }
    is(CSR_OP_RRS)  { wdata := io.csr_data_out | io.csr_data_in }
    is(CSR_OP_RRC)  { wdata := io.csr_data_out & ~io.csr_data_in }
    is(CSR_OP_RRWI) { wdata := io.csr_data_in }
    is(CSR_OP_RRSI) { wdata := io.csr_data_out | io.csr_data_in }
    is(CSR_OP_RRCI) { wdata := io.csr_data_out & ~io.csr_data_in }
  }

  // --- 5. Actual Update Logic ---
  when(io.trap_trigger) {
    mepc   := io.pc_in
    mcause := io.cause_in

  } .elsewhen(valid_write) {
    switch(io.csr_addr) {
      is(0x300.U) { mstatus  := wdata }
      is(0x304.U) { mie      := wdata }
      is(0x305.U) { mtvec    := wdata }
      is(0x340.U) { mscratch := wdata }
      is(0x341.U) { mepc     := wdata }
      is(0x342.U) { mcause   := wdata }
      is(0x343.U) { mtval    := wdata }

      // Machine Mode Counter Write Access (Normalt kun skrivbart i M-mode)
      // Vi skal passe på kun at skrive til de bits vi adresserer
      is(0xB00.U) { mcycle   := Cat(mcycle(63,32), wdata) } // Skriv Low
      is(0xB80.U) { mcycle   := Cat(wdata, mcycle(31,0)) }  // Skriv High
      is(0xB02.U) { minstret := Cat(minstret(63,32), wdata) }
      is(0xB82.U) { minstret := Cat(wdata, minstret(31,0)) }
    }
  }
  io.mcycle_out := mcycle(31, 0) // Forbind registeret direkte til den nye port
}