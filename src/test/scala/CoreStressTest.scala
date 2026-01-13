import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import core._

class CoreStressTest extends AnyFlatSpec with ChiselScalatestTester {
    "Pipeline" should "pass stress test (Forwarding & Branch Flushing)" in {
        // Load the program
        test(new Core(Programs.pipelineStressTest)) { c =>

        // We are stepping through the pipeline and checking the ALU result in the EX state
        // Latency: Fetch --> Decode --> Execute (Result ready) = 2 cycle delay

        println("--- Starting Pipeline Stress Test ---")

        // Cycle 0-2: Fill pipeline (Fetch I1, Fetch I2 / Decode I1)
        c.clock.step(3)

        // Cycle 3: Execute "addi x1, x0, 10" --> Result need to be 10.
        c.io.alu_res.expect(10.U)
        println("Cycle 3: x1=10 OK")
        c.clock.step(1)

        // Cycle 4: Execute "addi x2, x1, 5" --> Hazard: x1 is used instantly. Fowarding unit needs to deliver 10 fra EX/MEM.
        // Result is expected to be 15. 
        c.io.alu_res.expect(15.U)
        println("Cycle 4: x2=15 (EX-Forwarding) OK")
        c.clock.step(1)

        // Cycle 5: Execute "addi x3, x0, 20" --> No hazard, just fill --> Result needs to be 20.
        c.io.alu_res.expect(20.U)
        println("Cycle 5: x3=20 OK")
        c.clock.step(1)

        // Cycle 6: Execute "add x4, x2, x1". --> Hazard: x2 (15) is in MEM. x1 (10) is in WB. --> Needs MEM-forwarding for x2.
        // Result is expected to be 25
        c.io.alu_res.expect(25.U)
        println("Cycle 6: x4=25 (MEM-Forwarding) OK")
        c.clock.step(1)

        // Cycle 7: Execute "bge x0, x0, 8".
        // Branch condition true (0 >= 0). 
        // BGE uses SLT (Set Less Than). 0 < 0 is false (0).
        // ALU output therefore is 0.
        // Branch-logic in ID/EX should be signaling to flush now.
        c.io.alu_res.expect(0.U)
        println("Cycle 7: Branch Decision OK") 
        c.clock.step(1)

        // Cycle 8: Execute "addi x5, x0, 999" (The one to be flushed).
        // If the instruction in the cycle before worked (in the ID stage), this instruction is now a NOP.
        // NOP = addi x0, x0, 0. Result is 0. If we see 999 the branch flushing malfunctioned.
        c.io.alu_res.expect(0.U) 
        println("Cycle 8: Flush OK (Instruction killed)")
        c.clock.step(1)

        // Cycle 9: Pipeline Bubble (Branch Penalty)
        // It takes one cycle to fetch the new instructions from the branch-adress.
        // That's why we see an extra NOP
        c.io.alu_res.expect(0.U)
        println("Cycle 9: Bubble (Fetch Latency) OK")
        c.clock.step(1)

        // Cycle 10: Execute "addi x6, x0, 100" (Target).
        // Vi need to have jumped correctly
        c.io.alu_res.expect(100.U)
        println("Cycle 8: Jump Target Reached OK")
        c.clock.step(1)
        
        println("--- PIPELINE TORTURE TEST PASSED ---")
        } 
    } 
} 