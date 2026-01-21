package core

import chisel3._
import chisel3.util._

class BranchPredictor extends Module {
  val io = IO(new Bundle {
    // Fetch Stage (Prediction)
    val fetch_pc         = Input(UInt(32.W))
    val predict_taken    = Output(Bool())
    val predicted_target = Output(UInt(32.W))

    // Execute Stage (Update)
    val update_valid     = Input(Bool())
    val update_pc        = Input(UInt(32.W))
    val update_taken     = Input(Bool())
    val update_target    = Input(UInt(32.W))
    val is_branch        = Input(Bool())
  })

  // --- Branch Target Buffer (BTB) ---
  // Stores: PC -> Target Address
  // Size: 16 entries (PC bits [5:2] as index)
  val btb = Mem(16, UInt(32.W))
  val btb_tags = Mem(16, UInt(28.W))  // Store upper PC bits for tag matching
  val btb_valid = RegInit(VecInit(Seq.fill(16)(false.B)))

  // --- Pattern History Table (PHT) ---
  // 2-bit saturating counters: 00=Strong NT, 01=Weak NT, 10=Weak T, 11=Strong T
  val pht = Mem(16, UInt(2.W))

  // Initialize PHT to "Weakly Not Taken" (01)
  for (i <- 0 until 16) {
    pht.write(i.U, 1.U)
  }

  // === PREDICTION (Fetch Stage) ===
  val fetch_index = io.fetch_pc(5, 2)  // 4-bit index
  val fetch_tag = io.fetch_pc(31, 4)   // 28-bit tag

  val btb_hit = btb_valid(fetch_index) && (btb_tags(fetch_index) === fetch_tag)
  val counter = pht.read(fetch_index)

  // Predict taken if counter >= 2 (Weakly/Strongly Taken)
  io.predict_taken := btb_hit && counter(1)
  io.predicted_target := Mux(btb_hit, btb(fetch_index), io.fetch_pc + 4.U)

  // === UPDATE (Execute Stage) ===
  when(io.update_valid && io.is_branch) {
    val update_index = io.update_pc(5, 2)
    val update_tag = io.update_pc(31, 4)

    // Update BTB
    when(io.update_taken) {
      btb(update_index) := io.update_target
      btb_tags(update_index) := update_tag
      btb_valid(update_index) := true.B
    }

    // Update PHT (2-bit saturating counter)
    val old_counter = pht.read(update_index)
    val new_counter = WireDefault(old_counter)

    when(io.update_taken) {
      // Increment (saturate at 3)
      new_counter := Mux(old_counter === 3.U, 3.U, old_counter + 1.U)
    } .otherwise {
      // Decrement (saturate at 0)
      new_counter := Mux(old_counter === 0.U, 0.U, old_counter - 1.U)
    }

    pht.write(update_index, new_counter)
  }
}