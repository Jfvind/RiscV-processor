import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.Core

class PrimeDiagnosticTest extends AnyFlatSpec with ChiselScalatestTester {
  "Core" should "be diagnosed with full visibility" in {
    test(new Core(programFile = "src/main/resources/prime_bench.mem")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      println("\n--- START AF FULD DIAGNOSE (1000 CYKLUSSER) ---")
      var uartLog = ""
      
      // Vi hæver timeout, så vi kan se de 1000 cyklusser selvom den låser
      dut.clock.setTimeout(2000)

      for (cycle <- 0 until 1000) {
        val pc    = dut.io.pc_out.peek().litValue
        val instr = dut.io.instruction.peek().litValue
        val sp    = dut.io.debug_x2.peek().litValue  // Stack Pointer (x2)
        val a0    = dut.io.debug_x10.peek().litValue // a0 (x10)
        val flush = dut.io.debug_flush.peek().litToBoolean
        val stall = dut.io.debug_stall.peek().litToBoolean
        val uartV = dut.io.uartValid.peek().litToBoolean
        val uartD = dut.io.uartData.peek().litValue
        
        // 1. Log UART aktivitet
        if (uartV) {
          val c = uartD.toChar
          uartLog += c
          println(f"Cycle $cycle | UART SEND: '$c' | PC: 0x$pc%08X")
        }

        // 2. Detekter Ghost Writes (Issue 2)
        if (uartV && flush) {
          println(f"!!! GHOST WRITE DETEKTERET (UART) i Cycle $cycle ved PC: 0x$pc%08X !!!")
        }

        // 3. Detaljeret status hvert 50. taktslag eller hvis der sker noget kritisk
        if (cycle % 50 == 0 || (pc == 0x1A8 && cycle < 150)) {
            println(f"Cycle $cycle | PC: 0x$pc%08X | Instr: 0x$instr%08X | SP: 0x$sp%08X | a0: 0x$a0%08X | Flush: $flush | Stall: $stall")
        }

        dut.clock.step(1)
      }
      
      println("\n--- DIAGNOSE SLUT ---")
      println(f"Opsamlet UART: '$uartLog'")
      println("---------------------\n")
    }
  }
}