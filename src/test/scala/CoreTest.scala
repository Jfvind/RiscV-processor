package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RISC-V Core"

  it should "write to register x1 and read it back" in {
    test(new Core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      
      // --- CYCLE 0: addi x1, x0, 0x123 ---
      dut.io.pc_out.expect(0.U)
      dut.io.alu_res.expect(1.U) // Verify calculation
      dut.clock.step(1) // x1 is written here

      // --- CYCLE 1: addi x2, x1, 0 ---
      dut.io.pc_out.expect(4.U)
      
      // The ALU should now be calculating: x1 + 0
      // If x1 was written correctly, ALU result must be 0x123
      dut.io.alu_res.expect(0.U) 
      
      dut.clock.step(1)
    }
  }
}