#include <algorithm>
#include <vector>
#include <iostream>
#include <cmath>
using namespace std;

// 遍历输出数组元素值
void show(vector<int> nums)
{
    for (int &num : nums)
    {
        cout << num << ' ';
    }
    cout << endl;
}

// 01Knapsack01背包
class herb
{
public:
    int time, value;
    herb(int time, int value) : time(time), value(value) {}
    herb() {}
};
int MaxValue(vector<herb> &herbs, int T)
{
    vector<int> d(T + 1, 0);
    for (int i = 0; i < herbs.size(); i++)
        for (int j = T; j >= herbs[i].time; j--)
            //d[i][j] = max(d[i - 1][j], d[i - 1][j - herbs[i].time] + herbs[i].value);
            d[j] = max(d[j - herbs[i].time] + herbs[i].value, d[j]);
    return d[T];
}
void test()
{
    int T, M;
    cin >> T >> M;
    vector<herb> herbs;
    for (int i = 0; i < M; i++)
    {
        int a = 0, b = 0;
        cin >> a >> b;
        herbs.push_back(herb(a, b));
    }
    cout << MaxValue(herbs, T);
}

// Longest Common Subsequence最长公共子序列问题
int LongestSubsequence(vector<int> &p1, vector<int> &p2)
{
    int n = p1.size();
    vector<vector<int>> d(n + 1, vector<int>(n + 1));
    for (int i = 0; i <= n; i++)
        d[0][i] = 0;
    for (int i = 0; i <= n; i++)
        d[i][0] = 0;
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= n; j++)
            if (p1[i - 1] == p2[j - 1])
                d[i][j] = d[i - 1][j - 1] + 1;
            else
                d[i][j] = max(d[i - 1][j], d[i][j - 1]);
    return d[n][n];
}
void test()
{
    int n;
    cin >> n;
    vector<int> p1(n), p2(n);
    for (int i = 0; i < n; i++)
        cin >> p1[i];
    for (int i = 0; i < n; i++)
        cin >> p2[i];
    cout << LongestSubsequence(p1, p2);
}

// Minimum Edit Distance最小编辑距离
int MinimumEdit(string &A, string &B)
{
    int n = A.size(), m = B.size();
    vector<vector<int>> d(n + 1, vector<int>(m + 1));
    for (int i = 0; i <= n; i++)
        d[i][0] = i;
    for (int i = 0; i <= m; i++)
        d[0][i] = i;
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++)
            if (A[i - 1] == B[j - 1])
                d[i][j] = d[i - 1][j - 1];
            else
                d[i][j] = min({d[i - 1][j], d[i][j - 1], d[i - 1][j - 1]}) + 1;
    return d[n][m];
}
void test()
{
    string A, B;
    cin >> A >> B;
    cout << MinimumEdit(A, B);
}

// Cut Rod钢条切割
void CutRod(vector<int> &prices, int n)
{
    int m = prices.size();
    vector<int> d(n + 1, 0);
    vector<int> rec(n + 1, 0);
    for (int i = 1; i <= n; i++)
    {
        for (int j = 1; j <= min(m, i); j++)
        {
            if (d[i] < d[i - j] + prices[j - 1])
            {
                // 从长度1到n判断，切不切，切多少价格最大
                d[i] = d[i - j] + prices[j - 1];
                rec[i] = j;
            }
        }
    }
    cout << "最大价格：" << d[n] << endl;
    cout << "切割方案：";
    while (n > 0)
    {
        cout << rec[n] << ' ';
        n -= rec[n];
    }
    cout << "\n-----------------------\n";
}
void test()
{
    vector<int> prices = {1, 5, 8, 9, 10, 17, 17, 20
    , 24, 30};
    CutRod(prices, 6);
}

// Matrix Chain Order矩阵链乘法问题
void MatrixChain(vector<int> p)
{
    int n = p.size() - 1;
    // dp的下标代表矩阵的下标
    vector<vector<int>> d(n, vector<int>(n, INT_MAX));
    for (int i = 0; i < n; i++)
        d[i][i] = 0;
    for (int l = 2; l <= n; l++)
    {
        for (int i = 0; i <= n - l; i++)
        {
            int j = i + l - 1;
            for (int k = i; k < j; k++)
            {
                // 从矩阵长度为2开始判断最少矩阵乘法次数，动态规划数组从零开始编号，注意下标
                int t = d[i][k] + d[k + 1][j] + p[i] * p[k + 1] * p[j + 1];
                d[i][j] = min(d[i][j], t);
            }
        }
    }
    cout << d[0][n - 1];
}
void test()
{
    vector<int> p = {30, 35, 15, 5, 10, 20, 25};
    MatrixChain(p);
}

int main()
{
    // test();
    return 0;
}