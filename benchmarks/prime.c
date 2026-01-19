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
    volatile unsigned char DATA;         // Offset 0x00: Data Register
    volatile unsigned char RESERVED[3];  // Padding to align to 4 bytes
    volatile unsigned char STATUS;       // Offset 0x04: Status Register
    volatile unsigned char RESERVED2[3]; // Padding to align to 4 bytes

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
    uart->DATA = (unsigned char)c;
}

void uart_write_string(volatile UART_t *uart, const char *str)
{
    while (*str)
    {
        uart_write_char(uart, (unsigned char)*str++);
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
    __asm__ volatile("li sp, 0x00011000"); // Set stack pointer to top of memory
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

    return 0;
}