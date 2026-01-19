# RiscV-processor
RiscV processor on FPGA following DTU course 02114
# Block diagram
![Block diagram](block_diagrams/2/RISC-V_Stage_2.drawio.svg)
# Good links
For Risc V arch:
https://luplab.gitlab.io/rvcodecjs/#q=mul&abi=false&isa=RV128I
For Chisel commands:
https://www.chisel-lang.org/docs/explanations/
## Project Structure
```
RiscV-Processor/
├── benchmarks/                # Benchmark program (prime numbers)
├── block_diagrams/            # Block diagram files, latest in README page
├── project/
│   └── build.properties       # Specifies the sbt version
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── core/          # The actual processor logic
│   │   │   │   ├── ALU.scala
│   │   │   │   ├── ALUConstants.scala
|   |   |   |   ├── ALUDecoder.scala
|   |   |   |   ├── ControlConstants.scala
│   │   │   │   ├── ControlUnit.scala
│   │   │   │   ├── Core.scala # Top-level module connecting components
│   │   │   │   ├── DataMemory.scala
│   │   │   │   ├── ForwardingUnit.scala
│   │   │   │   ├── HazardUnit.scala
│   │   │   │   ├── ImmGen.scala
│   │   │   │   ├── InstructionFetch.scala
│   │   │   │   ├── MemoryMapping.scala
│   │   │   │   ├── Programs.scala
│   │   │   │   ├── RegisterFile.scala
│   │   │   │   ├── RegisterFile.scala
│   │   │   │   └── Serialport.scala
│   │   │   └── Top.scala      # The entry point to generate Verilog
│   │   └── resources/         # Hex files for instruction memory initialization
│   └── test/
│       └── scala/             # Unit tests for your modules
|           ├── ALUDecoderTest.scala
│           ├── ALUTest.scala
|           ├── AUIPCTest.scala
|           ├── ControlUnitTest.scala
|           ├── CoreStressTest.scala
│           ├── CoreTest.scala
|           ├── HazardTest.scala
|           ├── JALRTest.scala
|           ├── JALTest.scala
|           ├── LoadTest.scala
|           ├── LoadUseStallTest.scala
|           ├── PipelineIntegrationTest.scala
|           ├── RegisterTest.scala
|           └── UARTTest.scala
|
├── generated/                 # Output folder for the SystemVerilog file
├── build.sbt                  # The build configuration file
├── .gitignore
└── README.md
´´´
## Building and Testing

### Requirements
- SBT (Scala Build Tool) 1.9.7 or higher
- Java 8 or higher

### Commands

```bash
# Run tests
sbt test

# Compile processor to .sv
sbt run
```

# Signals in pipeline
### IF/ID register:
- instruction: 32 bits
- PC: 32 bits

### ID/EX register:
- pc: 32 bits
- rs1_data: 32 bits
- rs2_data: 32 bits
- imm: 32 bits
- rs1_addr: 5 bits (Forwarding)
- rs2_addr: 5 bits (Forwarding)
- rd_addr: 5 bits
- alu_op: 4 bits (R-type: add, sub, and, or, xor, sll, srl, sra osv)
- tx: Bool
- regWrite: Bool
- memWrite: Bool
- branch: Bool
- aluSrc: Bool
- rs1_address 5 bit (Forwarding)
- rs2_address 5 bit(Forwarding) 
- MemToReg 1 bit


    val aluOp    = UInt(4.W)
    val funct3   = UInt(3.W)
    val funct7   = UInt(7.W)
    val memToReg = Bool()
    val jump     = Bool()
    val jumpReg  = Bool()
    val auipc    = Bool()
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
