#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>

int getBit(int num, int n)
{
    if (num & (1 << n))
        return 1;
    return 0;
}

int codeBit(int a, int b, int c)
{
    int res = a;
    int countBit = 8;
    res <<= countBit;
    res += b;

    res <<= countBit;
    res += c;
    printf("%d\n", res);

    return res;
}

void decodeBitAssist(int *num, int *x)
{
    for (int i = 0; i < 8; i++)
    {
        *x <<= 1;
        if (*num & 1)
            *x += 1;
        *num >>= 1;
    }
}

void decodeBit(int num, int *a, int *b, int *c)
{
    decodeBitAssist(&num, c);
    decodeBitAssist(&num, b);
    decodeBitAssist(&num, a);
}

int main()
{
    // 1 2 4 8 16 32 64
    // 1 0 1 1 0  1
    printf("%d\n", getBit(45, 3));
    // 1 2 4 8 16 32 64
    // 0 0 1 1
    printf("%d\n", getBit(12, 3));
    // 1 2 4 8 16 32 64
    // 1 0 0 1 1  1
    printf("%d\n", getBit(57, 3));

    codeBit(2, 4, 1);

    int a = 0, b = 0, c = 0;
    decodeBit(132097, &a, &b, &c);
    printf("%d %d %d", a, b, c);

    return 0;
}