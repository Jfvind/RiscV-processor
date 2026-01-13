package core
import chisel3._

object Programs {
  
  // Program 1: Tester UART og Pipeline Hazards (Forwarding & Branching)
  // Dette er det program, I har brugt indtil nu.
  val uartPipelineTest = Seq(
    // 1. Setup UART Base Address (x3 = 200)
    "h0c800193".U(32.W), // 0: addi x3, x0, 200
    
    // 2. Test EX Forwarding (Data Hazard)
    // x1 = 10
    "h00a00093".U(32.W), // 4: addi x1, x0, 10
    // x2 = x1 + 5 = 15 (0xF). 
    // x1 is in EX stage when this fetches. Must forward from EX.
    "h00508113".U(32.W), // 8: addi x2, x1, 5 

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

  // Program 2: Blink Program (LED Test)
  // Dette er det program, der var udkommenteret i InstructionFetch.
  val blinkLED = Seq(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON Value)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF Value)
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h03200293".U(32.W), // 12: addi x5, x0, 50     (x5 = 50, Delay Limit)
    
    // --- LOOP START (LED ON) ---
    "h0011a023".U(32.W), // 16: sw   x1, 0(x3)      (Turn LED ON: Mem[100] = 1)
    
    // --- DELAY 1 ---
    "h00000213".U(32.W), // 20: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 24: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 28: bge  x5, x4, -4     (If 50 >= x4, jump back to 24)
    
    // --- Turn OFF ---
    "h0021a023".U(32.W), // 32: sw   x2, 0(x3)      (Turn LED OFF: Mem[100] = 0)
    
    // --- DELAY 2 ---
    "h00000213".U(32.W), // 36: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 40: addi x4, x4, 1      (Increment x4)
    "hfe42dee3".U(32.W), // 44: bge  x5, x4, -4     (If 50 >= x4, jump back to 40)
    
    // --- REPEAT (Jump back to start) ---
    "hfe0050e3".U(32.W)  // 48: bge x0, x0, -32     (Jump back to 16)
  )

  /* 
  ======= PROGRAM 3: Pipeline Stress Test (Torture Test) =======
  Tester: 
    1. Data Hazards (EX --> EX og MEM --> EX forwarding)
    2. Control Hazards (Branch Flushing)
  */
  val pipelineStressTest = Seq(
    // FASE 1: Data Hazards
    // Cycle 0 (Fetch): x1 = 10
    "h00a00093".U(32.W), //  0: addi x1, x0, 10 
    
    // Cycle 1 (Fetch): x2 = x1 + 5 = 15. 
    // Hazard! x1 is in EX. Needs EX-to-EX forwarding.
    "h00508113".U(32.W), //  4: addi x2, x1, 5 

    // Cycle 2 (Fetch): x3 = 20. (Fyld)
    "h01400193".U(32.W), //  8: addi x3, x0, 20

    // Cycle 3 (Fetch): x4 = x2 + x1 = 15 + 10 = 25.
    // Hazard! x2 is in MEM, x1 is in WB. Needs MEM-to-EX and RegFile read.
    "h00110233".U(32.W), // 12: add x4, x2, x1

    // FASE 2: Control Hazards (Branching)
    // Cycle 4 (Fetch): Branch! if 0 >= 0 (True) jump +8 (til PC=24)
    "h00005463".U(32.W), // 16: bge x0, x0, 8
    
    // Cycle 5 (Fetch): NEEDS FLUSHING. x5 = 999.
    // This instruction is created in the pipeline but should die (become NOP)
    "h3e700293".U(32.W), // 20: addi x5, x0, 999 

    // Cycle 6 (Fetch): Target. x6 = 100.
    "h06400313".U(32.W), // 24: addi x6, x0, 100

    // End loop
    "h00000013".U(32.W), // 28: nop
    "h00000013".U(32.W), // 32: nop
    "h00000013".U(32.W)  // 36: nop
  )
}



