#include <climits>
#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

class Squre
{
public:
    int t, c, cnt;
    Squre(int t, int c, int k) : t(t), c(c), cnt(c * (t - k)) {}
    bool operator<(const Squre other) const
    {
        return t < other.t;
    }
};

bool solve(vector<Squre> squres, int mid, int m)
{
    // 每个t到总耗时mid需要花费的资源和m对比，大于m该方案不可行
    int n = squres.size(), sum = 0;

    for (int i = 0; i < n; i++)
        sum = squres[i].c * (squres[i].t - mid) + sum;

    return sum <= m;
    
    // // 每次先拿出最大的t，如果这个区域cnt>0，m减去相应c，再放回堆内，直到m = 0
    // priority_queue<Squre> pq;
    // int n = squres.size(), num = n;
    // for (int i = 0; i < n; i++)
    //     pq.push(squres[i]);
    // // 求开垦n块区域的最少耗时
    // while (m && num)
    // {
    //     Squre maximum = pq.top();
    //     pq.pop();
    //     if (maximum.cnt)
    //     {
    //         if (m < maximum.c)
    //         {
    //             pq.push(maximum);
    //             break;
    //         }
    //         m -= maximum.c;
    //         maximum.cnt--, maximum.t--;
    //         pq.push(maximum);
    //     }
    //     else
    //         num--;
    // }
    // cout << pq.top().t;
}

/**
 * @param n:表示待开垦的区域总数
 * @param m:资源数量
 * @param k:开垦一块区域的最少天数
 * @param ti:第i块区域开垦耗时
 * @param ci:耗时缩短1天所需资源数量
 */
signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m, k, right = INT_MIN;
    cin >> n >> m >> k;

    vector<Squre> squres;
    for (int i = 0; i < n; i++)
    {
        int t, c;
        cin >> t >> c;
        right = max(right, t);
        squres.push_back(Squre(t, c, k));
    }

    // for (int i = 0; i < n; i++)
    //     cout << squres[i].t << ' ' << squres[i].c << ' ' << squres[i].cnt << endl;

    int left = k, res;

    // 尽可能小的总耗时（最大值）
    while (left <= right)
    {
        int mid = left + (right - left) / 2;

        if (solve(squres, mid, m))
        {
            res = mid;
            right = mid - 1;
        }
        else
            left = mid + 1;
    }

    cout << res;
}