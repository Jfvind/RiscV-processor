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

  // Program 2b: Blink Program with UART output (LED + UART Test)
  // Addresses: LED = 100 (0x64), UART Data = 0x1000, UART Status = 0x1004
  // Sends '1' (0x31) when LED on, '0' (0x30) when LED off
  val blinkLED2 = Seq(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON Value)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF Value)
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h00100293".U(32.W), // 12: addi x5, x0, 1      (x5 = 1, Delay Limit - small for testing)
    "h00001337".U(32.W), // 16: lui x6, 1           (x6 = 0x1000, UART Data Address)
    "h03100393".U(32.W), // 20: addi x7, x0, 0x31   (x7 = '1' ASCII = 49)
    "h03000e13".U(32.W), // 24: addi x28, x0, 0x30  (x28 = '0' ASCII = 48)
    "h00430e93".U(32.W), // 28: addi x29, x6, 4     (x29 = 0x1004, UART Status Address)
    
    // --- LOOP START (LED ON + UART '1') --- Address 32
    // ===== Wait for UART Ready =====
    "h000eaf03".U(32.W), // 32: lw x30, 0(x29)      (x30 = UART Status)
    "h001f7f13".U(32.W), // 36: andi x30, x30, 1    (x30 = Ready bit)
    "hfe0f0ce3".U(32.W), // 40: beq x30, x0, -8     (If not ready, wait - jump to 32)
    
    // ===== Send '1' via UART and turn LED ON =====
    "h00732023".U(32.W), // 44: sw x7, 0(x6)        (UART Data = '1')
    "h0011a023".U(32.W), // 48: sw x1, 0(x3)        (LED ON: Mem[100] = 1)
    
    // --- DELAY 1 ---
    "h00000213".U(32.W), // 52: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 56: addi x4, x4, 1      (Increment x4)
    "hfe52dee3".U(32.W), // 60: bge x5, x4, -4      (If limit >= x4, jump to 56)
    
    // ===== Wait for UART Ready =====
    "h000eaf03".U(32.W), // 64: lw x30, 0(x29)      (x30 = UART Status)
    "h001f7f13".U(32.W), // 68: andi x30, x30, 1    (x30 = Ready bit)
    "hfe0f0ce3".U(32.W), // 72: beq x30, x0, -8     (If not ready, wait - jump to 64)
    
    // ===== Send '0' via UART and turn LED OFF =====
    "h01c32023".U(32.W), // 76: sw x28, 0(x6)       (UART Data = '0')
    "h0021a023".U(32.W), // 80: sw x2, 0(x3)        (LED OFF: Mem[100] = 0)
    
    // --- DELAY 2 ---
    "h00000213".U(32.W), // 84: addi x4, x0, 0      (Reset Counter x4 = 0)
    "h00120213".U(32.W), // 88: addi x4, x4, 1      (Increment x4)
    "hfe52dee3".U(32.W), // 92: bge x5, x4, -4      (If limit >= x4, jump to 88)
    
    // --- REPEAT (Jump back to loop start at address 32) ---
    "hfa0050e3".U(32.W)  // 96: bge x0, x0, -96     (Jump back to 32)
  )

  // Program 2c: blinkLED3 - LED blink + UART without status polling (debug version)
  // Uses fixed delays instead of polling to isolate UART status issue
  val blinkLED3 = Seq(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF)
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h00001337".U(32.W), // 12: lui x6, 1           (x6 = 0x1000, UART Address)
    "h03100393".U(32.W), // 16: addi x7, x0, 0x31   (x7 = '1')
    "h03000e13".U(32.W), // 20: addi x28, x0, 0x30  (x28 = '0')
    
    // --- LOOP START --- Address 24
    // === Turn LED ON and send '1' ===
    "h0011a023".U(32.W), // 24: sw x1, 0(x3)        (LED ON)
    "h00732023".U(32.W), // 28: sw x7, 0(x6)        (UART = '1')
    
    // --- LONG DELAY (wait for UART transmission ~10000 cycles) ---
    "h00000213".U(32.W), // 32: addi x4, x0, 0      (x4 = 0)
    "h40000293".U(32.W), // 36: addi x5, x0, 1024   (x5 = 1024)
    "h00120213".U(32.W), // 40: addi x4, x4, 1      (x4++)
    "hfe52dee3".U(32.W), // 44: bge x5, x4, -4      (loop to 40)
    
    // === Turn LED OFF and send '0' ===
    "h0021a023".U(32.W), // 48: sw x2, 0(x3)        (LED OFF)
    "h01c32023".U(32.W), // 52: sw x28, 0(x6)       (UART = '0')
    
    // --- LONG DELAY ---
    "h00000213".U(32.W), // 56: addi x4, x0, 0      (x4 = 0)
    "h40000293".U(32.W), // 60: addi x5, x0, 1024   (x5 = 1024)
    "h00120213".U(32.W), // 64: addi x4, x4, 1      (x4++)
    "hfe52dee3".U(32.W), // 68: bge x5, x4, -4      (loop to 64)
    
    // --- REPEAT ---
    "hfc0050e3".U(32.W)  // 72: bge x0, x0, -64     (jump to 24)
  )

  // Program 2d: ledTest - Minimal LED blink (no UART)  
  // Tests: SW, ADDI, LUI, and BLT/BGE branches
  val ledTest = Seq(
    // --- SETUP ---
    "h00100093".U(32.W), //  0: addi x1, x0, 1      (x1 = 1, LED ON)
    "h00000113".U(32.W), //  4: addi x2, x0, 0      (x2 = 0, LED OFF)  
    "h06400193".U(32.W), //  8: addi x3, x0, 100    (x3 = 100, LED Address)
    "h001002b7".U(32.W), // 12: lui x5, 256         (x5 = 0x100000 = ~1M delay)
    
    // --- LOOP START --- Address 16
    "h0011a023".U(32.W), // 16: sw x1, 0(x3)        (LED ON)
    
    // --- DELAY 1 ---
    "h00000213".U(32.W), // 20: addi x4, x0, 0      (x4 = 0)
    "h00120213".U(32.W), // 24: addi x4, x4, 1      (x4++)
    "hfe524ee3".U(32.W), // 28: blt x4, x5, -4      (if x4 < x5, goto 24)

    // --- LED OFF ---
    "h0021a023".U(32.W), // 32: sw x2, 0(x3)        (LED OFF)
    
    // --- DELAY 2 ---
    "h00000213".U(32.W), // 36: addi x4, x0, 0      (x4 = 0)
    "h00120213".U(32.W), // 40: addi x4, x4, 1      (x4++)
    "hfe524ee3".U(32.W), // 44: blt x4, x5, -4      (if x4 < x5, goto 40)

    // --- LOOP BACK (PC=48, target=16, offset=-32) ---
    "hfe0050e3".U(32.W)  // 48: bge x0, x0, -32     (goto 16)
  )


  // Program 2c: Minimal UART Test - Just sends 'A' repeatedly (no status polling)
  // This is the simplest possible UART test to verify basic functionality
  val uartTest = Seq(
    // --- SETUP ---
    "h00001337".U(32.W), //  0: lui x6, 1           (x6 = 0x1000, UART Data Address)
    "h04100393".U(32.W), //  4: addi x7, x0, 0x41   (x7 = 'A' ASCII = 65)
    
    // --- LOOP: Send 'A' forever --- Address 8
    "h00732023".U(32.W), //  8: sw x7, 0(x6)        (Write 'A' to UART)
    
    // --- SIMPLE DELAY (gives UART time to transmit) ---
    "h00000213".U(32.W), // 12: addi x4, x0, 0      (x4 = 0)
    "h10000293".U(32.W), // 16: addi x5, x0, 256    (x5 = 256, delay count)
    "h00120213".U(32.W), // 20: addi x4, x4, 1      (x4++)
    "hfe52dee3".U(32.W), // 24: bge x5, x4, -4      (Loop until x4 >= 256)
    
    // --- REPEAT ---
    "hfe0050e3".U(32.W)  // 28: bge x0, x0, -32     (Jump back to 8)
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

  // Program converted from: prime_bench.mem
  // Total instructions: 289
  val primeBench = Seq(
    // Address 0
    "h00011137".U(32.W),
    "h36c000ef".U(32.W),
    "h00000073".U(32.W),
    "h00000013".U(32.W),
    // Address 16
    "hfd010113".U(32.W),
    "h02812623".U(32.W),
    "h03010413".U(32.W),
    "hfca42e23".U(32.W),
    // Address 32
    "hfe042623".U(32.W),
    "hfe042423".U(32.W),
    "h0200006f".U(32.W),
    "hfec42703".U(32.W),
    // Address 48
    "hfdc42783".U(32.W),
    "h00f707b3".U(32.W),
    "hfef42623".U(32.W),
    "hfe842783".U(32.W),
    // Address 64
    "h00178793".U(32.W),
    "hfef42423".U(32.W),
    "hfe842703".U(32.W),
    "hfdc42783".U(32.W),
    // Address 80
    "hfcf76ee3".U(32.W),
    "hfec42783".U(32.W),
    "h00078513".U(32.W),
    "h02c12403".U(32.W),
    // Address 96
    "h03010113".U(32.W),
    "h00008067".U(32.W),
    "hfd010113".U(32.W),
    "h02812623".U(32.W),
    // Address 112
    "h03010413".U(32.W),
    "hfca42e23".U(32.W),
    "hfcb42c23".U(32.W),
    "hfd842783".U(32.W),
    // Address 128
    "h00079663".U(32.W),
    "h00000793".U(32.W),
    "h0300006f".U(32.W),
    "hfdc42783".U(32.W),
    // Address 144
    "hfef42623".U(32.W),
    "h0140006f".U(32.W),
    "hfec42703".U(32.W),
    "hfd842783".U(32.W),
    // Address 160
    "h40f707b3".U(32.W),
    "hfef42623".U(32.W),
    "hfec42703".U(32.W),
    "hfd842783".U(32.W),
    // Address 176
    "hfef774e3".U(32.W),
    "hfec42783".U(32.W),
    "h00078513".U(32.W),
    "h02c12403".U(32.W),
    // Address 192
    "h03010113".U(32.W),
    "h00008067".U(32.W),
    "hfc010113".U(32.W),
    "h02112e23".U(32.W),
    // Address 208
    "h02812c23".U(32.W),
    "h04010413".U(32.W),
    "hfca42623".U(32.W),
    "hfcb42423".U(32.W),
    // Address 224
    "h47000793".U(32.W),
    "h0007a583".U(32.W),
    "h0047a603".U(32.W),
    "h0087a683".U(32.W),
    // Address 240
    "h00c7a703".U(32.W),
    "hfcb42c23".U(32.W),
    "hfcc42e23".U(32.W),
    "hfed42023".U(32.W),
    // Address 256
    "hfee42223".U(32.W),
    "h0107c783".U(32.W),
    "hfef40423".U(32.W),
    "h00700793".U(32.W),
    // Address 272
    "hfef42623".U(32.W),
    "h0480006f".U(32.W),
    "h01000593".U(32.W),
    "hfcc42503".U(32.W),
    // Address 288
    "hf49ff0ef".U(32.W),
    "h00050693".U(32.W),
    "hfec42783".U(32.W),
    "hfc842703".U(32.W),
    // Address 304
    "h00f707b3".U(32.W),
    "hff068713".U(32.W),
    "h00870733".U(32.W),
    "hfe874703".U(32.W),
    // Address 320
    "h00e78023".U(32.W),
    "hfcc42783".U(32.W),
    "h0047d793".U(32.W),
    "hfcf42623".U(32.W),
    // Address 336
    "hfec42783".U(32.W),
    "hfff78793".U(32.W),
    "hfef42623".U(32.W),
    "hfec42783".U(32.W),
    // Address 352
    "hfa07dce3".U(32.W),
    "hfc842783".U(32.W),
    "h00878793".U(32.W),
    "h00078023".U(32.W),
    // Address 368
    "h00000013".U(32.W),
    "h03c12083".U(32.W),
    "h03812403".U(32.W),
    "h04010113".U(32.W),
    // Address 384
    "h00008067".U(32.W),
    "hfe010113".U(32.W),
    "h00812e23".U(32.W),
    "h02010413".U(32.W),
    // Address 400
    "hfea42623".U(32.W),
    "hfec42783".U(32.W),
    "h00078513".U(32.W),
    "h01c12403".U(32.W),
    // Address 416
    "h02010113".U(32.W),
    "h00008067".U(32.W),
    "hfe010113".U(32.W),
    "h00812e23".U(32.W),
    // Address 432
    "h02010413".U(32.W),
    "hfea42623".U(32.W),
    "h00058793".U(32.W),
    "hfef405a3".U(32.W),
    // Address 448
    "hfec42783".U(32.W),
    "hfeb44703".U(32.W),
    "h00e78023".U(32.W),
    "h00000013".U(32.W),
    // Address 464
    "h01c12403".U(32.W),
    "h02010113".U(32.W),
    "h00008067".U(32.W),
    "hfe010113".U(32.W),
    // Address 480
    "h00112e23".U(32.W),
    "h00812c23".U(32.W),
    "h02010413".U(32.W),
    "hfea42623".U(32.W),
    // Address 496
    "hfeb42423".U(32.W),
    "h0200006f".U(32.W),
    "hfe842783".U(32.W),
    "h00178713".U(32.W),
    // Address 512
    "hfee42423".U(32.W),
    "h0007c783".U(32.W),
    "h00078593".U(32.W),
    "hfec42503".U(32.W),
    // Address 528
    "hf99ff0ef".U(32.W),
    "hfe842783".U(32.W),
    "h0007c783".U(32.W),
    "hfc079ee3".U(32.W),
    // Address 544
    "h00000013".U(32.W),
    "h00000013".U(32.W),
    "h01c12083".U(32.W),
    "h01812403".U(32.W),
    // Address 560
    "h02010113".U(32.W),
    "h00008067".U(32.W),
    "hfd010113".U(32.W),
    "h02112623".U(32.W),
    // Address 576
    "h02812423".U(32.W),
    "h03010413".U(32.W),
    "hfca42e23".U(32.W),
    "hfcb42c23".U(32.W),
    // Address 592
    "hfe440793".U(32.W),
    "h00078593".U(32.W),
    "hfd842503".U(32.W),
    "he6dff0ef".U(32.W),
    // Address 608
    "hfe440793".U(32.W),
    "h00078593".U(32.W),
    "hfdc42503".U(32.W),
    "hf71ff0ef".U(32.W),
    // Address 624
    "h00000013".U(32.W),
    "h02c12083".U(32.W),
    "h02812403".U(32.W),
    "h03010113".U(32.W),
    // Address 640
    "h00008067".U(32.W),
    "hfe010113".U(32.W),
    "h00812e23".U(32.W),
    "h02010413".U(32.W),
    // Address 656
    "hfea42623".U(32.W),
    "hfeb42423".U(32.W),
    "h00000013".U(32.W),
    "hfec42783".U(32.W),
    // Address 672
    "h0047c783".U(32.W),
    "h0ff7f793".U(32.W),
    "h0027f793".U(32.W),
    "hfe0788e3".U(32.W),
    // Address 688
    "hfec42783".U(32.W),
    "h0007c783".U(32.W),
    "h0ff7f713".U(32.W),
    "hfe842783".U(32.W),
    // Address 704
    "h00e78023".U(32.W),
    "h00000013".U(32.W),
    "h01c12403".U(32.W),
    "h02010113".U(32.W),
    // Address 720
    "h00008067".U(32.W),
    "hfd010113".U(32.W),
    "h02112623".U(32.W),
    "h02812403".U(32.W),
    // Address 736
    "h03010413".U(32.W),
    "hfca42e23".U(32.W),
    "hfcb42c23".U(32.W),
    "hfcc42a23".U(32.W),
    // Address 752
    "hfe042623".U(32.W),
    "h0480006f".U(32.W),
    "hfeb40793".U(32.W),
    "h00078593".U(32.W),
    // Address 768
    "hfdc42503".U(32.W),
    "hf81ff0ef".U(32.W),
    "hfeb44703".U(32.W),
    "h00a00793".U(32.W),
    // Address 784
    "h02f70e63".U(32.W),
    "hfeb44703".U(32.W),
    "h00d00793".U(32.W),
    "h02f70863".U(32.W),
    // Address 800
    "hfec42783".U(32.W),
    "h00178713".U(32.W),
    "hfee42623".U(32.W),
    "hfd842703".U(32.W),
    // Address 816
    "h00f707b3".U(32.W),
    "hfeb44703".U(32.W),
    "h00e78023".U(32.W),
    "hfd442783".U(32.W),
    // Address 832
    "hfff78793".U(32.W),
    "hfec42703".U(32.W),
    "hfaf768e3".U(32.W),
    "hfd842703".U(32.W),
    // Address 848
    "hfec42783".U(32.W),
    "h00f707b3".U(32.W),
    "h00078023".U(32.W),
    "h00000013".U(32.W),
    // Address 864
    "h02c12083".U(32.W),
    "h02812403".U(32.W),
    "h03010113".U(32.W),
    "h00008067".U(32.W),
    // Address 880
    "hf6010113".U(32.W),
    "h08112e23".U(32.W),
    "h08812c23".U(32.W),
    "h0a010413".U(32.W),
    // Address 896
    "hfc042823".U(32.W),
    "h00001537".U(32.W),
    "hdfdff0ef".U(32.W),
    "hfca42c23".U(32.W),
    // Address 912
    "h0ff00593".U(32.W),
    "hfd842503".U(32.W),
    "he11ff0ef".U(32.W),
    "hfe042623".U(32.W),
    // Address 928
    "h00200793".U(32.W),
    "hfef42423".U(32.W),
    "h0980006f".U(32.W),
    "h00100793".U(32.W),
    // Address 944
    "hfef42223".U(32.W),
    "h00200793".U(32.W),
    "hfef42023".U(32.W),
    "h00400793".U(32.W),
    // Address 960
    "hfcf42e23".U(32.W),
    "h03c0006f".U(32.W),
    "hfe042583".U(32.W),
    "hfe842503".U(32.W),
    // Address 976
    "hc99ff0ef".U(32.W),
    "hfca42a23".U(32.W),
    "hfd442783".U(32.W),
    "h00079663".U(32.W),
    // Address 992
    "hfe042223".U(32.W),
    "h0280006f".U(32.W),
    "hfe042783".U(32.W),
    "h00178793".U(32.W),
    // Address 1008
    "hfef42023".U(32.W),
    "hfe042503".U(32.W),
    "hc19ff0ef".U(32.W),
    "hfca42e23".U(32.W),
    // Address 1024
    "hfdc42703".U(32.W),
    "hfe842783".U(32.W),
    "hfce7f0e3".U(32.W),
    "hfe442783".U(32.W),
    // Address 1040
    "h02078263".U(32.W),
    "hfec42783".U(32.W),
    "h00178713".U(32.W),
    "hfee42623".U(32.W),
    // Address 1056
    "h00279793".U(32.W),
    "hff078793".U(32.W),
    "h008787b3".U(32.W),
    "hfe842703".U(32.W),
    // Address 1072
    "hf6e7ae23".U(32.W),
    "hfe842783".U(32.W),
    "h00178793".U(32.W),
    "hfef42423".U(32.W),
    // Address 1088
    "hfec42703".U(32.W),
    "h01800793".U(32.W),
    "hf6e7f2e3".U(32.W),
    "h0fe00593".U(32.W),
    // Address 1104
    "hfd842503".U(32.W),
    "hd55ff0ef".U(32.W),
    "h00000793".U(32.W),
    "h00078513".U(32.W),
    // Address 1120
    "h09c12083".U(32.W),
    "h09812403".U(32.W),
    "h0a010113".U(32.W),
    "h00008067".U(32.W),
    // Address 1136 - Hex chars lookup table "0123456789ABCDEF"
    "h33323130".U(32.W),
    "h37363534".U(32.W),
    "h42413938".U(32.W),
    "h46454443".U(32.W),
    // Address 1152
    "h00000000".U(32.W)
  )
}
