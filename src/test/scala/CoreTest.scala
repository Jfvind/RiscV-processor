import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core._


class CoreTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipelined Core" should "handle hazards and UART output correctly" in {
    val program = Seq(
    // 1. Setup UART Base Address (x3 = 200)
    "h0c800193".U(32.W), // 0: addi x3, x0, 200
    
    // 2. Test EX Forwarding (Data Hazard)
    // x1 = 10
    "h00a00093".U(32.W), // 4: addi x1, x0, 10
    // x2 = x1 + 5 = 15 (0xF). 
    // x1 is in EX stage when this fetches. Must forward from EX.
    "h00508113".U(32.W), // 8: addi x2, x1, 5 (x2 = F)
    //"hFBB08113".U(32.W), // (x2 = 5)

    // 3. Test MEM Forwarding (Data Hazard)
    // x4 = 20
    "h01400213".U(32.W), // 12: addi x4, x0, 20
    // NOP (addi x0, x0, 0) to put x4 into MEM stage
    "h00000013".U(32.W), // 16: nop
    // x5 = x4 + x0 = 20 (0x14). 
    // Uses R-Type ADD. Must forward x4 from MEM.
    "h000202b3".U(32.W), // 20: add x5, x4, x0

    // 4. Test UART Output
    // Write x2 (15) to UART Address for "x2" (200 + 2*4 = 208)
    "h0021a423".U(32.W), // 24: sw x2, 8(x3)

    // 5. Test Branch Flushing (Control Hazard)
    // We use BGE x0, x0, 8. 
    // Since 0 >= 0 is always TRUE, this acts as an unconditional jump.
    // It jumps over the next instruction (PC+8 -> 36).
    "h00005463".U(32.W), // 28: bge x0, x0, 8 
    
    // This instruction should be FLUSHED (never executed)
    "h3e700513".U(32.W), // 32: addi x10, x0, 999 (SHOULD NOT HAPPEN)

    // Target of branch
    "h00100513".U(32.W), // 36: addi x10, x0, 1 (x10 should be 1)
    
    // End loop
    "h00000013".U(32.W), // 40: nop
    "h00000013".U(32.W), // 44: nop
    "h00000013".U(32.W), // 48: nop
  )
    test(new Core(program)) { c =>
      
      // Helper to step and print PC for debugging
      def step(n: Int = 1): Unit = {
        for (_ <- 0 until n) {
          c.clock.step(1)
        }
      }

      println("--- Starting Simulation ---")

      // 1. Run for a few cycles to let the pipeline fill
      // Instructions:
      // Cycle 0: Fetch ADDI x3 (Setup)
      // Cycle 1: Fetch ADDI x1 (x1=10)
      // Cycle 2: Fetch ADDI x2 (x2=x1+5). Hazard! x1 is in EX.
      step(5)

      // By now, x2 calculation should be done or in progress.
      // Let's wait until the SW instruction (Instruction 24) hits the Memory stage.
      // That is roughly 5 instructions * 1 cycle + pipeline depth.
      
      var uartDetected = false
      
      // Run for 20 cycles and monitor outputs
      for (i <- 0 until 20) {
        val pc = c.io.pc_out.peek().litValue
        val valid = c.io.uartValid.peek().litToBoolean
        val addr = c.io.uartAddr.peek().litValue
        val data = c.io.uartData.peek().litValue
        
        // Check for UART Write
        if (valid) {
          println(f"[Cycle $i] UART WRITE DETECTED!")
          println(f"  Address: $addr%d (Expected 208 for x2)")
          println(f"  Data:    $data%d (Expected 15)")
          
          assert(addr == 208, "Error: UART Address should be 208 (for register x2)")
          assert(data == 15,  "Error: UART Data should be 15 (Forwarding failed if this is 0 or 5)")
          uartDetected = true
        }
        
        c.clock.step(1)
      }

      assert(uartDetected, "Error: UART Write never occurred within 20 cycles")
      println("--- UART & Forwarding Test Passed ---")
    }
  }
  
  "Top Module" should "toggle TX pin" in {
    // This tests the physical connection in Top.scala
    test(new Top) { c =>
      c.clock.setTimeout(10000) // Allow many cycles
      
      println("--- Waiting for TX activity ---")
      
      // Run until TX goes low (Start Bit)
      // The UART is idle HIGH. When it starts sending, it pulls TX LOW.
      var txActive = false
      for (_ <- 0 until 2000) { // Give it time to reach the SW instruction
        if (c.io.tx.peek().litValue == 0) {
          txActive = true
        }
        c.clock.step(1)
      }
      
      assert(txActive, "Error: TX pin never went low. UART did not trigger.")
      println("--- Top Module Connectivity Passed ---")
    }
  }
}