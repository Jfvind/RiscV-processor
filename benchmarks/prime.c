/*

// USER INPUT: CSR_TIMING and UART_ADDR
// #define CSR_EN TRUE
// #define UART_ADDR 0x00001000
// #define DEBUG_EN

#define NUM_PRIMES 25

#define BOOL int
#define TRUE 1
#define FALSE 0

#define STARTBYTE 0xFF
#define ENDBYTE 0xFE
#define CSRByte 0xC5

// Hardware Definitions
#define LED_ADDR    ((volatile int *) 0x0064)

// ###############################
//  UTILS
// ###############################

static inline int read_cycle(void)
{
    int value;
    __asm__ volatile("csrr %0, 0xC00" : "=r"(value));
    return value;
}

unsigned int squared(unsigned int x)
{
    unsigned int result = 0;
    for (unsigned int i = 0; i < x; i++)
    {
        result += x;
    }
    return result;
}

unsigned int modulus(unsigned int a, unsigned int b)
{
    if (b == 0)
    {
        return FALSE; // division by zero
    }
    unsigned int temp = a;
    while (temp >= b)
    {
        temp -= b;
    }

    return temp;
}

void itoa(unsigned int value, char *str)
{
    // convert integer to hex string, always 8 chars + null terminator
    const char hex_chars[] = "0123456789ABCDEF";
    for (int i = 7; i >= 0; i--)
    {
        str[i] = hex_chars[modulus(value, 16)];
        value >>= 4;
    }
    str[8] = '\0';
}

// ###############################
// UART UTILS
// ###############################
typedef struct
{
    volatile unsigned int DATA;   // Offset 0x00: Data Register
    volatile unsigned int STATUS; // Offset 0x04: Status Register

} UART_t;

volatile UART_t *get_uart(unsigned int addr)
{
    return (volatile UART_t *)addr;
}
// Example
// volatile UART_t *uart0 = get_uart(0x10000000);

void uart_write_char(volatile UART_t *uart, unsigned char c)
{
    // wait until TX is ready
    while ((uart->STATUS & 0x1) == 0)
        ;
    uart->DATA = (unsigned int)c;
}

void uart_write_string(volatile UART_t *uart, const char *str)
{
    while (*str)
    {
        uart_write_char(uart, (unsigned int)*str++);
    }
}

void uart_write_int(volatile UART_t *uart, unsigned int value)
{
    char bytes[9];
    itoa(value, bytes);
    uart_write_string(uart, bytes);
}

void uart_read_char(volatile UART_t *uart, unsigned char *out)
{
    // wait until data is available
    while ((uart->STATUS & 0x2) == 0)
        ;
    *out = (unsigned char)uart->DATA;
}

void uart_read_line(volatile UART_t *uart, unsigned char *buffer, unsigned int max_length)
{
    unsigned int index = 0;
    unsigned char c;
    while (index < max_length - 1)
    {
        uart_read_char(uart, &c);
        if (c == '\n' || c == '\r')
        {
            break;
        }
        buffer[index++] = c;
    }
    buffer[index] = '\0';
}

__attribute__((section(".text.start"))) __attribute__((naked)) void entry()
{
    // We love that no stack pointer is initialized by default
    __asm__ volatile("li sp, 0x00001000"); // Set stack pointer to top of memory
    __asm__ volatile("jal ra, main");
    __asm__ volatile("ecall"); // infinite loop after main returns
}

int main()
{
    *LED_ADDR = 1;
    
    volatile int start_time = 0;
    // INIT UART
    volatile UART_t *uart = get_uart(UART_ADDR);

#ifdef CSR_EN
    start_time = read_cycle();
#else
    // send start byte to indicate benchmark start
    uart_write_char(uart, STARTBYTE);
#endif

    unsigned int primes[NUM_PRIMES];
    unsigned int count = 0;
    unsigned int num = 2;

    while (count < NUM_PRIMES)
    {
        BOOL is_prime = TRUE;
        // calculate i^2 without multiplication
        unsigned int i = 2;
        unsigned int i_squared = 4;

        while (i_squared <= num)
        {
            // calculate num % i without division
            unsigned int mod = modulus(num, i);
            if (mod == 0)
            {
                is_prime = FALSE;
                break;
            }
            i++;
            i_squared = squared(i);
        }
        if (is_prime)
        {
            primes[count++] = num;
#ifdef DEBUG_EN
            // send prime over UART
            uart_write_string(uart, "Found prime: ");
            uart_write_int(uart, num);
            uart_write_string(uart, "\n");
#endif
        }
        num++;
    }

#ifdef CSR_EN
    volatile int end_time = 0;
    end_time = read_cycle();
    volatile int elapsed = end_time - start_time;
    // send Data from the benchmark over UART
    uart_write_char(uart, CSRByte); // start of benchmark data
    uart_write_string(uart, "Start Cycles: ");
    uart_write_int(uart, start_time);
    uart_write_string(uart, "\nEnd Cycles: ");
    uart_write_int(uart, end_time);
    uart_write_string(uart, "\nElapsed Cycles: ");
    uart_write_int(uart, elapsed);
    uart_write_string(uart, "\nNumber of Found Primes: ");
    uart_write_int(uart, count);
    uart_write_string(uart, "\nPrimes:");
    for (unsigned int i = 0; i < count; i++)
    {
        uart_write_string(uart, " ");
        uart_write_int(uart, primes[i]);
    }
    uart_write_string(uart, "\n");

#else
    uart_write_char(uart, ENDBYTE); // end benchmark if no csr
#endif

// send to registerfile
#ifdef REGISTER_TEST
    __asm__ volatile("mv a0, %0" : : "r"(num));
    __asm__ volatile("mv a1, %0" : : "r"(primes[NUM_PRIMES - 1]));
    __asm__ volatile("ecall");
#endif

    return 0;
}
*/

