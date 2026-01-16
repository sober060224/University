#include <climits>
#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

void solve(vector<int> a, int target, int maxHeight)
{
    int n = a.size();
    int left = 0, right = maxHeight;
    
    while (left <= right)
    {
        int mid = left + (right - left) / 2;
        int sum = 0;

        for (int i = 0; i < n; i++)
        {
            if (a[i] > mid)
                sum += (a[i] - mid);
        }

        if (sum >= target)
            left = mid + 1;
        else
            right = mid - 1;
    }

    cout << right;
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int N, M, maxHeight = INT_MIN;
    cin >> N >> M;
    vector<int> a(N);
    for (int i = 0; i < N; i++)
    {
        cin >> a[i];
        maxHeight = max(maxHeight, a[i]);
    }

    solve(a, M, maxHeight);

}