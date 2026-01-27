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
    *x = (*num & ((1 << 8) - 1));
    *num >>= 8;
}

void decodeBit(int num, int *a, int *b, int *c)
{
    decodeBitAssist(&num, c);
    decodeBitAssist(&num, b);
    *a = num;
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

    int **ab = (int **)malloc(sizeof(int *) * 10);
    for (int i = 0; i < 10; i++)
        ab[i] = (int *)malloc(sizeof(int) * 10);
    for (int i = 0; i < 10; i++)
        free(ab[i]);
    free(ab);

    return 0;
}