// USER INPUT: CSR_TIMING and UART_ADDR
// #define CSR_EN TRUE
// #define UART_ADDR 0x00001000
// #define DEBUG_EN

// Hardware Definitions
#define UART_ADDR   0x00001000
#define LED_ADDR    ((volatile int *) 0x0064)

#define NUM_PRIMES 25
#define STARTBYTE 0xFF
#define ENDBYTE 0xFE

// --- UART UTILS ---
typedef struct { volatile unsigned int DATA; volatile unsigned int STATUS; } UART_t;
volatile UART_t *get_uart(unsigned int addr) { return (volatile UART_t *)addr; }

void uart_write_char(volatile UART_t *uart, unsigned char c) {
    while ((uart->STATUS & 0x1) == 0); // Wait for Ready
    uart->DATA = c;
}

void uart_write_string(volatile UART_t *uart, const char *str) {
    while (*str) uart_write_char(uart, *str++);
}

// --- MATH HELPERS (Software Implementation) ---

// 1. Soft Multiply: Replaces 'i * i' to avoid __mulsi3 error
unsigned int soft_mul(unsigned int a, unsigned int b) {
    unsigned int res = 0;
    while (b > 0) {
        if (b & 1) res += a;
        a <<= 1;
        b >>= 1;
    }
    return res;
}

// 2. Modulo: Replaces '%' to avoid __umodsi3 error
unsigned int modulus(unsigned int a, unsigned int b) {
    if (b == 0) return 0;
    unsigned int rem = 0;
    for (int i = 31; i >= 0; i--) {
        unsigned int bit = (a >> i) & 1;
        rem = (rem << 1) | bit;
        if (rem >= b) rem -= b;
    }
    return rem;
}

// 3. Division for printing: Replaces '/' to avoid __udivsi3 error
unsigned int div_mod_10(unsigned int *n) {
    unsigned int rem = 0;
    unsigned int input = *n;
    unsigned int divisor = 10;
    unsigned int quo = 0;
    for (int i = 31; i >= 0; i--) {
        rem = (rem << 1) | ((input >> i) & 1);
        if (rem >= divisor) {
            rem -= divisor;
            quo |= (1 << i);
        }
    }
    *n = quo;
    return rem;
}

// Minimal itoa using SB (Store Byte)
void uart_write_int(volatile UART_t *uart, unsigned int value) {
    char buffer[12];
    char *ptr = &buffer[11];
    *ptr = '\0';
    
    if (value == 0) {
        *--ptr = '0';
    } else {
        while (value != 0) {
            unsigned int digit = div_mod_10(&value);
            *--ptr = "0123456789"[digit];
        }
    }
    uart_write_string(uart, ptr);
}

// --- ENTRY POINT ---
__attribute__((section(".text.start"))) __attribute__((naked)) void entry() {
    // 1. Setup Stack (0x1000)
    __asm__ volatile("li sp, 0x00001000");
    
    // 2. Turn on LED (Alive check)
    __asm__ volatile("li t0, 0x64\n\t" "li t1, 1\n\t" "sw t1, 0(t0)\n\t");
    
    // 3. Jump to Main
    __asm__ volatile("jal ra, main");
    
    // 4. Trap Loop
    __asm__ volatile("1: j 1b");
}

// --- MAIN ---
int main() {
    volatile UART_t *uart = get_uart(UART_ADDR);

    // Diagnostic: 'A' (Alive in C)
    uart_write_char(uart, 'A');

    // Start Benchmark
    uart_write_char(uart, STARTBYTE); 

    unsigned int primes[NUM_PRIMES];
    unsigned int count = 0;
    unsigned int num = 2;

    while (count < NUM_PRIMES) {
        int is_prime = 1;
        
        // Manual Square Calc using soft_mul
        unsigned int i = 2;
        unsigned int i_sq = 4; // 2*2

        while (i_sq <= num) {
            if (modulus(num, i) == 0) {
                is_prime = 0;
                break;
            }
            i++;
            i_sq = soft_mul(i, i); // <--- FIXED: Explicit software multiply
        }

        if (is_prime) {
            primes[count++] = num;
            
            // Print: "P: <num>"
            uart_write_string(uart, "P:");
            uart_write_int(uart, num);
            uart_write_string(uart, "\r\n");
        }
        num++;
    }

    uart_write_char(uart, ENDBYTE);
    
    // Success Blink
    *LED_ADDR = 0;
    for(volatile int k=0; k<200000; k++);
    *LED_ADDR = 1;

    return 0;
}