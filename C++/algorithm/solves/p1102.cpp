#include <iostream>
#include <vector>
#include <algorithm>
#include <unordered_map>
using namespace std;

// 函数 solve 用于计算满足特定条件的数对数量
void solve(vector<int> a, int c)
{
    // 对数组 a 进行升序排序
    sort(a.begin(), a.end());

    // 使用 unordered_map 来存储数字出现的次数
    unordered_map<int, int> um;
    int sum = 0; // 用于存储符合条件的数对数量

    // 遍历排序后的数组
    for (int i = 0; i < a.size(); i++)
    {
        // 检查是否存在 a[i] - c 的数字
        if (um.find(a[i] - c) != um.end())
            sum += um[a[i] - c]; // 如果存在，累加对应的出现次数
        um[a[i]]++;              // 将当前数字加入 map，记录出现次数
    }

    // 输出符合条件的数对数量
    cout << sum;
}

int main()
{
    // 关闭同步输入输出，提高输入输出效率
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, c;
    cin >> n >> c; // 读取输入的数组长度和目标差值 c

    vector<int> a(n); // 定义一个长度为 n 的数组

    // 读取数组元素
    for (int i = 0; i < n; i++)
    {
        cin >> a[i];
    }

    solve(a, c); // 调用 solve 函数处理数组

    return 0;
}
