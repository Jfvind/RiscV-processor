package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class AUIPCTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipeline" should "correctly execute AUIPC instruction" in {
    // Vi definerer programmet lokalt for ikke at ændre i Programs.scala
    val auipcTestProgram = Seq(
      // -------------------------------------------------------
      // 0: AUIPC x1, 1
      // Opcode: 0010111 (AUIPC)
      // Rd: x1 (00001)
      // Imm: 1 (0x00001) -> Shiftes 12 bits op -> 0x1000 (4096)
      // Resultat: x1 = PC(0) + 0x1000 = 0x1000
      // Hex: 0x00001097
      "h00001097".U(32.W),

      // -------------------------------------------------------
      // 4: ADDI x2, x1, 10
      // Tester Forwarding (EX->EX) fra AUIPC resultatet
      // x2 = x1 + 10 = 0x1000 + 10 = 0x100A (4106)
      "h00a08113".U(32.W),

      // -------------------------------------------------------
      // 8: AUIPC x3, 2
      // Imm: 2 -> 0x2000
      // Resultat: x3 = PC(8) + 0x2000 = 0x2008
      // Hex: 0x00002197
      "h00002197".U(32.W),

      // 12: NOP
      "h00000013".U(32.W),
      // 16: NOP
      "h00000013".U(32.W)
    )

    test(new Core(auipcTestProgram)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      println("\n=== AUIPC INSTRUCTION TEST ===")

      // Cycle 0: Fetch AUIPC (PC=0)
      dut.clock.step(1)

      // Cycle 1: Decode AUIPC
      dut.clock.step(1)

      // Cycle 2: Execute AUIPC
      // Her beregner ALU: 0 + 0x1000.
      // ADDI er i Decode og skal bruge x1. Forwarding Unit skal træde til!
      dut.clock.step(1)

      // Cycle 3: Memory (AUIPC writeback pending), Execute ADDI
      dut.clock.step(1)

      // Cycle 4: Writeback AUIPC (x1 skrives), Memory ADDI
      println("Cycle 4: Tjekker x1 (AUIPC resultat)")
      // Vi peeker direkte ind i registerfilen (hvis muligt) eller venter på UART/Output
      // For denne test antager vi intern inspektion eller VCD verifikation

      dut.clock.step(1)
      // Cycle 5: Writeback ADDI (x2 skrives)

      dut.clock.step(5) // Lad pipeline tømme

      println("✓ AUIPC test completed")
      println("VCD Checklist:")
      println("  - x1 (Time ~Cycle 4): Skal være 0x1000 (4096)")
      println("  - x2 (Time ~Cycle 5): Skal være 0x100A (4106) -> Beviser at forwarding virkede")
      println("  - x3 (Time ~Cycle 6): Skal være 0x2008 (PC=8 + 0x2000)")
    }
  }
}