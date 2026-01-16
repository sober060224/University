#include <climits>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class ticket
{
public:
    int len = INT_MAX, x, y;
};

void solve(vector<int> a, int m)
{
    int n = a.size() - 1, sum = 0, x = 0, y = INT_MAX, r, l = 1;
    vector<int> cnt(m + 1, 0);

    for (r = 1; r <= n ; r++)
    {
        if (cnt[a[r]] == 0)
            sum++;
        cnt[a[r]]++;

        while (sum == m)
        {
            if (y - x > r - l)
                y = r, x = l;
            
            if (cnt[--a[l]] == 0)
                sum--;
            l++;
        }
    }

    cout << x << ' ' << y;

    // ticket t;
    
    // for (int i = 1; i <= n; i++)
    // {
    //     vector<int> vis(m + 1, false);
    //     int j = i + 1, sum = m - 1;
    //     vis[a[i]] = true;

    //     while (j < n && sum > 0)
    //     {
    //         if (!vis[a[j]])
    //         {
    //             sum--;
    //             vis[a[j]] = true;
    //         }
    //         j++;
    //     }

    //     if (j - i - 1 < t.len && sum == 0)
    //         t.len = j - i - 1, t.x = i, t.y = j - 1;
    // }

    // cout << t.x << ' ' << t.y;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m;
    cin >> n >> m;
    vector<int> a(n + 1);
    for (int i = 1; i <= n; i++)
        cin >> a[i];

    solve(a, m);

    return 0;
}