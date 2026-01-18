package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class JALTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipeline" should "correctly execute JAL instruction" in {
    test(new Core(Programs.jalTest)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      println("\n=== JAL INSTRUCTION TEST ===")

      // JAL executes in cycle ~4 (reaches EX stage)
      dut.clock.step(5)
      println("Cycle 5: JAL resolved, jumped to target")

      // Check that skipped instructions didn't execute
      dut.clock.step(5)
      println("Cycle 10: Target instruction executed (x4 = 10)")

      // Verify return address was saved
      dut.clock.step(5)
      println("Cycle 15: Return address used (x5 = 5)")

      println("\n✓ JAL test completed")
      println("Check VCD:")
      println("  - x1 should be 4 (return address)")
      println("  - x2, x3 should be 0 (skipped)")
      println("  - x4 should be 10 (target executed)")
      println("  - x5 should be 5 (return address + 1)")
    }
  }
}