package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class RegisterFileTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "RegisterFile"

  it should "write and read correctly" in {
    test(new RegisterFile) { dut =>
      dut.io.reg_write.poke(true.B)
      dut.io.rd_addr.poke(5.U)
      dut.io.rd_data.poke(42.U)
      dut.clock.step()

      dut.io.rs1_addr.poke(5.U)
      dut.clock.step()
      dut.io.rs1_data.expect(42.U)
    }
  }

  it should "keep x0 at zero" in {
    test(new RegisterFile) { dut =>
      dut.io.reg_write.poke(true.B)
      dut.io.rd_addr.poke(0.U)
      dut.io.rd_data.poke(999.U)
      dut.clock.step()

      dut.io.rs1_addr.poke(0.U)
      dut.io.rs1_data.expect(0.U)
    }
  }
}