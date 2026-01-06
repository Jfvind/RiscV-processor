package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RISC-V Core"

  it should "execute addi instruction correctly" in {
    test(new Core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      
      // --- CYCLE 0 (Time = 0) ---
      // Reset is done. PC is 0.
      // The instruction 12300093 is currently on the wires.
      // The ALU is calculating 0 + 0x123 = 0x123 (291) combinatorially right now.
      
      dut.io.pc_out.expect(0.U)       // PC is 0
      dut.io.alu_res.expect(291.U)    // ALU result is ready immediately (combinatorial)

      // --- CLOCK EDGE ---
      dut.clock.step(1) 

      // --- CYCLE 1 (Time = 1) ---
      // PC has updated to 4.
      // Register x1 has captured the value 291.
      
      dut.io.pc_out.expect(4.U)       // PC is now 4
    }
  }
}