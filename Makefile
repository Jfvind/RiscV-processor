.PHONY: test clean compile

# Default target
all: test

# Run tests
test:
	sbt test

# Compile the project
compile:
	sbt compile

# Clean build artifacts
clean:
	sbt clean

# Generates Verilog
run:
	sbt run
