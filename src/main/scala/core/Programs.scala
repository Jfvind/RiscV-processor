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
    "h00508113".U(32.W), // 8: addi x2, x1, 5 (x2 = 0xF)
    //"h0E608113".U(32.W), // (x2 = 0xF0)
    //"hFBB08113".U(32.W), // (x2 = 0x5)
    //"h31608113".U(32.W), // (x2 = 0x320)

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
    //"h0021a823".U(32.W), // sw x4, 16(x3)

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
  // Program 4: Load Instruction Test
  val loadTest = Seq(
    // Test 1: Simple Load/Store
    "h06400093".U(32.W), //  0: addi x1, x0, 100    (x1 = 100)
    "h0010a023".U(32.W), //  4: sw   x1, 0(x1)      (Store 100 to addr 100)
    "h06402083".U(32.W), //  8: lw   x1, 100(x0)    (Load from addr 100 → x1 = 100)

    // Test 2: Load into different register
    "h06402103".U(32.W), // 12: lw   x2, 100(x0)    (x2 = 100)

    // Test 3: Load with offset
    "h01e00193".U(32.W), // 16: addi x3, x0, 30     (x3 = 30)
    "h0031a223".U(32.W), // 20: sw   x3, 4(x3)      (Store 30 to addr 34)
    "h00418203".U(32.W), // 24: lw   x4, 4(x3)      (Load from addr 34 → x4 = 30)

    // End
    "h00000013".U(32.W), // 28: nop
    "h00000013".U(32.W), // 32: nop
    "h00000013".U(32.W)  // 36: nop
  )

  // Program 5: Load-Use Hazard Test
  val loadUseHazardTest = Seq(
    // Setup: Store value to memory
    "h06400093".U(32.W), //  0: addi x1, x0, 100    (x1 = 100)
    "h0640a023".U(32.W), //  4: sw   x1, 0(x1)      (Store 100 to addr 100)

    // CRITICAL TEST: Load-Use Hazard
    // The 'add' immediately uses x2, which is being loaded
    // This REQUIRES a 1-cycle stall
    "h06402103".U(32.W), //  8: lw   x2, 100(x0)    (Load: x2 = 100)
    "h002081b3".U(32.W), // 12: add  x3, x1, x2    (Use: x3 = x1 + x2 = 200)
    //                                               ^ HAZARD! x2 not ready yet

    // Another test: No hazard (NOP between load and use)
    "h06402203".U(32.W), // 16: lw   x4, 100(x0)    (Load: x4 = 100)
    "h00000013".U(32.W), // 20: nop                (Bubble - no hazard)
    "h004082b3".U(32.W), // 24: add  x5, x1, x4    (Use: x5 = 200, no stall needed)

    // End
    "h00000013".U(32.W), // 28: nop
    "h00000013".U(32.W), // 32: nop
  )
  // Program 6: JAL Test
  val jalTest = Seq(
    // Test 1: Simple JAL forward
    "h00c000ef".U(32.W), //  0: jal x1, 12        (x1 = PC+4 = 4, jump to PC+12 = 12)
    "h00100113".U(32.W), //  4: addi x2, x0, 1    (SKIPPED - should not execute)
    "h00200193".U(32.W), //  8: addi x3, x0, 2    (SKIPPED - should not execute)
    "h00a00213".U(32.W), // 12: addi x4, x0, 10   (TARGET - x4 = 10)

    // Test 2: Verify return address
    "h00108293".U(32.W), // 16: addi x5, x1, 1    (x5 = x1 + 1 = 4 + 1 = 5)

    // End
    "h00000013".U(32.W), // 20: nop
    "h00000013".U(32.W), // 24: nop
  )
  // Program 7: JALR Test
  // Tester både jump target calculation og link-register (return address) logic
  val jalrTest = Seq(
    // 0: Setup: x1 = 20 (target address)
    // notice: x1 bliver opdateret her. Hvis vi ikke har forwarding, har vi en hazard i næste instruktion!
    "h01400093".U(32.W),

    // 4: JALR x1, x1, 0
    // rs1 = x1 (20). Target = 20.
    // rd = x1. Gemmer PC+4 = 8 i x1.
    // Bemærk: Vi bruger x1 som både source og destination.
    "h000080e7".U(32.W),

    // --- SKIPPED ZONE ---
    "h00200113".U(32.W), //  8: addi x2, x0, 2 (Burde blive flushet/skipped)
    "h00300193".U(32.W), // 12: addi x3, x0, 3 (Burde blive flushet/skipped)
    "h00000013".U(32.W), // 16: nop            (Buffer/Branch delay slot hvis relevant)

    // --- TARGET ZONE (Address 20 = 0x14) ---
    // 20: addi x4, x0, 10
    // Bevis på at vi landede her. x4 skal være 10.
    "h00a00213".U(32.W),

    // 24: Verify return address
    // x1 skulle nu indeholde 8 (fra JALR instruktionen i addr 4)
    // x5 = x1 + 1 = 8 + 1 = 9
    "h00108293".U(32.W),

    "h00000013".U(32.W)  // 28: nop
  )
}
