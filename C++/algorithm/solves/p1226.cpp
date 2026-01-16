#include <iostream>
#include <vector>
#include <algorithm>
#define ll long long
using namespace std;

int solve(int a, int b, int p)
{
    int t = a, sum = 1;
    while (b)
    {
        if (b & 1)
            sum = (sum * t) % p;
        t = (t * t) % p;
        b >>= 1;
    }
    return sum % p;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int a, b, p;
    cin >> a >> b >> p;
    cout << a << '^' << b << " mod " << p << '=' << solve(a, b, p);

    return 0;
}