package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.simulator.WriteVcdAnnotation

class CoreDiagnosticSpec extends AnyFlatSpec with ChiselScalatestTester with RISCVTestHelpers {

  def testCore(program: Seq[UInt])(testFn: Core => Unit): Unit = {
    test(new Core(program = program)).withAnnotations(Seq(WriteVcdAnnotation))(testFn)
  }

  behavior of "RISC-V Core Diagnostics"

  it should "pass Test 1: CSR Read-After-Write Forwarding" in {
    testCore(DiagnosticPrograms.csrForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrForwarding, Map(10 -> 0, 11 -> 0), cycles = 50)
      val x10 = dut.io.debug_x10.peek().litValue
      val x11 = dut.io.debug_x11.peek().litValue
      assert(x11 == x10 + 10, f"FORWARDING FAILURE: x11 (0x$x11%X) != x10 (0x$x10%X) + 10")
      println("✅ CSR Forwarding Logic Verified")
    }
  }

  it should "pass Test 2: CSR Double Read" in {
    testCore(DiagnosticPrograms.csrDoubleRead) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrDoubleRead, Map(), cycles = 50)
      val x14 = dut.io.debug_x14.peek().litValue
      assert((x14 & 0x80000000L) != 0, f"Subtraction result should be negative. Got 0x$x14%X")
    }
  }

  it should "pass Test 3: JAL Forwarding" in {
    testCore(DiagnosticPrograms.jalForwarding) { dut =>
      // x10 should be RA (4)
      runDiagnosticTest(dut, DiagnosticPrograms.jalForwarding, Map(10 -> 4))
    }
  }

  it should "pass Test 4: AUIPC Forwarding" in {
    testCore(DiagnosticPrograms.auipcForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.auipcForwarding,
        Map(10 -> 0x1000, 11 -> 0x100A))
    }
  }

  it should "pass Test 5: CSR Load-Use Hazard" in {
    testCore(DiagnosticPrograms.csrLoadUse) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrLoadUse,
        Map(11 -> 100))
    }
  }

  it should "pass Test 11: Nested Forwarding" in {
    testCore(DiagnosticPrograms.nestedForwarding) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.nestedForwarding,
        Map(1 -> 10, 2 -> 15, 3 -> 18, 4 -> 22))
    }
  }

  it should "pass Test 12: CSR Write-Read-Write" in {
    testCore(DiagnosticPrograms.csrWriteReadWrite) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.csrWriteReadWrite,
        Map(10 -> 0, 11 -> 10, 12 -> 0x12345000L))
    }
  }

  it should "pass Test 14: SyncReadMem Stall" in {
    testCore(DiagnosticPrograms.syncReadMemStall) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.syncReadMemStall,
        Map(2 -> 100, 3 -> 200, 4 -> 203))
    }
  }

  it should "pass Test 6: UART Status Read" in {
    testCore(DiagnosticPrograms.uartStatus) { dut =>
      runDiagnosticTest(dut, DiagnosticPrograms.uartStatus, Map(10 -> 1), cycles = 1000)
    }
  }

  it should "pass Test 7: UART Write" in {
    testCore(DiagnosticPrograms.uartWrite) { dut =>
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
}