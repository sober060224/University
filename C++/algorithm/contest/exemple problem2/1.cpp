#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

void solve()
{

}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m;
    cin >> n >> m;
    queue<int> q;
    for (int i = 1; i <= n; i++)
        q.push(i);

    int cnt = 0, end;
    while (q.size() > 1)
    {
        cnt++;
        end = q.front();
        q.pop();
        if (cnt % m != 0)
            q.push(end);
        else
            cnt = 0;
    }

    cout << q.front();
}