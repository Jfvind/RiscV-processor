package core 

import chisel3._
import chisel3.util._
//import chisel.lib.uart._

// --- UART HELPER CLASSES ---
class Channel extends Bundle {
  val bits = Input(Bits(8.W))
  val ready = Output(Bool())
  val valid = Input(Bool())
}

class Buffer extends Module {
  val io = IO(new Bundle {
    val in = new Channel()
    val out = Flipped(new Channel())
  })

  val empty :: full :: Nil = Enum(2)
  val stateReg = RegInit(empty)
  val dataReg = RegInit(0.U(8.W))

  io.in.ready := stateReg === empty
  io.out.valid := stateReg === full

  when (stateReg === empty) {
    when (io.in.valid) {
      dataReg := io.in.bits
      stateReg := full
    }
  } .otherwise {
    when (io.out.ready) {
      stateReg := empty
    }
  }
  io.out.bits := dataReg
}

class Tx(sysclk: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(Bits(1.W))
    val channel = new Channel()
  })
  // Calculate bit time counter limit based on system clock and baud rate
  val BIT_CNT = ((sysclk + baudRate/2)/baudRate - 1).asUInt
  
  val shiftReg = RegInit(0x7ff.U)
  val cntReg = RegInit(0.U(20.W))
  val bitsReg = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd := shiftReg(0)

  when (cntReg === 0.U) {
    cntReg := BIT_CNT
    when (bitsReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(9,0))
      bitsReg := bitsReg - 1.U
    } .otherwise {
      when (io.channel.valid) {
        // Start bit (0) + Data + Stop bits (11)
        shiftReg := Cat(Cat(3.U, io.channel.bits), 0.U)
        bitsReg := 11.U
      } .otherwise {
        shiftReg := 0x7ff.U
      }
    }
  } .otherwise {
    cntReg := cntReg - 1.U
  }
}

class BufferedTx(sysclk: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(Bits(1.W))
    val channel = new Channel()
  })
  val tx = Module(new Tx(sysclk, baudRate))
  val buf = Module(new Buffer())

  buf.io.in <> io.channel
  tx.io.channel <> buf.io.out
  io.txd <> tx.io.txd
}
// ---------------------------

class Serialport extends Module {
  val io = IO(new Bundle {
    val inputString = Input(Vec(32, UInt(8.W)))
    val sendTrigger = Input(Bool())
    val tx = Output(Bool())
  })

  val CNT_MAX = (100000000 / 115200 - 1).U // Adjusted for 115200 baud rate
  val uart = Module(new BufferedTx(100000000, 115200))
  val cntReg = RegInit(0.U(32.W))
  val strCntReg = RegInit(0.U(5.W))
  val sending = RegInit(false.B)

  uart.io.channel.valid := false.B
  uart.io.channel.bits := 0.U

  when(sending) {
    when(cntReg === CNT_MAX) {
      cntReg := 0.U
      when(uart.io.channel.ready) {
        uart.io.channel.valid := true.B
        uart.io.channel.bits := io.inputString(strCntReg)
        strCntReg := strCntReg + 1.U
      }
      when(strCntReg === 31.U) {
        sending := false.B
        strCntReg := 0.U
      }
    } .otherwise {
      cntReg := cntReg + 1.U
    }
  } .elsewhen(io.sendTrigger) {
    sending := true.B
  }

  io.tx := uart.io.txd
}