import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.Core

class PrimeDiagnosticTest extends AnyFlatSpec with ChiselScalatestTester {
  "Core" should "be diagnosed with full visibility" in {
    // Kritiske ændringer: Fjernet VCD annotation for at undgå OutOfMemory
    test(new Core(programFile = "src/main/resources/prime_bench.mem")) { dut =>
      println("\n--- START AF OPTIMERET DIAGNOSE (200.000 CYKLUSSER) ---")
      var uartLog = ""
     
      // Øget timeout til 250.000
      dut.clock.setTimeout(250000)

      // Øget loop til 200.000 cyklusser
      for (cycle <- 0 until 200000) {
        val pc      = dut.io.pc_out.peek().litValue
        val instr   = dut.io.instruction.peek().litValue
        val sp      = dut.io.debug_x2.peek().litValue
        val ra      = dut.io.debug_x1.peek().litValue
        val a0      = dut.io.debug_x10.peek().litValue
        val cycleCount = dut.io.debug_mcycle.peek().litValue
        val flush   = dut.io.debug_flush.peek().litToBoolean
        val stall   = dut.io.debug_stall.peek().litToBoolean
        val uartV   = dut.io.uartValid.peek().litToBoolean
        val uartD   = dut.io.uartData.peek().litValue
        val aluRes  = dut.io.alu_res.peek().litValue
       
        if (uartV) {
          uartLog += uartD.toChar
        }

        val erFørste50    = cycle < 50
        val erInterval    = cycle % 5000 == 0
        val erUartAktiv   = uartV
        val erGhostWrite  = uartV && flush
        val rammerMain    = pc == 0x198

        // RETTELSE HER: Vi tilføjer "cycle < 500" så entry-området kun printer under selve boot-up
        val erEntryOmråde = (pc >= 0x0 && pc <= 0x10) && cycle < 500

        val skalPrinte = erFørste50 || erInterval || erUartAktiv || erGhostWrite || rammerMain || erEntryOmråde

        if (skalPrinte) {
            val eventTag = if (erGhostWrite) " !!! GHOST WRITE !!!"
                           else if (rammerMain && pc == 0x198) " -> ENTRING MAIN"
                           else if (erUartAktiv) f" UART SEND: '${uartD.toChar}'"
                           else ""

            println(f"Cycle $cycle%-5d | PC: 0x$pc%04X | ra: 0x$ra%04X | sp: 0x$sp%04X | a0: 0x$a0%04X | mc: $cycleCount | F: $flush | S: $stall$eventTag")
            println(f"      L--> Instr: 0x$instr%08X | ALU Res: 0x$aluRes%08X")
        }

        dut.clock.step(1)
      }
     
      println("\n--- DIAGNOSE SLUT ---")
      println(f"Opsamlet UART (Rå): '$uartLog'")
      // Denne linje viser os præcis hvilke bytes der blev sendt
      println(f"Opsamlet UART (Hex): ${uartLog.map(c => f"0x${c.toInt}%02X ").mkString}")
      println("---------------------\n")
    }
  }
}