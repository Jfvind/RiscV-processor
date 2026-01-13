import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core.Core

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipelined Core" should "handle hazards and UART output correctly" in {
    test(new Core) { c =>
      
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