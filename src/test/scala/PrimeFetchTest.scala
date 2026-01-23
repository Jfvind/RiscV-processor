import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.Core

class PrimeDiagnosticTest extends AnyFlatSpec with ChiselScalatestTester {
  "Core" should "be diagnosed with full visibility" in {
    test(new Core(programFile = "src/main/resources/prime_bench.mem")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      println("\n--- START AF OPTIMERET DIAGNOSE (1000 CYKLUSSER) ---")
      var uartLog = ""
      
      dut.clock.setTimeout(20000)

      for (cycle <- 0 until 10000) {
        val pc    = dut.io.pc_out.peek().litValue
        val instr = dut.io.instruction.peek().litValue
        val sp    = dut.io.debug_x2.peek().litValue
        val a0    = dut.io.debug_x10.peek().litValue
        val cycleCount = dut.io.debug_mcycle.peek().litValue
        val flush = dut.io.debug_flush.peek().litToBoolean
        val stall = dut.io.debug_stall.peek().litToBoolean
        val uartV = dut.io.uartValid.peek().litToBoolean
        val uartD = dut.io.uartData.peek().litValue
        val aluRes = dut.io.alu_res.peek().litValue
        
        // Opsaml UART data
        if (uartV) {
          uartLog += uartD.toChar
        }

        // Definer hvornår vi ønsker at printe en statuslinje
        val erFørste50    = cycle < 50
        val erInterval    = cycle % 25 == 0
        val erUartAktiv   = uartV
        val erGhostWrite  = uartV && flush
        val rammerMain    = pc == 0x198 // PC for main() fundet i tidligere log
        val erEntryOmråde = pc >= 0x0 && pc <= 0x10

        val skalPrinte = erFørste50 || erInterval || erUartAktiv || erGhostWrite || rammerMain || erEntryOmråde

        if (skalPrinte) {
            val eventTag = if (erGhostWrite) " !!! GHOST WRITE !!!" 
                           else if (rammerMain) " -> ENTRING MAIN"
                           else if (erUartAktiv) f" UART SEND: '${uartD.toChar}'"
                           else ""

            println(f"Cycle $cycle%-4d | PC: 0x$pc%08X | Instr: 0x$instr%08X | SP: 0x$sp%08X | a0: 0x$a0%08X | mcycle: $cycleCount | F: $flush | S: $stall$eventTag")
        }

        // Vi printer ALU resultatet hver 25. cyklus, eller når der sker noget vigtigt
        if (cycle % 25 == 0 || uartV || flush) {
            println(f"Cycle $cycle%-4d | ALU Res (Addr/Target): 0x$aluRes%08X")
        }

        dut.clock.step(1)
      }
      
      println("\n--- DIAGNOSE SLUT ---")
      println(f"Opsamlet UART: '$uartLog'")
      println("---------------------\n")
    }
  }
}