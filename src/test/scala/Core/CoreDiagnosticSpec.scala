package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.simulator.WriteVcdAnnotation

class CoreDiagnosticSpec extends AnyFlatSpec with ChiselScalatestTester with RISCVTestHelpers {

  // Universal helper
  def testCore(program: Seq[UInt] = Seq(), file: String = "")(testFn: Core => Unit): Unit = {
    test(new Core(program = program, programFile = file)).withAnnotations(Seq(WriteVcdAnnotation))(testFn)
  }

  behavior of "RISC-V Core Diagnostics"

  // 1-14: Existing Passing Tests
  it should "pass Test 1: CSR Read-After-Write Forwarding" in {
    testCore(program = DiagnosticPrograms.csrForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrForwarding, Map(10 -> 0, 11 -> 0), cycles = 50)
      val x10 = dut.io.debug_x10.peek().litValue
      val x11 = dut.io.debug_x11.peek().litValue
      assert(x11 == x10 + 10, f"FORWARDING FAILURE: x11 (0x$x11%X) != x10 (0x$x10%X) + 10")
      println("✅ CSR Forwarding Logic Verified")
    }
  }

  it should "pass Test 2: CSR Double Read" in {
    testCore(program = DiagnosticPrograms.csrDoubleRead) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrDoubleRead, Map(), cycles = 50)
      val x14 = dut.io.debug_x14.peek().litValue
      assert((x14 & 0x80000000L) != 0, f"Subtraction result should be negative. Got 0x$x14%X")
    }
  }

  it should "pass Test 3: JAL Forwarding" in {
    testCore(program = DiagnosticPrograms.jalForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.jalForwarding, Map(10 -> 4))
    }
  }

  it should "pass Test 4: AUIPC Forwarding" in {
    testCore(program = DiagnosticPrograms.auipcForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.auipcForwarding, Map(10 -> 0x1000, 11 -> 0x100A))
    }
  }

  it should "pass Test 5: CSR Load-Use Hazard" in {
    testCore(program = DiagnosticPrograms.csrLoadUse) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrLoadUse, Map(11 -> 100))
    }
  }

  it should "pass Test 6: UART Status Read" in {
    testCore(program = DiagnosticPrograms.uartStatus) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.uartStatus, Map(10 -> 1), cycles = 1000)
    }
  }

  it should "pass Test 7: UART Write" in {
    testCore(program = DiagnosticPrograms.uartWrite) { dut =>
      dut.clock.setTimeout(200)
      var output = ""
      var cycle = 0
      var done = false
      var flushCycles = 0

      while (cycle < 200 && flushCycles < 10) {
        dut.clock.step(1)
        if (dut.io.uartValid.peek().litToBoolean) {
          output += (dut.io.uartData.peek().litValue.toInt & 0xFF).toChar
        }
        if (dut.io.instruction.peek().litValue == 0x73L) done = true
        if (done) flushCycles += 1
        cycle += 1
      }
      assert(output == "ABC", s"UART Output Mismatch: Expected 'ABC', got '$output'")
      println(s"✅ UART Write: $output")
    }
  }

  it should "pass Test 11: Nested Forwarding" in {
    testCore(program = DiagnosticPrograms.nestedForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.nestedForwarding, Map(1 -> 10, 2 -> 15, 3 -> 18, 4 -> 22))
    }
  }

  it should "pass Test 12: CSR Write-Read-Write" in {
    testCore(program = DiagnosticPrograms.csrWriteReadWrite) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrWriteReadWrite, Map(10 -> 0, 11 -> 10, 12 -> 0x12345000L))
    }
  }

  it should "pass Test 14: SyncReadMem Stall" in {
    testCore(program = DiagnosticPrograms.syncReadMemStall) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.syncReadMemStall, Map(2 -> 100, 3 -> 200, 4 -> 203))
    }
  }

  // ==============================================================================
  // NEW TESTS (Integrated & Fixed Pipeline Flush)
  // ==============================================================================

  it should "pass Minimal C Test (Stack + UART)" in {
    // FIX: Using address 0x200 for stack
    val minimalCTest = Seq(
      "h00000137".U(32.W), // lui sp, 0x00000
      "h20010113".U(32.W), // addi sp, sp, 0x200

      "h00001337".U(32.W), // lui x6, 1
      "h04100393".U(32.W), // addi x7, x0, 'A'
      "h00732023".U(32.W), // sw x7, 0(x6)
      "h00000073".U(32.W)  // ecall
    )

    testCore(program = minimalCTest) { dut =>
      dut.clock.setTimeout(1000)
      var output = ""
      var cycle = 0
      var done = false
      var flushCycles = 0 // FIX: Added flush logic

      while (cycle < 1000 && flushCycles < 10) {
        if (dut.io.uartValid.peek().litToBoolean) {
          output += (dut.io.uartData.peek().litValue.toInt & 0xFF).toChar
        }
        if (dut.io.instruction.peek().litValue == 0x73L) done = true
        if (done) flushCycles += 1

        dut.clock.step(1)
        cycle += 1
      }

      println(f"Stack Setup SP: 0x${dut.io.debug_x2.peek().litValue}%X")
      assert(output == "A", s"Expected 'A', got '$output'")
      println("✅ Minimal C Test Passed")
    }
  }

  it should "pass Stack Manipulation Test" in {
    // FIX: Using address 0x200 for stack
    val stackTest = Seq(
      "h00000137".U(32.W), // lui sp, 0
      "h20010113".U(32.W), // addi sp, sp, 0x200

      "hff010113".U(32.W), // addi sp, sp, -16
      "h00a00093".U(32.W), // addi x1, x0, 10
      "h00112623".U(32.W), // sw x1, 12(sp)
      "h00000013".U(32.W), // nop
      "h00c12083".U(32.W), // lw x1, 12(sp)
      "h01010113".U(32.W), // addi sp, sp, 16
      "h00000073".U(32.W)  // ecall
    )

    testCore(program = stackTest) { dut =>
      dut.clock.setTimeout(100)
      runDiagnosticTest(dut, stackTest, Map(1 -> 10, 2 -> 0x200), cycles = 100)
      println("✅ Stack Test Passed")
    }
  }

  it should "run Prime Benchmark" in {
    val programPath = "src/main/resources/prime_bench.mem"
    println(s"\n--- Starting Prime Benchmark from $programPath ---")

    testCore(file = programPath) { dut =>
      dut.clock.setTimeout(500000)

      var cycle = 0
      var done = false
      var flushCycles = 0
      val stringBuilder = new StringBuilder()

      while (cycle < 500000 && flushCycles < 20) {
        val instr = dut.io.instruction.peek().litValue
        val pc = dut.io.pc_out.peek().litValue

        // Debug trace
        if (cycle % 50000 == 0) {
          // println(f"Cycle $cycle%6d PC=0x$pc%04X")
        }

        if (dut.io.uartValid.peek().litToBoolean) {
          val char = (dut.io.uartData.peek().litValue.toInt & 0xFF).toChar
          print(char)
          stringBuilder.append(char)
        }

        if (instr == 0x00000073L) {
          done = true
          if (flushCycles == 0) println(s"\n\n✅ Program Halted at PC=0x$pc%X Cycle=$cycle (Flushing...)")
        }

        if (done) flushCycles += 1

        dut.clock.step(1)
        cycle += 1
      }

      if (!done) fail("Timeout: Prime benchmark did not finish within 500,000 cycles")
    }
  }
}