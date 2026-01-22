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

// 01Knapsack01背包问题
class herb
{
public:
    int time, value;
    herb(int time, int value) : time(time), value(value) {}
    herb() {}
};

/**
 * 计算01背包问题的最大价值
 * @param herbs 物品列表，每个物品包含时间和价值
 * @param T 背包容量
 * @return 最大价值
 */
int MaxValue(vector<herb> &herbs, int T)
{
    // 使用一维数组优化空间复杂度
    vector<int> d(T + 1, 0);
    for (int i = 0; i < herbs.size(); i++)
        for (int j = T; j >= herbs[i].time; j--)
            // 动态规划转移方程：d[j] = max(不选当前物品，选当前物品)
            d[j] = max(d[j - herbs[i].time] + herbs[i].value, d[j]);
    return d[T];
}

/**
 * 测试01背包问题
 */
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
/**
 * 计算最长公共子序列的长度
 * @param p1 第一个序列
 * @param p2 第二个序列
 * @return 最长公共子序列的长度
 */
int LongestSubsequence(vector<int> &p1, vector<int> &p2)
{
    int n = p1.size();
    vector<vector<int>> d(n + 1, vector<int>(n + 1));
    // 初始化边界条件
    for (int i = 0; i <= n; i++)
        d[0][i] = 0;
    for (int i = 0; i <= n; i++)
        d[i][0] = 0;
    // 动态规划填充表
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= n; j++)
            if (p1[i - 1] == p2[j - 1])
                d[i][j] = d[i - 1][j - 1] + 1;
            else
                d[i][j] = max(d[i - 1][j], d[i][j - 1]);
    return d[n][n];
}

/**
 * 测试最长公共子序列问题
 */
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
/**
 * 计算字符串的最小编辑距离
 * @param A 字符串1
 * @param B 字符串2
 * @return 最小编辑距离
 */
int MinimumEdit(string &A, string &B)
{
    int n = A.size(), m = B.size();
    vector<vector<int>> d(n + 1, vector<int>(m + 1));
    // 初始化边界条件
    for (int i = 0; i <= n; i++)
        d[i][0] = i;
    for (int i = 0; i <= m; i++)
        d[0][i] = i;
    // 动态规划填充表
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++)
            if (A[i - 1] == B[j - 1])
                d[i][j] = d[i - 1][j - 1];
            else
                d[i][j] = min({d[i - 1][j], d[i][j - 1], d[i - 1][j - 1]}) + 1;
    return d[n][m];
}

/**
 * 测试最小编辑距离问题
 */
void test()
{
    string A, B;
    cin >> A >> B;
    cout << MinimumEdit(A, B);
}

// Cut Rod钢条切割问题
/**
 * 计算钢条切割的最大价值及方案
 * @param prices 钢条价格数组
 * @param n 钢条长度
 */
void CutRod(vector<int> &prices, int n)
{
    int m = prices.size();
    vector<int> d(n + 1, 0);   // 存储最大价值
    vector<int> rec(n + 1, 0); // 记录切割方案
    for (int i = 1; i <= n; i++)
    {
        for (int j = 1; j <= min(m, i); j++)
        {
            if (d[i] < d[i - j] + prices[j - 1])
            {
                d[i] = d[i - j] + prices[j - 1];
                rec[i] = j; // 记录当前最优切割长度
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

/**
 * 测试钢条切割问题
 */
void test()
{
    vector<int> prices = {1, 5, 8, 9, 10, 17, 17, 20, 24, 30};
    CutRod(prices, 6);
}

// Matrix Chain Order矩阵链乘法问题
/**
 * 计算矩阵链乘法的最小计算次数
 * @param p 矩阵维度数组
 */
void MatrixChain(vector<int> p)
{
    int n = p.size() - 1;
    // dp的下标代表矩阵的下标
    vector<vector<int>> d(n, vector<int>(n, INT_MAX));
    for (int i = 0; i < n; i++)
        d[i][i] = 0;
    // 动态规划填充表
    for (int l = 2; l <= n; l++) // 矩阵链长度
    {
        for (int i = 0; i <= n - l; i++) // 起始矩阵索引
        {
            int j = i + l - 1;          // 结束矩阵索引
            for (int k = i; k < j; k++) // 分割点
            {
                // 计算分割后的乘法次数
                int t = d[i][k] + d[k + 1][j] + p[i] * p[k + 1] * p[j + 1];
                d[i][j] = min(d[i][j], t);
            }
        }
    }
    cout << d[0][n - 1];
}

/**
 * 测试矩阵链乘法问题
 */
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
