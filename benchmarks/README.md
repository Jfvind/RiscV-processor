# Prime Benchmark

This benchmark calculates the first N prime numbers (default 25) to test the RISC-V processor. It measures execution time using either a host-side Python script or the processor's cycle counter (CSR).

## Prerequisites

- **RISC-V Toolchain**: `riscv64-unknown-elf-gcc`, `objcopy` (or `riscv64-elf-` on macOS)
- **Python 3**: With `pyserial` installed (`pip install pyserial`)
- **Make**
- **Hardware**: A RISC-V core on FPGA with a UART peripheral.

## Configuration

You can configure the benchmark using the following variables in the `Makefile` or by passing them to `make`:

| Variable     | Default      | Description                                                                |
| ------------ | ------------ | -------------------------------------------------------------------------- |
| `UART_ADDR`  | `0x10000000` | Memory address of the UART peripheral.                                     |
| `CSR_EN`     | `N`          | Set to `Y` to enable `zicsr` extension and use the `cycle` CSR for timing. |
| `NUM_PRIMES` | `100`        | Number of primes to calculate.                                             |

## Building

To build the benchmark binary (`prime.bin`) and ELF executable (`prime.elf`):

```bash
# Build with defaults
make

# Build with custom UART address and CSR enabled
make UART_ADDR=0x80000000 CSR_EN=Y
```

## Running the Benchmark

1. **Connect the Hardware**: Connect your FPGA board to your computer via USB/Serial.
2. **Start the Listener**: Run the Python script to listen for the benchmark output and time the execution.
   ```bash
   python3 benchy.py
   ```
   Follow the on-screen prompts to select the correct Serial port.
3. **Run the Program**: Load `prime.bin` (or `prime.elf`) onto your RISC-V processor and start execution.
   - The script waits for a start byte (`0xFF`).
   - The processor calculates primes.
   - The script stops timing when it receives the end byte (`0xFE`).

## How it Works

- **UART Protocol**: 
  - `0xFF`: Benchmark Start
  - `0xFE`: Benchmark End
  - `0xC5`: CSR Cycle Count Report (if `CSR_EN=Y`)
- **Timing**: 
  - Host-side timing is calculated by `benchy.py` between the start and end bytes.
  - If `CSR_EN=Y`, the program also reads the `cycle` CSR and reports the cycle count over UART.
  - Not having CSR registers **SIGNIFICANTLY** worsens accuracy, as UART becomes a limiting factor in execution

## Troubleshooting

- **No Output**: Ensure `UART_ADDR` matches your hardware configuration.
- **Garbage Output**: Verify the baud rate in `benchy.py` (default 115200) matches your hardware.
- **General problems**: Verify CSR is not enabled if not supporting
  - The binary takes up ~1kb, ensure instruction memory can fit that