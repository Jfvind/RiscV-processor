# RiscV-processor
RiscV processor on FPGA following DTU course 02114
# Good links
For Risc V arch:
https://luplab.gitlab.io/rvcodecjs/#q=mul&abi=false&isa=RV128I
For Chisel commands:
https://www.chisel-lang.org/docs/explanations/
## Project Structure
```
RiscV-Processor/
├── project/
│   └── build.properties       # Specifies the sbt version
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── core/          # The actual processor logic
│   │   │   │   ├── ALU.scala
│   │   │   │   ├── ALUConstanss.scala
│   │   │   │   ├── ControlUnit.scala
│   │   │   │   ├── RegisterFile.scala
│   │   │   │   ├── InstructionFetch.scala
│   │   │   │   └── Core.scala # Top-level module connecting components
│   │   │   └── Top.scala      # The entry point to generate Verilog
│   │   └── resources/         # Hex files for instruction memory initialization
│   └── test/
│       └── scala/             # Unit tests for your modules
│           ├── ALUTest.scala
│           └── CoreTest.scala
|           └── RegisterTest.scala
|
├── generated/                 # Output folder for the Verilog file
├── build.sbt                  # The build configuration file
├── Makefile
├── .gitignore
└── README.md
## Building and Testing

### Requirements
- SBT (Scala Build Tool) 1.9.7 or higher
- Java 8 or higher

### Commands

```bash
# Run tests
make test
# or
sbt test

# Compile the project
make compile
# or
sbt compile

# Clean build artifacts
make clean
# or
sbt clean
```

# Signals in pipeline
### IF/ID register:
- instruction: 32 bits
- PC: 32 bits
- ???

### ID/EX register:
- rs1_data: 32 bits
- rs2_data: 32 bits
- immediate: 32 bits
- rd: 5 bits
- ALUOp: 4 bits (R-type: add, sub, and, or, xor, sll, srl, sra osv)
- ALUSrc: 1 bit
- rs1_address 5 bit (Forwarding)
- rs2_address 5 bit(Forwarding) 
- RegWrite signal 1 bit
- PC
- MemToReg 1 bit
### EX/MEM 
- ALU_Result
- rd: 5 bits
- RegWrite 1 bit
- MemRead 1 bit
- MemWrite 1 bit
- rs2_data 32 bit (ex. sw rs2, offset(rs1))
- MemToReg 1 bit

### MEM/WB)
- ALU_Result: 32 bit
- Memory_data: 32 bits 
- rd 5 bits (destination)
- RegWrite 1 bit
- MemToReg 1 bit



## Dependencies

- Chisel 5.1.0 - Hardware construction language
- ChiselTest 5.0.2 - Testing framework for Chisel designs
