#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(vector<int> a)
{
    
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int T;
    cin >> T;
    int n;

    while (T--)
    {
        cin >> n;
        // pre存当前下标元素的前一个下标
        vector<int> a(n), pre(n);
        // um存最近一次当前元素的下标
        unordered_map<int, int> um;

        // 将当前下标元素的前一个下标记录下来
        for (int i = 0; i < n; i++)
        {
            cin >> a[i];
            pre[i] = -1;
            if (um.find(a[i]) != um.end())
                pre[i] = um[a[i]];
            um[a[i]] = i;
        }
        
        int l = 0, r = 0, ans = INT_MIN;
        while (r < n)
        {
            while(r < n && pre[r] < l)
                r++;
            
            ans = max(ans, r - l);
            l = pre[r] + 1;
        }

        cout << ans;
    }

    return 0;
}