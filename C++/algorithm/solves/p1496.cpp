#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(vector<pair<int, int>> fires)
{
    int n = fires.size(), sum = 0;
    vector<int> vis(n, false);
    sort(fires.begin(), fires.end());

    int res = 0;
    for (int i = 0; i < n; i++)
    {
        if (vis[i])
            continue;
        vis[i] = true;

        int s = fires[i].first, e = fires[i].second, next = i, j;

        while (j != next + 1)
        {
            // 找当前结尾最长能扩展的位置
            for (j = next + 1; j < n && fires[j].first <= e && !vis[j]; j++)
            {
                e = max(e, fires[j].second);
                vis[j] = true;
                next = j;
            }
        }
        int len = e - s;

        // for (int j = i + 1; j < n && !vis[j]; j++)
        // {
        //     if (fires[j].first <= e)
        //     {
        //         len = max(len, fires[j].second - s);
        //         vis[j] = true;
        //     }
        //
        //     e = fires[j].second;
        // }

        res += len;
    }

    cout << res;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n;
    cin >> n;
    vector<pair<int, int>> fires(n);
    for (int i = 0; i < n; i++)
        cin >> fires[i].first >> fires[i].second;

    solve(fires);

    return 0;
}