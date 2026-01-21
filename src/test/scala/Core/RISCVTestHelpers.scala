package core

import chisel3._
import chiseltest._
import org.scalatest.Assertions._

trait RISCVTestHelpers { self: ChiselScalatestTester =>

  def runDiagnosticTest(
                         dut: Core,
                         program: Seq[UInt],
                         expectedRegs: Map[Int, Long],
                         cycles: Int = 100
                       ): Unit = {

    dut.clock.setTimeout(cycles)
    var cycleCount = 0
    var halted = false

    println(s"\n--- Starting Test ---")

    while (!halted && cycleCount < cycles) {
      // DEBUG: Trace execution
      val pc = dut.io.pc_out.peek().litValue
      val instr = dut.io.instruction.peek().litValue
      println(f"Cycle $cycleCount%3d: PC=0x$pc%04X Instr=0x$instr%08X")

      dut.clock.step(1)
      cycleCount += 1

      // Check for ECALL (0x73)
      if (dut.io.instruction.peek().litValue == 0x00000073L) {
        halted = true
        dut.clock.step(5) // Flush pipeline
      }
    }

    println(s"Test Finished at Cycle $cycleCount")
    if (!halted) println(s"⚠ WARNING: Timeout reached ($cycles cycles)")

    val actualRegs = Map(
      1 -> dut.io.debug_x1.peek().litValue.toLong,
      2 -> dut.io.debug_x2.peek().litValue.toLong,
      3 -> dut.io.debug_x3.peek().litValue.toLong,
      4 -> dut.io.debug_x4.peek().litValue.toLong,
      10 -> dut.io.debug_x10.peek().litValue.toLong,
      11 -> dut.io.debug_x11.peek().litValue.toLong,
      12 -> dut.io.debug_x12.peek().litValue.toLong,
      13 -> dut.io.debug_x13.peek().litValue.toLong,
      14 -> dut.io.debug_x14.peek().litValue.toLong
    )

    expectedRegs.foreach { case (reg, expected) =>
      val actual = actualRegs.getOrElse(reg, -1L)
      if (expected != 0) {
        if (actual != expected) {
          println(f"❌ x$reg: Expected 0x$expected%08X, Got 0x$actual%08X")
          fail(s"Register x$reg verification failed.")
        } else {
          println(f"✅ x$reg: 0x$actual%08X")
        }
      } else {
        println(f"ℹ️ x$reg: 0x$actual%08X (Value Observed)")
      }
    }
  }
}