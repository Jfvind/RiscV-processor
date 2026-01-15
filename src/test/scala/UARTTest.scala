import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core._

/**
 * UART Output Verification Tests
 * 
 * Tests the memory-mapped UART output functionality:
 * 1. Correct UART address mapping (x0 @ 200, x1 @ 204, x2 @ 208, etc.)
 * 2. Correct data values being sent to UART
 * 3. UART triggers only when storing to UART address range (200-399)
 * 4. UART doesn't trigger for regular memory or LED addresses
 * 5. Timing correctness between Core and Top module
 */
class UARTTest extends AnyFlatSpec with ChiselScalatestTester {

  // ============================================================================
  // TEST 1: Basic UART Output - Single Register
  // ============================================================================
  "UART" should "output correct value for register x2" in {
    val program = Seq(
      "h00f00093".U(32.W), // 0:  addi x1, x0, 15      // x1 = 15  
      "h0c800113".U(32.W), // 4:  addi x2, x0, 200     // x2 = 200 (UART base)
      "h00508193".U(32.W), // 8:  addi x3, x1, 5       // x3 = 15 + 5 = 20
      "h00112423".U(32.W), // 12: sw   x1, 8(x2)       // Store x1 (15) to address 208 (x2 register address)
      "h00000013".U(32.W), // 16: nop
      "h00000013".U(32.W)  // 20: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 1: Basic UART Output ===")
      
      var uartDetected = false
      
      // Run for enough cycles to let SW reach MEM stage
      for (cycle <- 0 until 15) {
        val valid = c.io.uartValid.peek().litToBoolean
        val addr = c.io.uartAddr.peek().litValue
        val data = c.io.uartData.peek().litValue
        
        if (valid) {
          println(s"✓ Cycle $cycle: UART Write Detected")
          println(s"  Address: $addr (expected 208 for register x2)")
          println(s"  Data: $data (expected 15)")
          
          // Verify correct address (208 = 200 + 8 = register x2 address)
          assert(addr == 208, s"UART address should be 208, got $addr")
          
          // Verify correct data (x1 = 15)
          assert(data == 15, s"UART data should be 15, got $data")
          
          uartDetected = true
        }
        
        c.clock.step(1)
      }
      
      assert(uartDetected, "UART write should have been detected within 15 cycles")
      println("✓ TEST 1 PASSED: UART output correct value\n")
    }
  }
  
  // ============================================================================
  // TEST 2: UART Address Mapping Verification
  // ============================================================================
  "UART" should "correctly map register addresses" in {
    val program = Seq(
      "h0c800093".U(32.W), // 0:  addi x1, x0, 200     // x1 = 200 (base)
      "h00500113".U(32.W), // 4:  addi x2, x0, 5       // x2 = 5 (value to store)
      "h0020a023".U(32.W), // 8:  sw   x2, 0(x1)       // Store to addr 200 (x0)
      "h0020a223".U(32.W), // 12: sw   x2, 4(x1)       // Store to addr 204 (x1)
      "h0020a423".U(32.W), // 16: sw   x2, 8(x1)       // Store to addr 208 (x2)
      "h0020a823".U(32.W), // 20: sw   x2, 16(x1)      // Store to addr 216 (x4)
      "h00000013".U(32.W)  // 24: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 2: UART Address Mapping ===")
      
      val expectedAddresses = Seq(200, 204, 208, 216)
      var addressesSeen = Set[Long]()
      
      for (cycle <- 0 until 20) {
        val valid = c.io.uartValid.peek().litToBoolean
        
        if (valid) {
          val addr = c.io.uartAddr.peek().litValue.toLong
          val data = c.io.uartData.peek().litValue.toLong
          
          println(s"✓ Cycle $cycle: UART Write")
          println(s"  Address: $addr (reg index: ${(addr - 200) / 4})")
          println(s"  Data: $data")
          
          assert(data == 5, s"All stores should write value 5, got $data")
          addressesSeen += addr
        }
        
        c.clock.step(1)
      }
      
      println(s"Addresses seen: ${addressesSeen.toSeq.sorted}")
      
      // Should have seen all 4 UART writes
      assert(addressesSeen.size == 4, 
        s"Should detect 4 UART writes, detected ${addressesSeen.size}")
      
      println("✓ TEST 2 PASSED: UART address mapping correct\n")
    }
  }
  
