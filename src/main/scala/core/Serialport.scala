package core 

import chisel3._
import chisel3.util._

/*
 * From Digital Electronics 2
 * Author: Martin Schoeberl
 * Repo: ip-contribution/src/main/scala/chisel/lib/uart
*/

class UartIO extends DecoupledIO(UInt(8.W))

/**
  * Transmit part of the UART.
  * A minimal version without any additional buffering.
  * Use a ready/valid handshaking.
  */
class Tx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).asUInt

  val shiftReg = RegInit(0x7ff.U)
  val cntReg = RegInit(0.U(20.W))
  val bitsReg = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd := shiftReg(0)

  when(cntReg === 0.U) {

    cntReg := BIT_CNT
    when(bitsReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(9, 0))
      bitsReg := bitsReg - 1.U
    }.otherwise {
      when(io.channel.valid) {
        shiftReg := Cat(Cat(3.U, io.channel.bits), 0.U) // two stop bits, data, one start bit
        bitsReg := 11.U
      }.otherwise {
        shiftReg := 0x7ff.U
      }
    }

  }.otherwise {
    cntReg := cntReg - 1.U
  }
}

/**
  * Receive part of the UART.
  * A minimal version without any additional buffering.
  * Use a ready/valid handshaking.
  *
  * The following code is inspired by Tommy's receive code at:
  * https://github.com/tommythorn/yarvi
  */
class Rx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val channel = new UartIO()
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1)
  val START_CNT = ((3 * frequency / 2 + baudRate / 2) / baudRate - 2) // -2 for the falling delay

  // Sync in the asynchronous RX data
  val rxReg = RegNext(RegNext(io.rxd, 0.U), 0.U)
  val falling = !rxReg && (RegNext(rxReg) === 1.U)

  val shiftReg = RegInit(0.U(8.W))
  val cntReg = RegInit(BIT_CNT.U(20.W)) // have some idle time before listening
  val bitsReg = RegInit(0.U(4.W))
  val valReg = RegInit(false.B)

  when(cntReg =/= 0.U) {
    cntReg := cntReg - 1.U
  }.elsewhen(bitsReg =/= 0.U) {
    cntReg := BIT_CNT.U
    shiftReg := Cat(rxReg, shiftReg >> 1)
    bitsReg := bitsReg - 1.U
    // the last shifted in
    when(bitsReg === 1.U) {
      valReg := true.B
    }
  }.elsewhen(falling) { // wait 1.5 bits after falling edge of start
    cntReg := START_CNT.U
    bitsReg := 8.U
  }

  when(valReg && io.channel.ready) {
    valReg := false.B
  }

  io.channel.bits := shiftReg
  io.channel.valid := valReg
}

/**
  * A single byte buffer with a ready/valid interface
  */
class Buffer extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new UartIO())
    val out = new UartIO()
  })

  val empty :: full :: Nil = Enum(2)
  val stateReg = RegInit(empty)
  val dataReg = RegInit(0.U(8.W))

  io.in.ready := stateReg === empty
  io.out.valid := stateReg === full

  when(stateReg === empty) {
    when(io.in.valid) {
      dataReg := io.in.bits
      stateReg := full
    }
  }.otherwise { // full
    when(io.out.ready) {
      stateReg := empty
    }
  }
  io.out.bits := dataReg
}

/**
  * A transmitter with a single buffer.
  */
class BufferedTx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })
  val tx = Module(new Tx(frequency, baudRate))
  val buf = Module(new Buffer())

  buf.io.in <> io.channel
  tx.io.channel <> buf.io.out
  io.txd <> tx.io.txd
}

// ====================================================================================

class Serialport extends Module {
  val io = IO(new Bundle {
    val inputString = Input(Vec(32, UInt(8.W)))
    val sendTrigger = Input(Bool())
    val tx = Output(Bool())
  })

  val uart = Module(new BufferedTx(100000000, 115200))
  val strCntReg = RegInit(0.U(5.W))
  val sending = RegInit(false.B)

  uart.io.channel.valid := false.B
  uart.io.channel.bits := 0.U

  when(sending) {
    when(uart.io.channel.ready) {
      uart.io.channel.valid := true.B
      uart.io.channel.bits := io.inputString(strCntReg)
      strCntReg := strCntReg + 1.U

      when(strCntReg === 31.U) {
      sending := false.B
      strCntReg := 0.U
      }
    }
  } .elsewhen(io.sendTrigger) {
    sending := true.B
  }

  io.tx := uart.io.txd
}