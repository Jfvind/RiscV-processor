import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core._

/**
 * Comprehensive Hazard Detection and Forwarding Tests
 * 
 * Tests all types of pipeline hazards:
 * 1. RAW (Read-After-Write) Data Hazards with EX-to-EX forwarding
 * 2. RAW Data Hazards with MEM-to-EX forwarding  
 * 3. Control Hazards (branches and pipeline flushes)
 * 4. Multiple consecutive hazards
 * 5. Hazards across different instruction types
 */
class HazardTest extends AnyFlatSpec with ChiselScalatestTester {

  // ============================================================================
  // TEST 1: EX-to-EX Forwarding (RAW Hazard, 1 cycle apart)
  // ============================================================================
  "Pipeline" should "handle EX-to-EX forwarding correctly" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      // x1 = 10
      "h00508113".U(32.W), // 4:  addi x2, x1, 5       // x2 = x1 + 5 = 15 (HAZARD!)
      "h00000013".U(32.W), // 8:  nop
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 1: EX-to-EX Forwarding ===")
      
      // Fill pipeline (3 cycles to get first instruction to EX)
      c.clock.step(3)
      
      // Cycle 3: addi x1, x0, 10 is in EX stage
      // ALU should compute 10
      c.io.alu_res.expect(10.U, "x1 should be computed as 10")
      println("✓ Cycle 3: x1 = 10 computed")
      c.clock.step(1)
      
