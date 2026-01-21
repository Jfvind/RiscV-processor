#!/usr/bin/env python3
"""
Converts a .mem file (hex values, one per line) into a Scala Seq[UInt] 
format compatible with Programs.scala
"""

import sys
import os

def convert_mem_to_scala(mem_file_path: str, output_path: str = None, program_name: str = "primeBench"):
    """
    Reads a .mem file and generates Scala code with a Seq of UInt values.
    
    Args:
        mem_file_path: Path to the input .mem file
        output_path: Path for output Scala file. If None, prints to stdout.
        program_name: Name of the val in the generated Scala code
    """
    # Read and parse the .mem file
    instructions = []
    with open(mem_file_path, 'r') as f:
        for line_num, line in enumerate(f, 1):
            line = line.strip()
            if not line:  # Skip empty lines
                continue
            # Validate hex format
            try:
                int(line, 16)
                instructions.append(line.lower())
            except ValueError:
                print(f"Warning: Line {line_num} is not valid hex: '{line}'", file=sys.stderr)
                continue
    
    if not instructions:
        print("Error: No instructions found in .mem file", file=sys.stderr)
        return
    
    # Generate Scala code
    lines = []
    lines.append(f"  // Program converted from: {os.path.basename(mem_file_path)}")
    lines.append(f"  // Total instructions: {len(instructions)}")
    lines.append(f"  val {program_name} = Seq(")
    
    for i, instr in enumerate(instructions):
        # Add address comment every 4 instructions for readability
        if i % 4 == 0:
            lines.append(f"    // Address {i * 4}")
        
        comma = "," if i < len(instructions) - 1 else ""
        lines.append(f'    "h{instr}".U(32.W){comma}')
    
    lines.append("  )")
    
    scala_code = "\n".join(lines)
    
    if output_path:
        with open(output_path, 'w') as f:
            f.write(scala_code)
        print(f"Wrote {len(instructions)} instructions to {output_path}")
    else:
        print(scala_code)
    
    return scala_code

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python mem_to_scala.py <input.mem> [output.scala] [program_name]")
        print("Example: python mem_to_scala.py prime_bench.mem prime_program.scala primeBench")
        sys.exit(1)
    
    mem_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    program_name = sys.argv[3] if len(sys.argv) > 3 else "primeBench"
    
    convert_mem_to_scala(mem_file, output_file, program_name)
