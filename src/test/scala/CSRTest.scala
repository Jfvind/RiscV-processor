package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Vi definerer test-programmet her i samme fil for nemhedens skyld
object CSRTest {
  val csrTest = Seq(
    // ---------------------------------------------------------
    // Test 1: Read cycle counter (CSRRS)
    // ---------------------------------------------------------
    // Inst 0: csrrs a0 (x10), mcycle (0xB00), x0
    // Læser mcycle ind i register x10.
    // Opcode: SYSTEM (1110011), Funct3: CSRRS (010), RD: 10, RS1: 0, CSR: 0xB00
    "hB0002573".U(32.W),

    // ---------------------------------------------------------
    // Test 2: Read instruction counter (CSRRS)
    // ---------------------------------------------------------
    // Inst 4: csrrs a2 (x12), minstret (0xB02), x0
    // Læser minstret ind i register x12.
    "hB0202673".U(32.W),

    // ---------------------------------------------------------
    // Test 3: Write / Swap mscratch (CSRRW)
    // ---------------------------------------------------------
    // Inst 8: addi ra (x1), x0, 10
    // Sætter x1 = 10 (Data vi vil skrive til CSR)
    "h00A00093".U(32.W),

    // Inst 12: csrrw a1 (x11), mscratch (0x340), ra (x1)
    // Skriver x1 (10) til mscratch, og læser den GAMLE værdi til x11
    "h340095F3".U(32.W),

    // ---------------------------------------------------------
    // Test 4: Read verification (CSRRS)
    // ---------------------------------------------------------
    // Inst 16: csrrs a4 (x14), mscratch (0x340), x0
    // Læser mscratch igen til x14. x14 bør nu være 10.
    "h34002773".U(32.W),

    // Inst 20: NOP (addi x0, x0, 0)
    "h00000013".U(32.W),

    // Ekstra NOPS for at lade pipelinen tømme ud
    "h00000013".U(32.W),
    "h00000013".U(32.W),
    "h00000013".U(32.W)
  )
}

class CSRTest extends AnyFlatSpec with ChiselScalatestTester {

  "Core Pipeline" should "execute CSR instructions correctly" in {
    // Vi antager din Core tager imod et program som argument
    test(new Core(CSRTest.csrTest)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      println("\n=== CSR INSTRUCTION TEST START ===")

      // Reset er implicit i ChiselTest, men pipelinen skal fyldes
      // Cycle 0-4: Fetching og decoding af de første instruktioner
      dut.clock.step(5)

      println("--- Step 1: Reading mcycle ---")
      // Vi venter lidt, så mcycle tælleren (som kører i baggrunden) når op på noget målbart
      dut.clock.step(5)

      // På dette tidspunkt bør instruktion 0 (læs mcycle) have passeret Writeback.
      // Hvis du har en 'peek' port til registerfilen, ville vi tjekke x10 her.
      println(s"Debug: Check waveform x10 (a0) for non-zero cycle count")

      println("--- Step 2: Writing mscratch ---")
      // Kør igennem instruktionerne 8 og 12 (addi og csrrw)
      dut.clock.step(10)

      println("--- Step 3: Verifying mscratch ---")
      // Kør instruktion 16 (læs mscratch tilbage)
      dut.clock.step(5)

      // Her burde x14 indeholde værdien 10
      println(s"Debug: Check waveform x14 (a4) should be 10 (0xA)")

      println("\n=== CSR TEST COMPLETED ===")
      println("VIGTIGT: Denne test passer altid (grøn) fordi vi ikke har assertions.")
      println("Du SKAL åbne VCD filen og tjekke at x10, x12 og x14 ændrer sig korrekt.")
    }
  }
}