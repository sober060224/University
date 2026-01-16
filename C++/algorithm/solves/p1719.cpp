#include <cstring>
#include <cctype>
#include <iostream>
#include <vector>
#include <climits>
#include <algorithm>
using namespace std;

void solve(vector<vector<int>> a)
{
    int n = a.size(), res = INT_MIN;
    vector<vector<int>> sum(n + 1, vector<int>(n + 1, 0));

    // for (int i = 0; i < n; i++)
    //     for (int j = 0; j < n; j++)
    //         sum[i + 1][j + 1] = sum[i + 1][j] + sum[i][j + 1] - sum[i][j] + a[i][j];

    // for (int x2 = 1; x2 <= n; x2++)
    //     for (int y2 = 1; y2 <= n; y2++)
    //         for (int x1 = 1; x1 <= x2; x1++)
    //             for (int y1 = 1; y1 <= y2; y1++)
    //                 res = max(sum[x2][y2] - sum[x2][y1 - 1] - sum[x1 - 1][y2] + sum[x1 - 1][y1 - 1], res);

    vector<int> d(n + 1), t(n);
    for (int i = 0; i < n; i++)
    {
        fill(t.begin(), t.end(), 0);
        for (int j = i; j < n; j++)
        {
            for (int k = 0; k < n; k++)
                t[k] += a[j][k];
            
            d[0] = 0;
            for (int x = 1; x <= n; x++)
            {
                d[x] = max(d[x - 1] + t[x - 1], t[x - 1]);
                res = max(d[x], res);
            }
        }
    }

    cout << res;
}

void test()
{
    vector<int> a = {9, 2, -6, 2, -10}, d(a.size(), 0);
    d[0] = a[0];
    for (int i = 1; i < a.size(); i++)
        d[i] = max(d[i - 1] + a[i], a[i]);

    for (int i = 0; i < a.size(); i++)
        cout << d[i] << ' ';
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    // test();
    int n;
    cin >> n;
    vector<vector<int>> a(n, vector<int>(n));
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            cin >> a[i][j];

    solve(a);

    return 0;
}