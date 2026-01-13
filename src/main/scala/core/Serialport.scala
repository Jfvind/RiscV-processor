import chisel3._
import chisel3.util._
import chisel.lib.uart._

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