#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(vector<int> a, vector<pair<int, int>> q)
{
    int n = a.size();
    vector<int> sum(n + 1);
    sum[0] = 0;
    for (int i = 0; i < n; i++)
        sum[i + 1] = sum[i] + a[i];
    
    for (pair<int, int> i : q)
        cout << sum[i.second] - sum[i.first - 1] << endl;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n;
    cin >> n;
    vector<int> a(n);
    for (int i = 0; i < n; i++)
        cin >> a[i];
    int m;
    cin >> m;
    vector<pair<int, int>> q(m);
    for (int i = 0; i < m; i++)
        cin >> q[i].first >> q[i].second;
    
    solve(a, q);

    return 0;
}