#include <unordered_map>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve()
{

}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m;
    cin >> n >> m;
    vector<int> a(n + 1);
    unordered_map<int, int> um;
    for (int i = 1; i <= n; i++)
    {
        cin >> a[i];
        if (um.find(a[i]) == um.end())
            um[a[i]] = i;
    }

    for (int i = 0; i < m; i++)
    {
        int t;
        cin >> t;
        if (um.find(t) != um.end())
            cout << um[t] << ' ';
        else
            cout << -1 << ' ';
    }

    return 0;
}