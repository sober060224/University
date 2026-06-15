#include <bits/stdc++.h>
using namespace std;
typedef long long ll;
ll n, m, k;
ll ans = -1;
map<ll, ll, greater<ll>> land; // 按天数降序
int main()
{
    scanf("%lld%lld%lld", &n, &m, &k);
    for (int i = 0; i < n; i++)
    {
        ll t, c;
        scanf("%lld%lld", &t, &c);
        land[t] += c;
    }
    land[k] += 0; // 最少天数，保证每次能有两个合并
    map<ll, ll, greater<ll>>::iterator it = land.begin();
    while (m >= it->second && it->first > k) // 资源还够减少天数，且不是全部都为最小天数
    {
        map<ll, ll, greater<ll>>::iterator it1 = it;
        it1++;
        ll cost = ((it->first - it1->first) * it->second);
        if (m - cost < 0) // 不够减
        {
            ans = it->first;
            while (m >= it->second)
            {
                m -= it->second;
                ans--;
            }
            printf("%lld", ans);
            break;
        }
        else // 足够合并
        {
            m -= cost;
            it1->second += it->second;
            land.erase(it->first);
        }
        it = land.begin();
    }
    if (ans == -1) // 其实这已经说明能全部减到最小天数了
        printf("%lld", it->first);
    return 0;
}