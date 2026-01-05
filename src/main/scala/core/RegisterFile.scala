// Contains the 32 general-purpose registers (x0-x31). It handles reading two registers and writing to one.
import chisel3._
// Contains the 32 general-purpose registers (x0-x31). It handles reading two registers and writing to one.
(RegInit(VecInit(Seq.fill(32)(0.U(32.W)))))