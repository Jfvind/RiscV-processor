import sys
import struct

def main():
    if len(sys.argv) != 3:
        print("Usage: python3 make_hex.py <input.bin> <output.mem>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    try:
        with open(input_path, 'rb') as f:
            data = f.read()

        # Pad with zeros to ensure multiple of 4 bytes
        while len(data) % 4 != 0:
            data += b'\x00'

        with open(output_path, 'w') as f:
            # Read 4 bytes at a time
            for i in range(0, len(data), 4):
                chunk = data[i:i+4]
                # Unpack as Little Endian unsigned int (Standard RISC-V)
                val = struct.unpack('<I', chunk)[0]
                # Write as 8-character Hex string (Big Endian text format)
                f.write(f"{val:08x}\n")
                
        print(f"Successfully converted {len(data)} bytes to {output_path}")

    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()