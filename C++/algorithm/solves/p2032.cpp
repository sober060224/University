#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(vector<int> a, int k)
{
    deque<int> q;
    vector<int> res;

    for (int i = 0; i < a.size(); i++)
    {
        while (!q.empty() && a[q.back()] <= a[i])
            q.pop_back();
        
        while (i >= k && !q.empty() && i - q.front() >= k)
            q.pop_front();
        
        q.push_back(i);
        
        if (i >= k - 1)
            res.push_back(a[q.front()]);
    }

    for (int i : res)
        cout << i << endl;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, k;
    cin >> n >> k;
    vector<int> a(n);
    for (int i = 0; i < n; i++)
        cin >> a[i];

    solve(a, k);

    return 0;
}