#include <stdio.h>
#include <string.h>

int main()
{
    char a[100];
    strcpy(a, "hello world!");
    for (int i = 0; i < strlen(a); i++)
        printf("%c", a[i]);

    return 0;
}