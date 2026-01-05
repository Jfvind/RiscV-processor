// Decodes the instruction (opcode, funct3, funct7) and generates control signals (e.g., aluOp, memWrite, regWrite).
import chisel3._

/*
Input: instruction[31:0] (fra IF/ID register)
Output: Alle control signals (RegWrite, ALUOp, ALUSrc, MemRead, MemWrite, MemToReg, etc.)
Kombinatorisk logic der decoder opcode, funct3, funct7
 */