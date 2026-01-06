package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RISC-V Core"

  it should "execute addi instruction correctly" in {
    test(new Core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // The simulation starts with Reset high for one cycle automatically.
      
      // Cycle 0: Reset is released. PC = 0.
      // Fetch: Instruction at 0x0 is fetched (addi x5, x0, 0)
      // Decode: Control Unit sees ADDI. Sets ALUOp=ADD, ALUSrc=Imm.
      // Execute: ALU adds x0 (0) + Imm (0). Result = 0.
      // WriteBack: x5 gets written with 0 at the END of this cycle (rising edge of next).
      
      dut.clock.step(1)
      
      // Let's verify the PC moved
      dut.io.pc_out.expect(0.U) // PC is 0 during the first instruction execution
      
      // Check the ALU result for the first instruction
      // If program.hex has 00000293 (addi x5, x0, 0), result should be 0
      dut.io.alu_res.expect(0x123.U)

      dut.clock.step(1)
      
      // Cycle 1: PC should be 4
      dut.io.pc_out.expect(4.U)
      
      // If you had a second instruction, you could check it here.
      // For example, if 2nd instr is: addi x6, x0, 10 (00a00313)
      // dut.io.alu_res.expect(10.U)
    }
  }
}