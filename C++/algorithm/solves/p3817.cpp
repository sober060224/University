#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

void solve(vector<int> a, int x)
{
    int sum = 0;
    for (int i = 1; i < a.size(); i++)
    {
        int diff = a[i - 1] + a[i] - x;
        if (diff > 0)
        {
            sum += diff;
            a[i] -= diff;
        }
    }

    cout << sum;
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, x;
    cin >> n >> x;
    vector<int> a(n + 1);
    for (int i = 1; i <= n; i++)
        cin >> a[i];

    solve(a, x);
}