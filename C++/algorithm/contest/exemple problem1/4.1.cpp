#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

class line
{
public:
    int v;
    vector<vector<int>> w; // 线路造价

    line(int v, vector<vector<int>> t) : v(v), w(t) {}
};

class Edge
{
public:
    int u, v, w;
    Edge(int u, int v, int w) : u(u), v(v), w(w) {}
    bool operator<(const Edge other) const
    {
        return w > other.w;
    }
};

class UnionFind
{
public:
    vector<int> fa;

    UnionFind(int n)
    {
        fa.resize(n);
        for (int i = 0; i < n; i++)
            fa[i] = i;
    }

    void Union(int a, int b)
    {
        fa[a] = fa[b];
    }

    int Find(int x)
    {
        if (fa[x] != x)
            fa[x] = Find(fa[x]);
        return fa[x];
    }
};

// 用小根堆将两个城镇之间的每种可能造价放进去，取堆顶元素，获得两个城镇之间建电网的最低造价。再清空小根堆找下两个城镇之间造价
vector<Edge> setGraph(vector<vector<int>> powers, vector<vector<line>> lines)
{
    int n = powers.size(), k = powers[0].size();
    vector<Edge> graph;
    priority_queue<Edge> pq;
    for (int u = 0; u < n; u++)
    {
        for (line l : lines[u])
        {
            int v = l.v;
            for (int i = 0; i < k; i++)
            {
                int a = powers[u][i];
                for (int j = 0; j < k; j++)
                {
                    int b = powers[v][j], c = l.w[i][j];
                    pq.push(Edge(u, v, a + b + c));
                }
            }
        }
        Edge t = pq.top();
        graph.push_back(Edge(t.u, t.v, t.w));
        while (!pq.empty())
            pq.pop();
    }

    // int v = lines[u].v;
    // for (int i = 0; i < k; i++)
    // {
    //     int a = powers[u][i];
    //     for (int j = 0; j < k; j++)
    //     {
    //         int b = powers[v][j], c = lines[u].w[i][j];
    //         pq.push(Edge(u, v, a + b + c));
    //     }
    // }
    return graph;
}

void solve(vector<Edge> graph, int n)
{
    // 电网的总造价（变电站造价加上输电线路造价）最低是多少，所有城镇能够被连接成一个电力网络
    // 状态是每个城镇这个点
    // 将所有两个城镇之间的最低造价再从低到高排序，选低造价且并查集保证连通
    sort(graph.begin(), graph.end(), [](Edge a, Edge b)
         { return a.w < b.w; });
    UnionFind uf(n);
    int sum = 0, cnt = 0;

    for (Edge e : graph)
    {
        if (uf.Find(e.u) != uf.Find(e.v) && cnt < n - 1)
        {
            uf.Union(e.u, e.v);
            sum += e.w;
            cnt++;
        }
    }

    cout << sum;
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, m, k;
    cin >> n >> m >> k;

    vector<vector<int>> powers(n, vector<int>(k)); // 城镇不同候选地址的变电站造价
    for (int i = 0; i < n; i++)
        for (int j = 0; j < k; j++)
            cin >> powers[i][j];

    vector<vector<line>> lines(m);
    for (int i = 0; i < m; i++)
    {
        int u, v;
        cin >> u >> v;
        vector<vector<int>> rec(k, vector<int>(k));

        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++)
                cin >> rec[i][j];

        lines[u].push_back(line(v, rec));
        // lines[v].push_back(line(u, rec));
    }

    vector<Edge> graph = setGraph(powers, lines);

    solve(graph, n);
    // for (int i = 0; i < graph.size(); i++)
    // {
    //     for (Edge e : graph[i])
    //     {
    //         cout << i << ' ' << e.v << ' ' << e.w;
    //     }
    //     cout << endl;
    // }
}