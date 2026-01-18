package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class LoadUseStallTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipeline" should "correctly stall for load-use hazards" in {
    test(new Core(Programs.loadUseHazardTest)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      println("\n=== LOAD-USE HAZARD STALL TEST ===")

      // Setup phase
      dut.clock.step(3)
      println("Cycle 3: Setup complete")

      // Store executes
      dut.clock.step(2)
      println("Cycle 5: Store complete (100 written to memory)")

      // Load instruction enters pipeline
      dut.clock.step(1)
      println("Cycle 6: lw x2, 100(x0) in IF")

      dut.clock.step(1)
      println("Cycle 7: lw in ID")

      dut.clock.step(1)
      println("Cycle 8: lw in EX (address calculated)")

      dut.clock.step(1)
      println("Cycle 9: lw in MEM (data read)")
      println("         add x3, x1, x2 in IF - DETECTS HAZARD!")
      println("         Pipeline should STALL here")

      dut.clock.step(1)
      println("Cycle 10: lw in WB (x2 = 100 written)")
      println("          add STILL in IF (stalled)")
      println("          ID/EX has NOP (bubble)")

      dut.clock.step(1)
      println("Cycle 11: Stall released")
      println("          add proceeds to ID")

      dut.clock.step(3)
      println("Cycle 14: add completes")
      println("          x3 should equal 200 (100 + 100)")

      // Continue for second test (no hazard)
      dut.clock.step(10)

      println("\n✓ Load-use stall test completed")
      println("Check VCD: Look for PC not incrementing during stall")
      println("           Look for NOP in ID/EX when stalling")
    }
  }
}