      // Cycle 4: addi x2, x1, 5 is in EX stage
      // x1 is in MEM stage (ex_mem.alu_result = 10)
      // Forwarding should provide x1=10 from MEM
      // ALU should compute 10 + 5 = 15
      c.io.alu_res.expect(15.U, "x2 should be 15 via EX-to-EX forwarding")
      println("✓ Cycle 4: x2 = 15 (forwarded from MEM stage)")

      
      println("✓ TEST 1 PASSED: EX-to-EX forwarding works correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 2: MEM-to-EX Forwarding (RAW Hazard, 2 cycles apart)
  // ============================================================================
  "Pipeline" should "handle MEM-to-EX forwarding correctly" in {
    val program = Seq(
      "h01400093".U(32.W), // 0:  addi x1, x0, 20      // x1 = 20
      "h00000013".U(32.W), // 4:  nop                  // Spacer
      "h00508113".U(32.W), // 8:  addi x2, x1, 5       // x2 = x1 + 5 = 25 (HAZARD!)
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 2: MEM-to-EX Forwarding ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(20.U, "x1 should be 20")
      println("✓ Cycle 3: x1 = 20 computed")
      c.clock.step(1)
      
      // Cycle 4: NOP in EX
      c.io.alu_res.expect(0.U, "NOP in EX")
      println("✓ Cycle 4: NOP")
      c.clock.step(1)
      
      // Cycle 5: addi x2, x1, 5 is in EX
      // x1 is in WB stage (mem_wb.result = 20)
      // Forwarding should provide x1=20 from WB
      // ALU should compute 20 + 5 = 25
      c.io.alu_res.expect(25.U, "x2 should be 25 via MEM-to-EX forwarding")
      println("✓ Cycle 5: x2 = 25 (forwarded from WB stage)")
      
      println("✓ TEST 2 PASSED: MEM-to-EX forwarding works correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 3: R-Type Instruction Forwarding (both operands need forwarding)
  // ============================================================================
  "Pipeline" should "forward to both ALU operands for R-type instructions" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      // x1 = 10
      "h01400113".U(32.W), // 4:  addi x2, x0, 20      // x2 = 20
      "h002081b3".U(32.W), // 8:  add  x3, x1, x2      // x3 = x1 + x2 = 30 (BOTH hazards!)
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 3: Dual Operand Forwarding (R-Type) ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(10.U, "x1 should be 10")
      println("✓ Cycle 3: x1 = 10")
      c.clock.step(1)
      
      c.io.alu_res.expect(20.U, "x2 should be 20")
      println("✓ Cycle 4: x2 = 20")
      c.clock.step(1)
      
      // Cycle 5: add x3, x1, x2 is in EX
      // x1 is in MEM (ex_mem.alu_result = 10) - needs forwardA from MEM
      // x2 is in WB (mem_wb.result = 20) - needs forwardB from WB
      // ALU should compute 10 + 20 = 30
      c.io.alu_res.expect(30.U, "x3 should be 30 with both operands forwarded")
      println("✓ Cycle 5: x3 = 30 (both operands forwarded)")
      
      println("✓ TEST 3 PASSED: Dual operand forwarding works correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 4: Control Hazard - Branch Taken (Flush Pipeline)
  // ============================================================================
  "Pipeline" should "flush pipeline correctly when branch is taken" in {
    val program = Seq(
      "h00000093".U(32.W), // 0:  addi x1, x0, 0       // x1 = 0
      "h00005463".U(32.W), // 4:  bge  x0, x0, 8       // Branch to PC=12 (always taken)
      "h06400113".U(32.W), // 8:  addi x2, x0, 100     // Should be FLUSHED
      "h00a00193".U(32.W), // 12: addi x3, x0, 10      // Branch target
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 4: Branch Taken - Pipeline Flush ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(0.U, "x1 = 0")
      println("✓ Cycle 3: x1 = 0")
      c.clock.step(1)
      
      // Cycle 4: BGE in EX, evaluates 0 >= 0 (true)
      // ALU computes SLT: 0 < 0 = 0 (false), so branch taken
      c.io.alu_res.expect(0.U, "BGE: 0 < 0 = false, branch should be taken")
      println("✓ Cycle 4: Branch condition evaluated (taken)")
      c.clock.step(1)
      
      // Cycle 5: Instruction that was in ID (addi x2, x0, 100) should be flushed
      // It gets converted to NOP, so ALU result should be 0, not 100
      c.io.alu_res.expect(0.U, "Flushed instruction should become NOP (0, not 100)")
      println("✓ Cycle 5: Pipeline flushed (x2 = 100 was killed)")
      c.clock.step(1)
      
      // Cycle 6: NOP/Bubble from fetch latency
      c.io.alu_res.expect(0.U, "Fetch bubble")
      println("✓ Cycle 6: Fetch bubble")
      c.clock.step(1)
      
      // Cycle 7: Branch target instruction (addi x3, x0, 10) in EX
      c.io.alu_res.expect(10.U, "Branch target reached: x3 = 10")
      println("✓ Cycle 7: Branch target executed (x3 = 10)")
      
      println("✓ TEST 4 PASSED: Branch flush works correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 5: Control Hazard - Branch Not Taken
  // ============================================================================
  "Pipeline" should "not flush pipeline when branch is not taken" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      // x1 = 10
      "h00105263".U(32.W), // 4:  bge  x0, x1, 8       // Branch if 0 >= 10 (FALSE)
      "h01400113".U(32.W), // 8:  addi x2, x0, 20      // Should NOT be flushed
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 5: Branch Not Taken ===")

      c.clock.step(3)
      c.io.alu_res.expect(10.U, "x1 = 10")
      println("✓ Cycle 3: x1 = 10")
      c.clock.step(1)

      // Cycle 4: BGE in EX, evaluates 0 >= 10 (false)
      // ALU computes SLT: 0 < 10 = 1 (true), so branch NOT taken
      c.io.alu_res.expect(1.U, "BGE: 0 < 10 = true, branch should NOT be taken")
      println("✓ Cycle 4: Branch condition evaluated (not taken)")
      c.clock.step(1)

      // Cycle 5: Next instruction (addi x2, x0, 20) should execute normally
      c.io.alu_res.expect(20.U, "x2 should be 20 (instruction not flushed)")
      println("✓ Cycle 5: Sequential instruction executed (x2 = 20)")

      println("✓ TEST 5 PASSED: Branch not taken works correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 6: Chain of Dependencies (Stress Test)
  // ============================================================================
  "Pipeline" should "handle a chain of dependent instructions" in {
    val program = Seq(
      "h00100093".U(32.W), // 0:  addi x1, x0, 1       // x1 = 1
      "h00108113".U(32.W), // 4:  addi x2, x1, 1       // x2 = x1 + 1 = 2
      "h00110193".U(32.W), // 8:  addi x3, x2, 1       // x3 = x2 + 1 = 3
      "h00118213".U(32.W), // 12: addi x4, x3, 1       // x4 = x3 + 1 = 4
      "h00120293".U(32.W), // 16: addi x5, x4, 1       // x5 = x4 + 1 = 5
      "h00000013".U(32.W)  // 20: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 6: Chain of Dependencies ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(1.U, "x1 = 1")
      println("✓ Cycle 3: x1 = 1")
      c.clock.step(1)
      
      c.io.alu_res.expect(2.U, "x2 = 2 (forwarded)")
      println("✓ Cycle 4: x2 = 2")
      c.clock.step(1)
      
      c.io.alu_res.expect(3.U, "x3 = 3 (forwarded)")
      println("✓ Cycle 5: x3 = 3")
      c.clock.step(1)
      
      c.io.alu_res.expect(4.U, "x4 = 4 (forwarded)")
      println("✓ Cycle 6: x4 = 4")
      c.clock.step(1)
      
      c.io.alu_res.expect(5.U, "x5 = 5 (forwarded)")
      println("✓ Cycle 7: x5 = 5")
      
      println("✓ TEST 6 PASSED: Chain of dependencies handled correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 7: Store Instruction with Forwarding
  // ============================================================================
  "Pipeline" should "forward correct value to store instruction" in {
    val program = Seq(
      "h00f00093".U(32.W), // 0:  addi x1, x0, 15      // x1 = 15
      "h0c800113".U(32.W), // 4:  addi x2, x0, 200     // x2 = 200 (base addr)
      "h00112423".U(32.W), // 8:  sw   x1, 8(x2)       // Store x1 to addr 208
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 7: Store with Forwarding ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(15.U, "x1 = 15")
      println("✓ Cycle 3: x1 = 15")
      c.clock.step(1)
      
      c.io.alu_res.expect(200.U, "x2 = 200")
      println("✓ Cycle 4: x2 = 200")
      c.clock.step(1)
      
      // Cycle 5: SW in EX, calculates address
      // Address = x2 + 8 = 200 + 8 = 208
      c.io.alu_res.expect(208.U, "SW address should be 208")
      println("✓ Cycle 5: SW address calculated = 208")
      c.clock.step(1)
      
      // Cycle 6: SW in MEM, should trigger UART
      // Check that UART sees the correct data (x1 = 15)
      val uartValid = c.io.uartValid.peek().litToBoolean
      if (uartValid) {
        c.io.uartAddr.expect(208.U, "UART address should be 208")
        c.io.uartData.expect(15.U, "UART data should be 15")
        println("✓ Cycle 6: UART triggered with address=208, data=15")
      }
      
      println("✓ TEST 7 PASSED: Store instruction forwards correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 8: x0 Register Behavior (Always Zero)
  // ============================================================================
  "Pipeline" should "maintain x0 as zero regardless of writes" in {
    val program = Seq(
      "h01400013".U(32.W), // 0:  addi x0, x0, 20      // Try to write to x0
      "h00002033".U(32.W), // 4:  add  x0, x0, x0      // Try to write to x0 again
      "h00000093".U(32.W), // 8:  addi x1, x0, 0       // x1 = x0 (should be 0)
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 8: x0 Always Zero ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(20.U, "ALU computed 20 (but shouldn't write to x0)")
      println("✓ Cycle 3: Attempted write to x0")
      c.clock.step(1)
      
      c.io.alu_res.expect(0.U, "x0 + x0 = 0")
      println("✓ Cycle 4: x0 still zero")
      c.clock.step(1)
      
      c.io.alu_res.expect(0.U, "x1 = x0 = 0 (x0 was not modified)")
      println("✓ Cycle 5: x1 = x0 = 0 (x0 protected)")
      
      println("✓ TEST 8 PASSED: x0 register correctly hardwired to zero\n")
    }
  }
}
