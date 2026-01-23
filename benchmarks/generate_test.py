import struct

def write_line(f, val):
    f.write(f"{val:08x}\n")

# --- ENCODERS ---
def encode_i_type(opcode, funct3, rd, rs1, imm):
    if imm < 0: imm = (1 << 12) + imm
    val = (imm << 20) | (rs1 << 15) | (funct3 << 12) | (rd << 7) | opcode
    return val

def encode_s_type(opcode, funct3, rs1, rs2, imm):
    if imm < 0: imm = (1 << 12) + imm
    val = ((imm >> 5) << 25) | (rs2 << 20) | (rs1 << 15) | (funct3 << 12) | ((imm & 0x1F) << 7) | opcode
    return val

def encode_b_type(opcode, funct3, rs1, rs2, imm):
    if imm < 0: imm = (1 << 13) + imm
    bit_12   = (imm >> 12) & 1
    bit_11   = (imm >> 11) & 1
    bit_10_5 = (imm >> 5)  & 0x3F
    bit_4_1  = (imm >> 1)  & 0xF
    val = (bit_12 << 31) | (bit_10_5 << 25) | (rs2 << 20) | (rs1 << 15) | (funct3 << 12) | (bit_4_1 << 8) | (bit_11 << 7) | opcode
    return val

def encode_u_type(opcode, rd, imm):
    val = (imm << 12) | (rd << 7) | opcode
    return val

def encode_j_type(opcode, rd, imm):
    if imm < 0: imm = (1 << 21) + imm
    bit_20    = (imm >> 20) & 1
    bit_10_1  = (imm >> 1)  & 0x3FF
    bit_11    = (imm >> 11) & 1
    bit_19_12 = (imm >> 12) & 0xFF
    val = (bit_20 << 31) | (bit_10_1 << 21) | (bit_11 << 20) | (bit_19_12 << 12) | (rd << 7) | opcode
    return val

# --- OPCODES ---
LUI = 0x37; ADDI = 0x13; SW = 0x23; LW = 0x03; BEQ = 0x63; JAL = 0x6F
x0=0; x1=1; x2=2; x3=3; x4=4; x5=5; x8=8; x9=9

prog = []

# 1. SETUP
# x1 = 0x1000 (UART Base Address)
prog.append(encode_u_type(LUI, x1, 1)) 

# x4 = 'A' (Initial Char 65)
prog.append(encode_i_type(ADDI, 0, x4, x0, 65))

# x5 = 'Z' + 1 (Limit 91)
prog.append(encode_i_type(ADDI, 0, x5, x0, 91))

# --- MAIN LOOP START (Label: LOOP_CHAR) ---
# We are currently at instruction index 3.

# --- POLLING SUB-LOOP START (Label: WAIT_TX) ---
# 2. Read UART Status (Address 0x1004 = x1 + 4)
# LW x2, 4(x1)
prog.append(encode_i_type(LW, 2, x2, x1, 4))

# 3. Check Ready Bit (Bit 0)
# ANDI x2, x2, 1
prog.append(encode_i_type(ADDI, 7, x2, x2, 1)) # funct3=7 is ANDI

# 4. Branch if NOT Ready (x2 == 0) -> Jump back to LW
prog.append(encode_b_type(BEQ, 0, x2, x0, -8))
# --- POLLING SUB-LOOP END ---

# 5. Print Character
# SW x4, 0(x1)
prog.append(encode_s_type(SW, 2, x1, x4, 0))

# 6. Increment Character
# ADDI x4, x4, 1
prog.append(encode_i_type(ADDI, 0, x4, x4, 1))

# 7. Loop Check (If x4 != x5, jump back to POLLING Start)
# Jump Offset = -20 bytes.
# Use BNE (BEQ opcode + funct3=1)
prog.append(encode_b_type(BEQ, 1, x4, x5, -20))
# --- MAIN LOOP END ---

# 8. LED ON (Success Indication)
prog.append(encode_i_type(ADDI, 0, x8, x0, 100))
prog.append(encode_i_type(ADDI, 0, x9, x0, 1))
prog.append(encode_s_type(SW, 2, x8, x9, 0))

# 9. Infinite Loop
prog.append(encode_j_type(JAL, x0, 0))

# Write File
with open("prime_bench.mem", "w") as f:
    for instr in prog:
        write_line(f, instr)

print(f"Generated prime_bench.mem with {len(prog)} instructions.")