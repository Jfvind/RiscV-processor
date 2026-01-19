SECTIONS
{
    . = 0x00000000;

    .text : {
        KEEP(*(.text.start)) /*We move main to start*/
        *(.text*)
        *(.rodata*)
    }

    .data : {
        . = ALIGN(4);
        *(.data*)
    }

    .bss : {
        . = ALIGN(4);
        *(.bss*)
        *(COMMON)
    }

}