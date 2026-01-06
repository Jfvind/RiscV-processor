// The Arithmetic Logic Unit. It takes two operands and an operation code (add, sub, xor, etc.) and outputs the result.
package core

import chisel3._

/*
Input A, Input B (efter forwarding MUXes!)
ALUOp control signal
Output: result, zero flag (til branches senere)
 */