  // ============================================================================
  // TEST 3: UART Should Not Trigger for Non-UART Addresses
  // ============================================================================
  "UART" should "not trigger for regular memory addresses" in {
    val program = Seq(
      "h00a00093".U(32.W), // 0:  addi x1, x0, 10      // x1 = 10
      "h06400113".U(32.W), // 4:  addi x2, x0, 100     // x2 = 100 (LED address)
      "h00000193".U(32.W), // 8:  addi x3, x0, 0       // x3 = 0 (regular RAM)
      "h00112023".U(32.W), // 12: sw   x1, 0(x2)       // Store to 100 (LED, not UART)
      "h0011a023".U(32.W), // 16: sw   x1, 0(x3)       // Store to 0 (RAM, not UART)
      "h00000013".U(32.W), // 20: nop
      "h00000013".U(32.W)  // 24: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 3: UART Non-Trigger Test ===")
      
      var uartTriggered = false
      
      for (cycle <- 0 until 20) {
        val valid = c.io.uartValid.peek().litToBoolean
        
        if (valid) {
          uartTriggered = true
          val addr = c.io.uartAddr.peek().litValue
          println(s"✗ Cycle $cycle: UART incorrectly triggered for address $addr")
        }
        
        c.clock.step(1)
      }
      
      assert(!uartTriggered, "UART should not trigger for non-UART addresses")
      println("✓ TEST 3 PASSED: UART correctly ignores non-UART addresses\n")
    }
  }
  
  // ============================================================================
  // TEST 4: UART with Forwarded Values (The Critical Test!)
  // ============================================================================
  "UART" should "output correct forwarded value for x2" in {
    // This recreates the exact scenario from uartPipelineTest
    val program = Seq(
      "h0c800193".U(32.W), // 0:  addi x3, x0, 200     // x3 = 200 (base)
      "h00a00093".U(32.W), // 4:  addi x1, x0, 10      // x1 = 10
      "h00508113".U(32.W), // 8:  addi x2, x1, 5       // x2 = 15 (NEEDS FORWARDING!)
      "h01400213".U(32.W), // 12: addi x4, x0, 20      // x4 = 20
      "h00000013".U(32.W), // 16: nop
      "h000202b3".U(32.W), // 20: add  x5, x4, x0      // x5 = 20
      "h0021a423".U(32.W), // 24: sw   x2, 8(x3)       // Store x2 to 208 - CRITICAL!
      "h00000013".U(32.W), // 28: nop
      "h00000013".U(32.W)  // 32: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 4: UART with Forwarded Value (Critical Test) ===")
      println("This test recreates the uartPipelineTest scenario")
      
      var uartDetected = false
      var correctValue = false
      
      for (cycle <- 0 until 20) {
        val valid = c.io.uartValid.peek().litToBoolean
        
        if (valid) {
          val addr = c.io.uartAddr.peek().litValue
          val data = c.io.uartData.peek().litValue
          
          println(s"\n✓ Cycle $cycle: UART Write Detected!")
          println(s"  Address: $addr")
          println(s"  Data: $data")
          
          uartDetected = true
          
          // This is THE critical assertion!
          // x2 should be 15 (not 0), even though it required forwarding
          if (addr == 208) {
            assert(data == 15, 
              s"CRITICAL BUG: x2 should be 15 (forwarded), but got $data")
            correctValue = true
            println("  ✓ CORRECT: x2 = 15 (forwarding worked!)")
          }
        }
        
        c.clock.step(1)
      }
      
      assert(uartDetected, "UART write should have occurred")
      assert(correctValue, "UART should have transmitted correct value (15)")
      
      println("\n✓ TEST 4 PASSED: UART correctly outputs forwarded value!\n")
      println("This confirms that the pipeline forwarding works for UART output")
    }
  }
  
