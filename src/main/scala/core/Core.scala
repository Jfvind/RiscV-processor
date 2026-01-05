// This is the "motherboard." It instantiates the ALU, Register File, and Control Unit and wires them together.

package core
import chisel3._

/*
instantierer:

IF, ControlUnit, RegFile, ALU moduler
Pipeline registers (IF/ID, ID/EX, EX/MEM, MEM/WB)
Forwarding unit (den logic vi lige skrev)
Hazard detection unit (til load-use hazards senere)
Alle MUXes mellem stages
 */

class Core extends Module {
    // bla bla
}

