#include <iostream>
#include <vector>
#include <algorithm>
#define N 501
using namespace std;

int solve(vector<int> &a, int lo, int hi)
{
    if (lo >= hi)
        return 0;

    int mid = (hi + lo) / 2;

    int left = solve(a, lo, mid);
    int right = solve(a, mid + 1, hi);
    
    
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    // 第一行，一个数 n，表示序列中有 n 个数。
    // 第二行 n 个数，表示给定的序列。序列中每个数字不超过 109
    // int n;
    // cin >> n;
    // vector<int> a(n);
    // for (int i = 0; i < n; i++)
    // {
    //     cin >> a[i];
    // }
    cout << 1;
    // 输出序列中逆序对的数目。

    return 0;
}