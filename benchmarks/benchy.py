import serial
import serial.tools.list_ports
import time

fmax = 100_000_000  # 100 MHz

def csr_benchmark(ser: serial.Serial):
    while True:
        # get the strings outputted by the csr benchmark until we get to the end byte 0xfe
        line = ser.readline().decode('utf-8').strip()
        print(line)
        if line.startswith("Elapsed Cycles:"):
            cycles = int(line.split()[-1], 16) # parse hex value
            seconds = cycles / fmax
            print(f"With Fmax = {fmax / 1_000_000} MHz, the CSR Benchmark completed in {seconds:.10f} seconds.")
        if line.startswith("Goodbye!"):
            break
    print(f"With Fmax = {fmax / 1_000_000} MHz, the CSR Benchmark completed in {line.split()[-2]} seconds.")
    
def default_benchmark(ser: serial.Serial):
    # get time of first reciefed 0xff byte
        while True:
            byte = ser.read(1)
            if byte == b'\xff':
                break
            if byte == b'\xc5':
                print("CSR Benchmark detected.")
                return csr_benchmark(ser)

        start_time = time.time()
        print("Benchmark started.")
        # read until we get 0xfe byte
        while True:
            byte = ser.read(1)
            if byte == b'\xfe':
                break
        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"Benchmark completed in {elapsed_time:.10f} seconds.")

def main():
    global fmax
    print("Prime RV32I Benchmark Runner!")
    fmax = int(input("Enter the Fmax of your design in MHz: ")) * 1_000_000
    print("Found UART devices:")
    device_list = []
    for i, port in enumerate(serial.tools.list_ports.comports()):
        print(f"[{i}] - {port.device}: {port.description}")
        device_list.append(port.device)
    print("Select FPGA UART port to connect to:")
    port_index = int(input("Port: "))
    port_name = device_list[port_index]
    baud_rate = int(input("Enter baud rate (e.g., 115200): "))
    with serial.Serial(port_name, baud_rate, timeout=1, stopbits=2) as ser:
        print(f"Connected to {port_name} at {baud_rate} baud.")
        print("Reset board or load program to start benchmark.")
        default_benchmark(ser)
    print("Benchmark session ended.")

if __name__ == "__main__":
    main()