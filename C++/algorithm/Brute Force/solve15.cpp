#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define N 501
using namespace std;
class Node
{
public:
    vector<int> s;
    int dist; // 到原点的距离（倒水量）
    Node()
    {
        s.resize(3);
        dist = 0;
    }
    bool operator<(const Node &t) const
    {
        return dist > t.dist;
    }
};

void solve(int a, int b, int c, int d)
{
    /*
    两个Dijkstra关键状态数组
    dist：每个点到原点的距离（每个状态到原状态的倒水量）
    visited：每个点是否已经是到原点的最短距离（状态是否已经是到原点最少倒水量）
    */
    vector<vector<int>> dist(N, vector<int>(N, INT_MAX)), visited(N, vector<int>(N, false));

    // 初始化
    Node start;
    vector<int> cap = {a, b, c}, ans(N, INT_MAX);
    start.s = {0, 0, c}, start.dist = 0;
    dist[start.s[0]][start.s[1]] = 0;
    priority_queue<Node> pq;
    pq.push(start);

    while (!pq.empty())
    {
        Node u = pq.top();
        // cout << dist[u.s[0]][u.s[1]] << ' ' << u.dist << endl;
        pq.pop();

        // 图判重
        if (visited[u.s[0]][u.s[1]])
            continue;
        visited[u.s[0]][u.s[1]] = true;

        // 到达每个点之后都要更新每个点的内部数据到ans进行查询，找不到目标点内部状态d之后要找最靠近的点的内部状态
        for (int i = 0; i < 3; i++)
        {
            if (u.dist < ans[u.s[i]])
                ans[u.s[i]] = u.dist;
        }

        // 到达了目标点内部状态的值（状态的状态值）就一定是最短距离（最少倒水量）
        if (ans[d] != INT_MAX)
            break;

        // 遍历邻接点（相邻状态）
        for (int i = 0; i < 3; i++) // out
        {
            for (int j = 0; j < 3; j++) // in
            {
                if (i == j || u.s[i] == 0 || u.s[j] == cap[j])
                    continue;

                Node v = u;                                // 邻接点
                int amount = min(cap[j] - v.s[j], v.s[i]); // 两点间距离（倒水量）

                // 计算得到所有邻接点
                v.s[i] -= amount, v.s[j] += amount;
                // u.dist = dist[v.s[0]][v.s[1]]
                v.dist = u.dist + amount;

                /*
                对邻接点进行松弛操作，更新最短路径（最小倒水量）
                dist数组是点的最优状态值，点的dist是尝试值
                虽然两个变量在判断完之后值相同，但是意义作用不一样
                */
                if (dist[v.s[0]][v.s[1]] > v.dist)
                {
                    dist[v.s[0]][v.s[1]] = v.dist;
                    pq.push(v);
                }
            }
        }
    }

    // 输出最优解
    for (int i = d; i >= 0; i--)
    {
        if (ans[i] > 0)
        {
            cout << ans[i] << ' ' << i << endl;
            return;
        }
    }
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int T;
    cin >> T;
    while (T--)
    {
        int a, b, c, d;
        cin >> a >> b >> c >> d;
        solve(a, b, c, d);
    }

    return 0;
}