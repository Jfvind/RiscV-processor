package core

import chisel3.{fromIntToLiteral, fromIntToWidth}

object CSRConstants {
  // CSR Operation Codes (baseret på funct3)
  val CSR_OP_NOP   = 0.U(3.W)  // Ikke-CSR (f.eks. ECALL)
  val CSR_OP_RRW   = 1.U(3.W)  // CSRRW
  val CSR_OP_RRS   = 2.U(3.W)  // CSRRS
  val CSR_OP_RRC   = 3.U(3.W)  // CSRRC
  val CSR_OP_RRWI  = 4.U(3.W)  // CSRRWI
  val CSR_OP_RRSI  = 5.U(3.W)  // CSRRSI
  val CSR_OP_RRCI  = 6.U(3.W)  // CSRRCI

  // Eksempel CSR-adresser (fra RISC-V spec, user-level)
  val CSR_CYCLE    = 0xC00.U(12.W)  // Read-only cycle counter
  val CSR_TIME     = 0xC01.U(12.W)  // Read-only timer
  val CSR_INSTRET  = 0xC02.U(12.W)  // Read-only instruction-retired counter
}