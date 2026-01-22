#include <cstring>   // 字符串处理头文件，用于strlen等函数
#include <cctype>    // 字符处理头文件，用于字符分类函数
#include <iostream>  // 输入输出流头文件，用于cin和cout
#include <vector>    // 向量容器头文件，用于动态数组
#include <climits>   // 极限值头文件，包含INT_MIN等常量
#include <algorithm> // 算法头文件，提供sort等函数
using namespace std; // 使用标准命名空间，简化代码

// solve函数：计算二维数组的最大子矩阵和（或类似问题），使用动态规划
// 参数a：二维整数数组，表示输入数据
void solve(vector<vector<int>> a)
{
    int n = a.size(), res = INT_MIN;                       // n是数组维度，res初始化为最小整数值，用于存储最大和
    vector<vector<int>> sum(n + 1, vector<int>(n + 1, 0)); // 创建前缀和数组，大小为(n+1)x(n+1)，初始化为0

    // 原代码注释：这部分是计算二维前缀和，但被注释掉，未使用
    /*
    // for (int i = 0; i < n; i++)
    //     for (int j = 0; j < n; j++)
    //         sum[i + 1][j + 1] = sum[i + 1][j] + sum[i][j + 1] - sum[i][j] + a[i][j];
    // 该部分计算前缀和，用于后续快速计算子矩阵和
    */

    // 原代码注释：这部分是暴力枚举子矩阵并计算和，但被注释掉，未使用
    /*
    // for (int x2 = 1; x2 <= n; x2++)
    //     for (int y2 = 1; y2 <= n; y2++)
    //         for (int x1 = 1; x1 <= x2; x1++)
    //             for (int y1 = 1; y1 <= y2; y1++)
    //                 res = max(sum[x2][y2] - sum[x2][y1 - 1] - sum[x1 - 1][y2] + sum[x1 - 1][y1 - 1], res);
    // 该部分使用前缀和计算所有子矩阵和，并更新最大值res
    */

    // 采用动态规划方法计算最大子矩阵和（或类似问题）
    vector<int> d(n + 1), t(n); // d是动态规划数组，t用于临时存储行和
    for (int i = 0; i < n; i++)
    {
        fill(t.begin(), t.end(), 0); // 将t数组清零，t.size()是n
        for (int j = i; j < n; j++)
        {
            for (int k = 0; k < n; k++)
                t[k] += a[j][k]; // 计算当前行j的列和，存储在t[k]中

            d[0] = 0; // 初始化d数组的起点
            for (int x = 1; x <= n; x++)
            {
                d[x] = max(d[x - 1] + t[x - 1], t[x - 1]); // 动态规划更新：计算当前列x的最大和
                res = max(d[x], res);                      // 更新全局最大值res
            }
        }
    }

    cout << res; // 输出结果res
}

// test函数：测试一维数组的最大子数组和问题
// 参数：无，使用内部定义的数组进行测试
void test()
{
    vector<int> a = {9, 2, -6, 2, -10}, d(a.size(), 0); // 定义测试数组a和动态规划数组d
    d[0] = a[0];                                        // 初始化d[0]
    for (int i = 1; i < a.size(); i++)
        d[i] = max(d[i - 1] + a[i], a[i]); // 动态规划计算一维最大子数组和

    // 输出d数组，用于调试或验证
    for (int i = 0; i < a.size(); i++)
        cout << d[i] << ' ';
}

int main()
{
    ios::sync_with_stdio(false); // 禁用同步以提高输入输出性能
    cin.tie(0), cout.tie(0);     // 解绑cin和cout，进一步提升速度

    // test(); // 注释掉的测试函数调用，用于单独测试一维情况

    int n;                                    // 定义二维数组维度n
    cin >> n;                                 // 从输入读取n
    vector<vector<int>> a(n, vector<int>(n)); // 定义二维数组a，大小为n x n
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            cin >> a[i][j]; // 读入二维数组元素

    solve(a); // 调用solve函数计算并输出结果

    return 0; // 程序结束，返回0
}
