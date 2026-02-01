#include <string.h>
#include <stdio.h>

int main()
{
    int rule;
    char argv[100] = "0x1808";
    for (int i = 2; i < strlen(argv); i++)
    {
        rule *= 16;
        rule += (argv[i] - '0');
    }
    printf("%d", rule);

    return 0;
}