package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RISC-V Core"

  it should "write to register x1 and read it back" in {
    test(new Core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      
      // --- CYCLE 0: ---
      dut.io.pc_out.expect(0.U)
      dut.io.alu_res.expect(1.U) // addi x1, x0, 1


      dut.clock.step(1)


      // --- CYCLE 1: ---
      dut.io.pc_out.expect(4.U)
      dut.io.alu_res.expect(0.U) // addi x2, x0, 0
      
      
      dut.clock.step(1)
    }
  }
}