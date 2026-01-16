#include <iostream>
#include <vector>
#include <algorithm>
#include <unordered_map>
using namespace std;

void solve(vector<int> a, int c)
{
    sort(a.begin(), a.end());
    unordered_map<int, int> um;
    int sum = 0;

    for (int i = 0; i < a.size(); i++)
    {
        if (um.find(a[i] - c) != um.end())
            sum += um[a[i] - c];
        um[a[i]]++;
    }

    cout << sum;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, c;
    cin >> n >> c;
    // unordered_map<int, int> um;
    vector<int> a(n);
    // int sum = 0;

    for (int i = 0; i < n; i++)
    {
        // int t;
        // cin >> t;
        // if (um.find(t) != um.end())
        //     sum++;
        // um[c + t] = 1;
        cin >> a[i];
    }

    solve(a, c);
    // cout << sum;

    return 0;
}