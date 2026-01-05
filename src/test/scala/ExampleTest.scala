package riscv

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Simple test to verify Chisel setup works correctly
 */
class ExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Example"

  it should "pass through input to output" in {
    test(new Example) { dut =>
      dut.io.in.poke(42.U)
      dut.clock.step()
      dut.io.out.expect(42.U)
    }
  }
}