  // ============================================================================
  // TEST 5: Multiple UART Writes in Sequence
  // ============================================================================
  "UART" should "handle multiple sequential UART writes" in {
    val program = Seq(
      "h0c800093".U(32.W), // 0:  addi x1, x0, 200     // x1 = 200 (base)
      "h00a00113".U(32.W), // 4:  addi x2, x0, 10      // x2 = 10
      "h01400193".U(32.W), // 8:  addi x3, x0, 20      // x3 = 20
      "h01e00213".U(32.W), // 12: addi x4, x0, 30      // x4 = 30
      "h0020a423".U(32.W), // 16: sw   x2, 8(x1)       // Write 10 to addr 208
      "h0030a623".U(32.W), // 20: sw   x3, 12(x1)      // Write 20 to addr 212
      "h0040a823".U(32.W), // 24: sw   x4, 16(x1)      // Write 30 to addr 216
      "h00000013".U(32.W)  // 28: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 5: Multiple Sequential UART Writes ===")
      
      case class UartWrite(cycle: Int, addr: Long, data: Long)
      var writes = Seq[UartWrite]()
      
      for (cycle <- 0 until 25) {
        val valid = c.io.uartValid.peek().litToBoolean
        
        if (valid) {
          val addr = c.io.uartAddr.peek().litValue.toLong
          val data = c.io.uartData.peek().litValue.toLong
          writes :+= UartWrite(cycle, addr, data)
          
          println(s"✓ Cycle $cycle: UART Write - Addr: $addr, Data: $data")
        }
        
        c.clock.step(1)
      }
      
      println(s"\nTotal UART writes detected: ${writes.length}")
      assert(writes.length == 3, s"Should detect 3 UART writes, got ${writes.length}")
      
      // Verify the data values
      assert(writes.exists(w => w.addr == 208 && w.data == 10),
        "Should write 10 to address 208")
      assert(writes.exists(w => w.addr == 212 && w.data == 20),
        "Should write 20 to address 212")
      assert(writes.exists(w => w.addr == 216 && w.data == 30),
        "Should write 30 to address 216")
      
      println("✓ TEST 5 PASSED: Multiple UART writes handled correctly\n")
    }
  }
  
  // ============================================================================
  // TEST 6: Top Module Integration (UART Trigger to Serialport)
  // ============================================================================
  "Top Module" should "correctly connect Core to Serialport" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 6: Top Module Integration ===")
      
      // The Top module uses uartPipelineTest program
      // We expect the TX line to go active (start bit = LOW) when UART sends
      
      c.clock.setTimeout(5000) //Change timeout to be greater than 1000 cycles
      
      var txWentLow = false
      val maxCycles = 2000
      
      println("Monitoring TX line for UART activity...")
      
      for (cycle <- 0 until maxCycles) {
        val tx = c.io.tx.peek().litToBoolean
        
        if (!tx && !txWentLow) {
          // TX line went low (start bit)
          println(s"✓ Cycle $cycle: TX line went LOW (UART start bit detected)")
          txWentLow = true
        }
        
        c.clock.step(1)
      }
      
      assert(txWentLow, 
        s"TX line should go low (start bit) within $maxCycles cycles")
      
      println("✓ TEST 6 PASSED: Top module UART connectivity works\n")
    }
  }
  
  // ============================================================================
  // TEST 7: Edge Case - Store to Boundary Addresses
  // ============================================================================
  "UART" should "correctly handle boundary addresses" in {
    val program = Seq(
      "h0ff00093".U(32.W), // 0:  addi x1, x0, 255     // x1 = 255
      "h0c800113".U(32.W), // 4:  addi x2, x0, 200     // x2 = 200 (lower bound)
      "h19000193".U(32.W), // 8:  addi x3, x0, 400     // x3 = 400 (upper bound)
      "h00112023".U(32.W), // 12: sw   x1, 0(x2)       // Store to 200 (should trigger)
      "h0011a023".U(32.W), // 16: sw   x1, 0(x3)       // Store to 400 (should NOT trigger)
      "h00000013".U(32.W)  // 20: nop
    )
    
    test(new Core(program)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      println("\n=== TEST 7: Boundary Address Test ===")
      
      var count = 0
      
      for (cycle <- 0 until 20) {
        val valid = c.io.uartValid.peek().litToBoolean
        
        if (valid) {
          val addr = c.io.uartAddr.peek().litValue
          println(s"✓ Cycle $cycle: UART Write to address $addr")
          
          // UART range is [200, 400), so 400 should NOT trigger
          assert(addr >= 200 && addr < 400, 
            s"UART address $addr out of expected range")
          count += 1
        }
        
        c.clock.step(1)
      }
      
      // Should only see 1 UART write (address 200), not address 400
      assert(count == 1, s"Should see 1 UART write (200 only), got $count")
      
      println("✓ TEST 7 PASSED: Boundary addresses handled correctly\n")
    }
  }
}
