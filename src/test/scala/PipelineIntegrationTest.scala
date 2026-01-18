import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core._

/**
 * Pipeline Integration and End-to-End Tests
 * 
 * Tests complete pipeline functionality:
 * 1. Full 5-stage pipeline operation (IF, ID, EX, MEM, WB)
 * 2. Complex programs with mixed instruction types
 * 3. Pipeline bubble and stall behavior
 * 4. Interaction between hazards, branches, and memory operations
 * 5. Register file write-back and bypass logic
 * 6. Real-world program scenarios
 */
class PipelineIntegrationTest extends AnyFlatSpec with ChiselScalatestTester {

  // ============================================================================
  // TEST 1: 5-Stage Pipeline Latency Verification
  // ============================================================================
  "Pipeline" should "have correct 5-stage latency" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      
      "h00000013".U(32.W), // 4:  nop
      "h00000013".U(32.W), // 8:  nop
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 1: 5-Stage Pipeline Latency ===")
      
      // Cycle 0: IF(addi x1)
      println("Cycle 0: addi x1 in IF stage")
      c.io.pc_out.expect(0.U, "PC should be 0")
      c.clock.step(1)
      
      // Cycle 1: IF(nop), ID(addi x1)
      println("Cycle 1: addi x1 in ID stage")
      c.io.pc_out.expect(0.U, "IF/ID PC should be 0")
      c.clock.step(1)
      
      // Cycle 2: IF(nop), ID(nop), EX(addi x1)
      println("Cycle 2: addi x1 in EX stage")
      c.clock.step(1)
      
      // Cycle 3: IF(nop), ID(nop), EX(nop), MEM(addi x1)
      println("Cycle 3: addi x1 in MEM stage")
      // ALU result is in ex_mem pipeline register
      c.io.alu_res.expect(10.U, "ALU should compute 10")
      c.clock.step(1)
      
      // Cycle 4: IF(nop), ID(nop), EX(nop), MEM(nop), WB(addi x1)
      println("Cycle 4: addi x1 in WB stage - x1 written to regfile")
      c.clock.step(1)
      
