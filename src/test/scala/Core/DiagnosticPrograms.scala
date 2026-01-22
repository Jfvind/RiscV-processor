package core

import chisel3._

object DiagnosticPrograms {

  val padding = Seq(
    "h00000013".U(32.W),
    "h00000013".U(32.W),
    "h00000013".U(32.W)
  )

  // TEST 1: CSR Forwarding
  val csrForwarding = Seq(
    "hc0002573".U(32.W),
    "h00a50593".U(32.W),
    "h00000013".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 2: CSR Double Read
  val csrDoubleRead = Seq(
    "hc0002573".U(32.W),
    "h00000013".U(32.W),
    "h00000013".U(32.W),
    "hc0002673".U(32.W),
    "h40c50733".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 3: JAL Forwarding (Robust)
  val jalForwarding = Seq(
    "h010000ef".U(32.W), // 0: jal ra, 16 -> Target=16
    "h00100513".U(32.W), // 4: skipped
    "h00200593".U(32.W), // 8: skipped
    "h00300613".U(32.W), // 12: skipped
    "h00008513".U(32.W), // 16: addi x10, ra, 0
    "h00000073".U(32.W)  // 20: ecall
  ) ++ padding

  // TEST 4: AUIPC Forwarding
  val auipcForwarding = Seq(
    "h00001517".U(32.W),
    "h00a50593".U(32.W),
    "h00000013".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 5: CSR Load-Use Hazard
  val csrLoadUse = Seq(
    "h06400513".U(32.W),
    "h00a02a23".U(32.W),
    "h01402583".U(32.W),
    "hc0002673".U(32.W),
    "h00b60733".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 6: UART Status (POLLING)
  val uartStatus = Seq(
    "h40100513".U(32.W), // 0: addi x10, x0, 1025
    "h00251513".U(32.W), // 4: slli x10, x10, 2 (0x1004)
    // Loop
    "h00052583".U(32.W), // 8: lw x11, 0(x10)
    "h0015f593".U(32.W), // 12: andi x11, x11, 1
    "hfe058ce3".U(32.W), // 16: beq x11, x0, -8 (Check x11!=0)
    "h00058513".U(32.W), // 20: addi x10, x11, 0
    "h00000073".U(32.W)  // 24: ecall
  ) ++ padding

  // TEST 7: UART Write
  val uartWrite = Seq(
    "h00001337".U(32.W),
    "h04100393".U(32.W),
    "h00732023".U(32.W),
    "h04200393".U(32.W),
    "h00732023".U(32.W),
    "h04300393".U(32.W),
    "h00732023".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 11: Nested Forwarding
  val nestedForwarding = Seq(
    "h00a00093".U(32.W),
    "h00508113".U(32.W),
    "h00310193".U(32.W),
    "h00418213".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 12: CSR Write-Read-Write
  val csrWriteReadWrite = Seq(
    "h12345537".U(32.W),
    "h34052573".U(32.W),
    "h00a50593".U(32.W),
    "h34002673".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 13: Integration
  val integration = Seq(
    "hc0002573".U(32.W),
    "h00001337".U(32.W),
    "h00000593".U(32.W),
    "h00a00613".U(32.W),
    "h00158593".U(32.W),
    "hfec64ee3".U(32.W),
    "hc0002673".U(32.W),
    "h40a60733".U(32.W),
    "h00e32023".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding

  // TEST 14: SyncReadMem Stall
  val syncReadMemStall = Seq(
    "h06400093".U(32.W),
    "h00102a23".U(32.W),
    "h01402103".U(32.W),
    "h002081b3".U(32.W),
    "h00318213".U(32.W),
    "h00000073".U(32.W)
  ) ++ padding
}