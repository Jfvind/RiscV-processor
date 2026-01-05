# RiscV-processor
RiscV processor on FPGA following DTU course 02114
# Good links
https://luplab.gitlab.io/rvcodecjs/#q=mul&abi=false&isa=RV128I
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

## Dependencies

- Chisel 5.1.0 - Hardware construction language
- ChiselTest 5.0.2 - Testing framework for Chisel designs
