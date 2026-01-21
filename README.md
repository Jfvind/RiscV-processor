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
│   │   │   │   ├── CSRConstants.scala
│   │   │   │   ├── CSRModule.scala
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
│           ├── ALUTest.scala
|           ├── AUIPCTest.scala
|           ├── ControlUnitTest.scala
|           ├── CoreStressTest.scala
│           ├── CoreTest.scala
|           ├── CSRTest.scala
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
```
## Building and Testing

### Requirements
- SBT (Scala Build Tool) 1.9.7 or higher
- Java 8 or higher

## Build process
```bash
# Create .sv file in generated/
sbt run 
```
- Import as design source in Vivado.
- Import .mem file as design source in Vivado
```TCL console
set_property verilog_define {ENABLE_INITIAL_MEM_=1} [current_fileset]
```
- Run write bitstream in Vivado

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
- PC:          32 bits

### ID/EX register:
- pc:          32 bits
- rs1_data:    32 bits
- rs2_data:    32 bits
- imm:         32 bits
- rs1_addr:     5 bits (Forwarding)
- rs2_addr:     5 bits (Forwarding)
- rd_addr:      5 bits
- alu_op:       4 bits 
- tx:           Bool
- regWrite:     Bool
- memWrite:     Bool
- branch:       Bool
- aluSrc:       Bool
- funct3:       3 bits
- funct7:       7 bits
- memToReg:     Bool
- jump:         Bool
- jumpReg:      Bool
- auipc:        Bool
- halt:         Bool

### EX/MEM 
- alu_Result:  32 bits
- rs2_data:    32 bits
- rd_addr:      5 bits
- regWrite:     Bool
- memWrite:     Bool
- tx:           Bool
- memToReg:     Bool
- pc_plus_4:   32 bits
- jump:         Bool
- jumpReg:      Bool
- pc:          32 bits
- imm:         32 bits
- auipc:        Bool

### MEM/WB
- result:      32 bit 
- rd_addr:      5 bits
- regWrite:     Bool

## Dependencies
- Chisel 6.2.0 - Hardware description language
- ChiselTest 6.0.0 - Testing framework for Chisel designs