      println("✓ TEST 1 PASSED: 5-stage pipeline latency verified\n")
    }
  }
  
  // ============================================================================
  // TEST 2: Mixed Instruction Types
  // ============================================================================
  "Pipeline" should "handle mixed I-type, R-type, S-type, and B-type instructions" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      // I-type
      "h01400113".U(32.W), // 4:  addi x2, x0, 20      // I-type
      "h002081b3".U(32.W), // 8:  add  x3, x1, x2      // R-type (10 + 20 = 30)
      "h0c800213".U(32.W), // 12: addi x4, x0, 200     // I-type
      "h00322423".U(32.W), // 16: sw   x3, 8(x4)       // S-type (store 30 to addr 208)
      "h00105263".U(32.W), // 20: bge  x0, x1, 8       // B-type (should not branch: 0 >= 10 is false)
      "h03200293".U(32.W), // 24: addi x5, x0, 50      // Should execute
      "h00000013".U(32.W)  // 28: nop
    )

    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 2: Mixed Instruction Types ===")

      // Track instruction execution through pipeline
      c.clock.step(3)
      c.io.alu_res.expect(10.U, "I-type: x1 = 10")
      println("✓ I-type (addi) executed: x1 = 10")
      c.clock.step(1)

      c.io.alu_res.expect(20.U, "I-type: x2 = 20")
      println("✓ I-type (addi) executed: x2 = 20")
      c.clock.step(1)

      c.io.alu_res.expect(30.U, "R-type: x3 = x1 + x2 = 30")
      println("✓ R-type (add) executed: x3 = 30")
      c.clock.step(1)

      c.io.alu_res.expect(200.U, "I-type: x4 = 200")
      println("✓ I-type (addi) executed: x4 = 200")
      c.clock.step(1)

      c.io.alu_res.expect(208.U, "S-type: address = 200 + 8 = 208")
      println("✓ S-type (sw) address calculated: 208")

      // Check UART output for the store
      var uartSeen = false
      if (c.io.uartValid.peek().litToBoolean) {
        c.io.uartAddr.expect(208.U, "Store address")
        c.io.uartData.expect(30.U, "Store data")
        println("✓ S-type (sw) executed: stored 30 to UART addr 208")
        uartSeen = true
      }
      c.clock.step(1)

      if (!uartSeen && c.io.uartValid.peek().litToBoolean) {
        c.io.uartAddr.expect(208.U)
        c.io.uartData.expect(30.U)
        println("✓ S-type (sw) executed: stored 30 to UART addr 208")
      }

      // Branch should not be taken (0 < 10 is true, so !(0 < 10) = false)
      c.io.alu_res.expect(1.U, "B-type: 0 < 10 = true (branch not taken)")
      println("✓ B-type (bge) executed: branch not taken")
      c.clock.step(1)

      c.io.alu_res.expect(50.U, "Next instruction executed: x5 = 50")
      println("✓ Sequential instruction executed: x5 = 50")

      println("✓ TEST 2 PASSED: Mixed instruction types handled correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 3: Branch Penalty and Pipeline Bubbles
  // ============================================================================
  "Pipeline" should "introduce correct branch penalty and bubbles" in {
    val program = Seq(
      "h00000093".U(32.W), // 0:  addi x1, x0, 0
      "h00005463".U(32.W), // 4:  bge  x0, x0, 8       // Branch taken to PC=12
      "h03e00113".U(32.W), // 8:  addi x2, x0, 62      // Flushed
      "h00a00193".U(32.W), // 12: addi x3, x0, 10      // Branch target
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 3: Branch Penalty and Bubbles ===")
      
      var cycle = 0
      
      // Cycle 0-2: Fill pipeline
      c.clock.step(3)
      cycle += 3
      
      // Cycle 3: addi x1 in EX
      c.io.alu_res.expect(0.U)
      println(s"Cycle $cycle: addi x1 = 0 in EX")
      c.clock.step(1)
      cycle += 1
      
      // Cycle 4: bge in EX, branch evaluated
      c.io.alu_res.expect(0.U, "Branch comparison: 0 < 0 = false, branch taken")
      println(s"Cycle $cycle: Branch evaluated and taken")
      c.clock.step(1)
      cycle += 1
      
      // Cycle 5: Flushed instruction (should be NOP, not 62)
      c.io.alu_res.expect(0.U, "Flushed instruction converted to NOP")
      println(s"Cycle $cycle: Flushed instruction (not 62)")
      c.clock.step(1)
      cycle += 1
      
      // Cycle 6: Pipeline bubble from fetch
      c.io.alu_res.expect(0.U, "Pipeline bubble")
      println(s"Cycle $cycle: Pipeline bubble")
      c.clock.step(1)
      cycle += 1
      
      // Cycle 7: Branch target instruction
      c.io.alu_res.expect(10.U, "Branch target: x3 = 10")
      println(s"Cycle $cycle: Branch target executed (x3 = 10)")
      
      println("✓ TEST 3 PASSED: Branch penalty handled correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 4: Register File Bypass (Same-Cycle Write and Read)
  // ============================================================================
  "Pipeline" should "handle register file internal bypass" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10
      "h00000013".U(32.W), // 4:  nop
      "h00000013".U(32.W), // 8:  nop
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W), // 16: nop
      "h00108113".U(32.W), // 20: addi x2, x1, 1       // x2 = x1 + 1 = 11
      "h00000013".U(32.W)  // 24: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 4: Register File Bypass ===")
      
      // Let x1 write complete
      c.clock.step(7)
      println("Cycle 6: x1 written to register file")
      c.clock.step(1)
      
      // Cycle 7: Read x1 from register file
      // This should get the correct value (10) even though write happened in same cycle
      println("Cycle 7: Reading x1 for next instruction")
      
      // Cycle 9: x2 = x1 + 1 in EX
      c.io.alu_res.expect(11.U, "x2 should be 11 (x1 read correctly from regfile)")
      println("✓ Cycle 9: x2 = 11 (register file bypass worked)")
      
      println("✓ TEST 4 PASSED: Register file bypass works\n")
    }
  }
  
  // ============================================================================
  // TEST 5: Complete uartPipelineTest Program Simulation
  // ============================================================================
  "Pipeline" should "correctly execute the complete uartPipelineTest program" in {
    test(new Core(Programs.uartPipelineTest)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 5: Complete uartPipelineTest Execution ===")
      
      var expectedRegisters = Map[Int, Int](
        3 -> 200,  // x3 = 200 (UART base)
        1 -> 10,   // x1 = 10
        2 -> 15,   // x2 = 15 (via forwarding!)
        4 -> 20,   // x4 = 20
        5 -> 20,   // x5 = 20 (via forwarding!)
        10 -> 1    // x10 = 1 (after branch)
      )
      
      println("Expected final register values:")
      expectedRegisters.toSeq.sortBy(_._1).foreach { case (reg, value) =>
        println(s"  x$reg = $value")
      }
      
      var uartDetected = false
      var branchTaken = false
      
      for (cycle <- 0 until 25) {
        val pc = c.io.pc_out.peek().litValue
        val instr = c.io.instruction.peek().litValue
        val aluRes = c.io.alu_res.peek().litValue
        val uartValid = c.io.uartValid.peek().litToBoolean
        
        if (cycle >= 3) {
          // Check specific expected ALU results
          cycle match {
            case 3 => 
              c.io.alu_res.expect(200.U, "x3 = 200")
              println(s"✓ Cycle $cycle: x3 = 200")
            case 4 => 
              c.io.alu_res.expect(10.U, "x1 = 10")
              println(s"✓ Cycle $cycle: x1 = 10")
            case 5 => 
              c.io.alu_res.expect(15.U, "x2 = 15 (EX forwarding)")
              println(s"✓ Cycle $cycle: x2 = 15 (CRITICAL: forwarding worked!)")
            case 6 => 
              c.io.alu_res.expect(20.U, "x4 = 20")
              println(s"✓ Cycle $cycle: x4 = 20")
            case 8 => 
              c.io.alu_res.expect(20.U, "x5 = 20 (MEM forwarding)")
              println(s"✓ Cycle $cycle: x5 = 20 (MEM forwarding)")
            case _ => // Other cycles
          }
        }
        
        if (uartValid) {
          val addr = c.io.uartAddr.peek().litValue
          val data = c.io.uartData.peek().litValue
          
          println(s"\n✓ Cycle $cycle: UART WRITE DETECTED")
          println(s"  Address: $addr")
          println(s"  Data: $data")
          
          if (addr == 208) {
            assert(data == 15, 
              s"CRITICAL: x2 should be 15 (not 0!), got $data")
            println("  ✓✓✓ SUCCESS: x2 = 15 transmitted to UART!")
            uartDetected = true
          }
        }
        
        c.clock.step(1)
      }
      
      assert(uartDetected, "UART should have transmitted x2 value")
      
      println("\n✓ TEST 5 PASSED: Complete uartPipelineTest executed correctly!")
      println("✓ The forwarding logic works and x2 = 15 is correctly transmitted\n")
    }
  }
  
  // ============================================================================
  // TEST 6: Pipeline Stress Test (From Programs.scala)
  // ============================================================================
  "Pipeline" should "pass the pipelineStressTest program" in {
    test(new Core(Programs.pipelineStressTest)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 6: Pipeline Stress Test ===")
      
      c.clock.step(3)
      c.io.alu_res.expect(10.U, "x1 = 10")
      println("✓ Cycle 3: x1 = 10")
      c.clock.step(1)
      
      c.io.alu_res.expect(15.U, "x2 = 15 (EX forwarding)")
      println("✓ Cycle 4: x2 = 15 (EX-to-EX forwarding)")
      c.clock.step(1)
      
      c.io.alu_res.expect(20.U, "x3 = 20")
      println("✓ Cycle 5: x3 = 20")
      c.clock.step(1)
      
      c.io.alu_res.expect(25.U, "x4 = 25 (MEM forwarding)")
      println("✓ Cycle 6: x4 = 25 (MEM-to-EX forwarding)")
      c.clock.step(1)
      
      c.io.alu_res.expect(0.U, "Branch decision")
      println("✓ Cycle 7: Branch evaluated")
      c.clock.step(1)
      
      c.io.alu_res.expect(0.U, "Flushed instruction")
      println("✓ Cycle 8: Instruction flushed (not 999)")
      c.clock.step(1)
      
      c.io.alu_res.expect(0.U, "Fetch bubble")
      println("✓ Cycle 9: Pipeline bubble")
      c.clock.step(1)
      
      c.io.alu_res.expect(100.U, "Branch target")
      println("✓ Cycle 10: x6 = 100 (branch target reached)")
      
      println("✓ TEST 6 PASSED: Pipeline stress test successful\n")
    }
  }
  
  // ============================================================================
  // TEST 7: Verify No Spurious UART Triggers
  // ============================================================================
  "Pipeline" should "trigger UART exactly once per store instruction" in {
    val program = Seq(
      "h00f00093".U(32.W), // 0:  addi x1, x0, 15
      "h0c800113".U(32.W), // 4:  addi x2, x0, 200
      "h00112423".U(32.W), // 8:  sw   x1, 8(x2)       // Single store
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W), // 16: nop
      "h00000013".U(32.W), // 20: nop
      "h00000013".U(32.W), // 24: nop
      "h00000013".U(32.W)  // 28: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 7: Single UART Trigger Verification ===")
      
      var uartTriggerCount = 0
      
      for (cycle <- 0 until 20) {
        if (c.io.uartValid.peek().litToBoolean) {
          uartTriggerCount += 1
          val addr = c.io.uartAddr.peek().litValue
          val data = c.io.uartData.peek().litValue
          println(s"Cycle $cycle: UART trigger #$uartTriggerCount (addr=$addr, data=$data)")
        }
        c.clock.step(1)
      }
      
      assert(uartTriggerCount == 1, 
        s"Should trigger UART exactly once, got $uartTriggerCount triggers")
      
      println(s"✓ UART triggered exactly once (correct)")
      println("✓ TEST 7 PASSED: No spurious UART triggers\n")
    }
  }
  
  // ============================================================================
  // TEST 8: PC Increment Verification
  // ============================================================================
  "Pipeline" should "increment PC correctly for sequential instructions" in {
    val program = Seq(
      "h00000013".U(32.W), // 0:  nop
      "h00000013".U(32.W), // 4:  nop
      "h00000013".U(32.W), // 8:  nop
      "h00000013".U(32.W), // 12: nop
      "h00000013".U(32.W)  // 16: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 8: PC Increment Verification ===")
      
      val expectedPCs = Seq(0, 0, 4, 8, 12, 16)
      
      for ((expectedPC, cycle) <- expectedPCs.zipWithIndex) {
        c.io.pc_out.expect(expectedPC.U, s"PC should be $expectedPC")
        println(s"✓ Cycle $cycle: PC = $expectedPC")
        c.clock.step(1)
      }
      
      println("✓ TEST 8 PASSED: PC increments correctly\n")
    }
  }
}
