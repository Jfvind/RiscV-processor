import struct

def write_line(f, val):
    f.write(f"{val:08x}\n")

# Encoders
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
    # B-Type Scrambling
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

# Opcodes
LUI = 0x37; ADDI = 0x13; SW = 0x23; BEQ = 0x63; JAL = 0x6F
x0=0; x1=1; x2=2; x3=3; x8=8; x9=9

prog = []

# 1. SETUP: x1 = 0x1000 (UART Base)
prog.append(encode_u_type(LUI, x1, 1)) 

def print_safe(char):
    # Load Char
    prog.append(encode_i_type(ADDI, 0, x2, x0, ord(char)))
    # Store to UART
    prog.append(encode_s_type(SW, 2, x1, x2, 0))
    
    # DELAY LOOP SETUP
    # Use LUI to load 4096 (0x1000) into x3. Safe 32-bit instruction.
    prog.append(encode_u_type(LUI, x3, 1)) 
    
    # --- LOOP START ---
    # Decrement x3 (x3 = x3 - 1)
    prog.append(encode_i_type(ADDI, 0, x3, x3, -1))
    
    # NOPs (Safety)
    prog.append(encode_i_type(ADDI, 0, x0, x0, 0))
    prog.append(encode_i_type(ADDI, 0, x0, x0, 0))
    
    # Branch if x3 != 0 (Back to Decrement)
    # Current instr is at Offset 0. 
    # We need to jump back 3 instructions (BNE itself + 2 NOPs + ADDI).
    # 3 instructions * 4 bytes = 12 bytes. Offset = -12.
    prog.append(encode_b_type(BEQ, 1, x3, x0, -12))
    # --- LOOP END ---

# 2. Print Sequence
print_safe('A')
print_safe('B')
print_safe('C')
print_safe('\r')
print_safe('\n')

# 3. LED ON
prog.append(encode_i_type(ADDI, 0, x8, x0, 100))
prog.append(encode_i_type(ADDI, 0, x9, x0, 1))
prog.append(encode_s_type(SW, 2, x8, x9, 0))

# 4. Infinite Loop
prog.append(encode_j_type(JAL, x0, 0))

# Write File
with open("prime_bench.mem", "w") as f:
    for instr in prog:
        write_line(f, instr)

print(f"Generated prime_bench.mem with {len(prog)} instructions.")