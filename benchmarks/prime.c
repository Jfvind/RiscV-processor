/*// USER INPUT: CSR_TIMING and UART_ADDR
// #define CSR_EN TRUE
// #define UART_ADDR 0x00001000
// #define DEBUG_EN
// USER INPUT: UART_ADDR
#define LED_ADDR  ((volatile int *) 0x0064)
#define NUM_PRIMES 25 

// Forward declaration of the C logic function
void run_benchmark();

// --- 1. STARTUP (NAKED) ---
// This function ONLY sets up the stack and jumps to the logic.
// It contains NO variables, solving your compilation error.
__attribute__((section(".text.start"))) __attribute__((naked)) void _start() {
    // 1. Initialize Stack Pointer (Top of 16KB)
    __asm__ volatile("li sp, 0x4000"); 

    // 2. Jump to the C logic (Linker resolves the short jump)
    // We use a tail-call (jump) instead of call to save stack space
    __asm__ volatile("j run_benchmark");
}

// --- MATH HELPERS (Inline) ---
static inline unsigned int soft_mul(unsigned int a, unsigned int b) {
    unsigned int res = 0;
    while (b > 0) {
        if (b & 1) res += a;
        a <<= 1;
        b >>= 1;
    }
    return res;
}

static inline unsigned int soft_mod(unsigned int a, unsigned int b) {
    if (b == 0) return 0;
    unsigned int r = 0;
    for (int i = 31; i >= 0; i--) {
        r <<= 1;
        r |= (a >> i) & 1;
        if (r >= b) {
            r -= b;
        }
    }
    return r;
}

static inline unsigned int soft_div(unsigned int a, unsigned int b) {
    if (b == 0) return 0;
    unsigned int q = 0;
    unsigned int r = 0;
    for (int i = 31; i >= 0; i--) {
        r <<= 1;
        r |= (a >> i) & 1;
        if (r >= b) {
            r -= b;
            q |= (1 << i);
        }
    }
    return q;
}

// --- 2. BENCHMARK LOGIC (STANDARD C) ---
// This is a normal function. The compiler manages the stack for 'w', 'k', etc.
// We force 'noinline' to ensure it doesn't get merged back into _start.
__attribute__((noinline)) void run_benchmark() {
    
    // LED ON (Proof of life)
    *((volatile int*)0x64) = 1;

    // MANUAL UART SETUP
    volatile int* uart_data = (volatile int*)0x1000;

    // Print "GO"
    *uart_data = 'G';
    for(volatile int w=0; w<2000; w++) __asm__("nop");
    *uart_data = 'O';
    for(volatile int w=0; w<2000; w++) __asm__("nop");
    *uart_data = '\r';
    for(volatile int w=0; w<2000; w++) __asm__("nop");
    *uart_data = '\n';
    for(volatile int w=0; w<2000; w++) __asm__("nop");

    unsigned int count = 0;
    unsigned int num = 2;

    while (count < NUM_PRIMES) {
        int is_prime = 1;
        
        // Check prime
        for (unsigned int i = 2; soft_mul(i, i) <= num; i++) {
            if (soft_mod(num, i) == 0) {
                is_prime = 0;
                break;
            }
        }

        if (is_prime) {
            count++;
            
            // Print Number
            unsigned int val = num;
            if (val >= 100) {
               *uart_data = '0' + soft_div(val, 100);
               for(volatile int w=0; w<2000; w++) __asm__("nop");
               val = soft_mod(val, 100);
            }
            if (val >= 10 || num >= 100) { 
               *uart_data = '0' + soft_div(val, 10);
               for(volatile int w=0; w<2000; w++) __asm__("nop");
               val = soft_mod(val, 10);
            }
            *uart_data = '0' + val; 
            for(volatile int w=0; w<2000; w++) __asm__("nop");
            
            *uart_data = ' '; 
            for(volatile int w=0; w<2000; w++) __asm__("nop");
        }
        num++;
    }

    // DONE - BLINK LED
    while(1) {
        *((volatile int*)0x64) = 0; // OFF
        for(volatile int k=0; k<1000000; k++); 
        *((volatile int*)0x64) = 1; // ON
        for(volatile int k=0; k<1000000; k++); 
    }
}*/

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
    __asm__ volatile("li sp, 0x2FFF0"); // Set stack pointer to top of memory
    __asm__ volatile("jal ra, main");
    __asm__ volatile("ecall"); // infinite loop after main returns
}

int main()
{
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