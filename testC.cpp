// 生成并查集测试用例文件
#include <iostream>
#include <vector>
#include <algorithm>
#define ll long long

using namespace std;

class UnionFind
{
private:
    // 父节点数组，初始值为自己
    vector<int> parent;

public:
    // 构造函数，初始化所有节点
    UnionFind(int n)
    {
        parent.resize(n + 1);
        for (int i = 1; i <= n; ++i)
            parent[i] = i;
    }

    // 合并两个节点 x 和 y，路径压缩后，x 的父节点设置为 y 的父节点
    void Union(int x, int y)
    {
        if (Find(x) != Find(y))
            parent[x] = parent[y];
    }

    // 查找节点 x 的根节点，并进行路径压缩
    int Find(int x)
    {
        if (parent[x] != x)
            parent[x] = Find(parent[x]);
        return parent[x];
    }
};

// 用于解决特定问题的函数，当前为空
void solve()
{
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    // 解决问题的调用
    solve();

    return 0;
}