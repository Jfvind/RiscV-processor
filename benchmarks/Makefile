# Makefile for the RISC-V benchmarks

# Default configuration
UART_ADDR ?= 0x00001000
NUM_PRIMES ?= 25

CSR_EN ?= N
DEBUG_EN ?= N

# Toolchain
# Shamelessly stealing from Schoeberl to support mac
UNAME := $(shell uname)
ifeq ($(UNAME),Darwin)
# assuming tools are installed with Mac Homebrew
export CROSS=riscv64-elf-
else
export CROSS=riscv64-unknown-elf-
endif
#end of shameless stealing

CC = $(CROSS)gcc
OBJCOPY = $(CROSS)objcopy

# Compiler flags for RV32I bare metal
CFLAGS = -mabi=ilp32 -O0 
CFLAGS += -ffreestanding -nostdlib -e entry -T linker.ld

# Pass configuration to C code
DEFINES = -DUART_ADDR=$(UART_ADDR) -DNUM_PRIMES=$(NUM_PRIMES)
ifeq ($(CSR_EN), Y)
    DEFINES += -DCSR_EN
	CFLAGS += -march=rv32ia_zicsr
else
    #DEFINES += -DCSR_EN=0
	CFLAGS += -march=rv32i
endif
ifeq ($(DEBUG_EN), Y)
	DEFINES += -DDEBUG_EN
endif

TARGET = prime
SRC = prime.c

.PHONY: all clean test-hw

all: $(TARGET).bin

$(TARGET).elf: $(SRC)
	$(CC) $(CFLAGS) $(DEFINES) $< -o $@

$(TARGET).bin: $(TARGET).elf
	$(OBJCOPY) -O binary $< $@

test-hw: $(TARGET).elf
	python3 benchy.py --addr $(UART_ADDR) --csr $(CSR_EN)

clean:
	rm -f *.elf *.o *.bin