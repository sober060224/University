#include <climits>
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

    int n, k;
    cin >> n >> k;
    vector<int> w(n);
    for (int i = 0; i < n; i++)
        cin >> w[i];

    vector<int> prefixMax(n, INT_MIN), suffixMax(n, INT_MIN), prefixMin(n, INT_MAX), suffixMin(n, INT_MAX);
    prefixMax[0] = prefixMin[0] = w[0];
    suffixMax[n - 1] = suffixMin[n - 1] = w[n - 1];

    for (int i = 1; i < n; i++)
    {
        prefixMax[i] = max(prefixMax[i - 1], w[i]);
        prefixMin[i] = min(prefixMin[i - 1], w[i]);
    }

    for (int i = n - 2; i >= 0; i--)
    {
        suffixMax[i] = max(suffixMax[i + 1], w[i]);
        suffixMin[i] = min(suffixMin[i + 1], w[i]);
    }

    int res = INT_MAX;
    for (int i = 0; i < n - k - 1; i++)
    {
        int leftMax = prefixMax[i], leftMin = prefixMin[i];
        int rightMax = suffixMax[i + k + 1], rightMin = suffixMin[i + k + 1];
        int MAX = max(leftMax, rightMax), MIN = min(leftMin, rightMin);
        res = min(res, MAX - MIN);
    }

    cout << res;
}