// Handles the Program Counter (PC) and fetches instructions from memory.
package core

import chisel3._
/*
Signals:
PC register
PC+4 adder
Instruction memory interface
MUX til at vælge næste PC (PC+4 vs branch target )

 */
