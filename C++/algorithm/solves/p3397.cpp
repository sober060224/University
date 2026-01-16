#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class ditan
{
public:
    int x1, y1, x2, y2;
};

void solve(vector<ditan> ditans, int n)
{
    vector<vector<int>> a(n + 2, vector<int>(n + 2, 0));

    for (ditan t : ditans)
    {
        a[t.x1][t.y1]++;
        a[t.x2 + 1][t.y2 + 1]++;
        a[t.x1][t.y2 + 1]--;
        a[t.x2 + 1][t.y1]--;
    }

    // 容斥原理计算前缀和
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= n; j++)
            a[i][j] += a[i - 1][j] + a[i][j - 1] - a[i - 1][j - 1];

    for (int i = 1; i <= n; i++)
    {
        for (int j = 1; j <= n; j++)
        {
            cout << a[i][j] << ' ';
        }
        cout << endl;
    }

    // vector<vector<int>> diff(n + 1, vector<int>(n + 1, 0));

    // for (ditan t : ditans)
    // {
    //     diff[t.x1][t.y1]++;
    //     if (t.x2 < n && t.y2 < n)
    //         diff[t.x2 + 1][t.y2 + 1]++;
    //     if (t.x2 < n)
    //         diff[t.x2 + 1][t.y1]--;// 左下
    //     if (t.y2 < n)
    //         diff[t.x1][t.y2 + 1]--;// 右上
    // }

    // vector<vector<int>> res(n, vector<int>(n, 0));
    // for (int i = 0; i < n; i++)
    // {
    //     for (int j = 0; j < n; j++)
    //     {
    //         res[i][j] = diff[i + 1][j + 1];
    //         if (i > 0)
    //             res[i][j] += res[i - 1][j];
    //         if (j > 0)
    //             res[i][j] += res[i][j - 1];
    //         if (i > 0 && j > 0)
    //             res[i][j] -= res[i - 1][j - 1];
    //     }
    // }

    // for (int i = 0; i < n; i++)
    // {
    //     for (int j = 0; j < n; j++)
    //     {
    //         cout << res[i][j] << ' ';
    //     }
    //     cout << endl;
    // }
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m;
    cin >> n >> m;
    vector<ditan> ditans(m);
    for (int i = 0; i < m; i++)
        cin >> ditans[i].x1 >> ditans[i].y1 >> ditans[i].x2 >> ditans[i].y2;

    solve(ditans, n);

    return 0;
}