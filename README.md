# RiscV-processor
RiscV processor on FPGA following DTU course 02114

## Project Structure

```
.
├── build.sbt                    # SBT build configuration with Chisel dependencies
├── project/
│   └── build.properties         # SBT version specification
├── src/
│   ├── main/
│   │   └── scala/              # Main Chisel source code
│   └── test/
│       └── scala/              # ChiselTest test files
└── Makefile                     # Build automation (test, compile, clean)
```

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
