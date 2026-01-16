#include <climits>
#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

class Fenwick
{
public:
    vector<int> tree;
    int n;
    Fenwick(int size)
    {
        n = size;
        tree.resize(n + 5, 0);
    }
    int lowbit(int x)
    {
        return x & (-x);
    }
    void update(int idx, int delta)
    {
        for (int i = idx; i <= n; i += lowbit(i))
            tree[i] += delta;
    }
    int query(int idx)
    {
        int sum = 0;
        for (int i = idx; i > 0; i -= lowbit(i))
            sum += tree[i];
        return sum;
    }
    int rangeQuery(int l, int r)
    {
        return query(r) - query(l - 1);
    }
    void build(const vector<int> &arr)
    {
        for (int i = 0; i < arr.size(); i++)
            update(i + 1, arr[i]);
    }
};
class RangeUpdatePointQuery
{
public:
    Fenwick bit;
    RangeUpdatePointQuery(int n) : bit(n) {}
    void rangeUpdate(int l, int r, int val)
    {
        bit.update(l, val);
        bit.update(r + 1, -val);
    }
    int pointQuery(int pos)
    {
        return bit.query(pos);
    }
    void build(vector<int> a)
    {
        for (int i = 0; i < a.size(); i++)
            rangeUpdate(i + 1, i + 1, a[i]);
    }
};
class RangeUpdateRangeQuery
{
private:
    vector<int> tree1, tree2;// d[j], j * d[j]
    int n;
    int lowbit(int x)
    {
        return x & (-x);
    }
    void update(int idx, int val)// sum[1, k] = (k + 1) * ∑d[j] - ∑j * d[j]
    {
        int val2 = idx * val;
        for (int i = idx; i <= n; i += lowbit(i))
        {
            tree1[i] += val;
            tree2[i] += val2;
        }
    }
    int query(int idx)
    {
        int sum = 0;
        for (int i = idx; i > 0; i -= lowbit(i))
            sum += (idx + 1) * tree1[i] - tree2[i];
        return sum;
    }
public:
    RangeUpdateRangeQuery(int n) : n(n) 
    {
        tree1.resize(n + 5);
        tree2.resize(n + 5);
    }
    void rangeUpdate(int l, int r, int val)
    {
        update(l, val);
        update(r + 1, -val);
    }
    int rangeQuery(int l, int r)
    {
        return query(r) - query(l - 1);
    }
    void build(vector<int> a)
    {
        for (int i = 0; i < a.size(); i++)
            update(i + 1, a[i]);
    }
};
void test()
{
    vector<int> a = {5, 2, 7, 2, 8, 10};
    int n = a.size();
    RangeUpdatePointQuery rupq(n);
    rupq.build(a);
    rupq.rangeUpdate(2, 4, 10);
    cout << rupq.pointQuery(3) << endl;
    RangeUpdateRangeQuery rurq(n);
    rurq.build(a);
    cout << rurq.rangeQuery(2, 4) << endl;
    rurq.rangeUpdate(2, 4, 10);
    cout << rurq.rangeQuery(2, 4) << endl;
}
class UnionFind
{
private:
    vector<int> parent;
    vector<int> rank;

public:
    UnionFind(int n)
    {
        parent.resize(n);
        rank.resize(n, 0);
        for (int i = 0; i < n; i++)
            parent[i] = i;
    }
    int Find(int target)
    {
        if (parent[target] != target)
            parent[target] = Find(parent[target]);
        return parent[target];
        // return parent[target] == target ? target : Find(parent[target]);
    }
    void Union(int x, int y)
    {
        int x_parent = Find(x), y_parent = Find(y);
        if (rank[x_parent] <= rank[y_parent])
        {
            if (rank[x_parent] = rank[y_parent])
                rank[x_parent]++;
            parent[x_parent] = y_parent;
        }
        else
            parent[y_parent] = x_parent;
    }
};
class Edge
{
public:
    int u, v, w;
    Edge(int u, int v, int w) : u(u), v(v), w(w) {}
    Edge(int v, int w) : v(v), w(w) {}
    bool operator<(const Edge &other) const
    {
        return w > other.w;
    }
};
void removeZero(vector<int> &a)
{
    while (a.back() == 0 && a.size() > 1)
        a.pop_back();
}
bool isLarger(vector<int> a, vector<int> b)
{
    if (a.size() != b.size())
        return a.size() > b.size() ? true : false;
    for (int i = a.size() - 1; i >= 0; i--)
    {
        if (a[i] != b[i])
            return a[i] > b[i] ? true : false;
    }
    return true;
}
vector<int> intToVector(int a)
{
    vector<int> right;
    while (a > 0)
    {
        right.push_back(a % 10);
        a /= 10;
    }
    return right;
}
vector<int> multi(vector<int> a, vector<int> b)
{
    vector<int> res(a.size() + b.size());
    for (int i = 0; i < a.size(); i++)
        for (int j = 0; j < b.size(); j++)
            res[i + j] += a[i] * b[j];
    int carry = 0;
    for (int i = 0; i < res.size(); i++)
    {
        res[i] += carry;
        carry = res[i] / 10;
        res[i] %= 10;
    }
    removeZero(res);
    return res;
}
vector<int> add(vector<int> a, vector<int> b)
{
    while (a.size() > b.size())
        b.push_back(0);
    while (b.size() > a.size())
        a.push_back(0);
    int n = a.size();
    vector<int> res(n + 1);
    for (int i = 0; i < n; i++)
        res[i] = a[i] + b[i];
    int carry = 0;
    for (int i = 0; i < n; i++)
    {
        res[i] += carry;
        carry = res[i] / 10;
        res[i] %= 10;
    }
    removeZero(res);
    return res;
}
vector<int> sub(vector<int> a, vector<int> b)
{
    vector<int> res(a.size());
    int t = 0;
    for (int i = 0; i < a.size(); i++)
    {
        t = a[i] - t;
        if (i < b.size())
            t -= b[i];
        res[i] = (t + 10) % 10;
        if (t < 0)
            t = 1;
        else
            t = 0;
    }
    removeZero(res);
    return res;
}
vector<int> divide(vector<int> a, vector<int> b)
{
    if (b.size() == 1 && b[0] == 0)
        return vector<int>(1, 0);
    vector<int> res, cur;
    for (int i = a.size() - 1; i >= 0; i--)
    {
        cur.insert(cur.begin(), a[i]);
        int cnt = 0;
        while (isLarger(cur, b))
        {
            cur = sub(cur, b);
            cnt++;
        }
        res.push_back(cnt);
    }
    reverse(res.begin(), res.end());
    removeZero(res);
    return res;
}
void DFS1(vector<vector<int>> &graph, int index, vector<int> &visited, vector<int> &topo)
{
    visited[index] = 1;
    for (int &v : graph[index])
        if (visited[v] == 0)
            DFS1(graph, v, visited, topo);
    visited[index] = 2;
    topo.push_back(index);
}
void DFS2(vector<vector<int>> &reverse_graph, int index, vector<int> &visited, vector<int> &component)
{
    visited[index] = 1;
    component.push_back(index);
    for (int &v : reverse_graph[index])
        if (visited[v] == 0)
            DFS2(reverse_graph, v, visited, component);
    visited[index] = 2;
}
void StronglyComponent(vector<vector<int>> &graph, vector<vector<int>> &reverse_graph) // Strongly Connected Component强连通分量
{
    int n = graph.size() - 1;      // n
    vector<int> visited(n + 1, 0); // 0未访问 1正在访问 2完成访问
    vector<int> topo;
    for (int i = 1; i <= n; i++)
        if (visited[i] == 0)
            DFS1(graph, i, visited, topo);
    reverse(topo.begin(), topo.end()); // 得到原图拓扑序
    visited.assign(n + 1, 0);
    vector<int> component;
    int sum = 0;
    for (int &i : topo)
    {
        if (visited[i] == 0)
        {
            component.clear();
            DFS2(reverse_graph, i, visited, component);
            if (component.size() > 1)
                sum++;
        }
    }
    cout << sum;
}
void Kruskal(vector<Edge> &graph, int n) // Kruskal最小生成树
{
    sort(graph.begin(), graph.end(), [](Edge &a, Edge &b)
         { return a.w < b.w; });
    // 并查集记录森林，边的点在森林内不添加，直到成为一棵树
    UnionFind uf(n);
    int sum = 0, edge = 0;
    for (Edge &e : graph)
    {
        int u = e.u, v = e.v, w = e.w;
        if (uf.Find(u) != uf.Find(v) && edge < n - 1)
        {
            uf.Union(u, v);
            sum += w;
            edge++;
        }
    }
    if (edge == n - 1)
        cout << sum;
    else
        cout << "orz";
}
void Dijkstra(vector<vector<Edge>> graph, int s) // Single-source Shortest Path单源最短路径Dijkstra
{
    int n = graph.size() - 1;
    vector<int> dis(n + 1, INT_MAX);
    vector<bool> vis(n + 1, false);
    priority_queue<pair<int, int>> pq; // 状态
    pq.push({0, s});
    dis[s] = 0;
    while (!pq.empty())
    {
        int u = pq.top().second;
        pq.pop();
        if (vis[u]) // 判重
            continue;
        vis[u] = true;
        for (Edge e : graph[u]) // 遍历
        {
            int v = e.v, w = e.w;
            if (dis[v] > dis[u] + w)
            {
                dis[v] = dis[u] + w;
                pq.push({dis[v], v});
            }
        }
    }
}
bool SPFA(vector<vector<Edge>> &graph) // SPFA(Shortest Path Faster Algorithm)单源最短路径Bellman-Ford
{
    int n = graph.size() - 1;
    queue<int> q; // 状态
    vector<int> dis(n + 1, INT_MAX);
    vector<bool> inQueue(n + 1, false);
    vector<int> cnt(n + 1, 0);
    q.push(1);
    dis[1] = 0;
    inQueue[1] = true;
    while (!q.empty())
    {
        int u = q.front();
        q.pop();
        inQueue[u] = false;
        for (Edge e : graph[u]) // 遍历
        {
            if (dis[e.v] > dis[u] + e.w)
            {
                dis[e.v] = dis[u] + e.w; // 松弛，更新入队节点数据信息
                if (!inQueue[e.v])       // 防止节点重复入队，判重
                {
                    q.push(e.v);
                    inQueue[e.v] = true;
                    if (++cnt[e.v] >= n)
                        return true;
                }
            }
        }
    }
    return false;
}
void Floyd_Warshall(vector<vector<int>> graph) // 多源最短路径、传递闭包、最小环
{
    int n = graph.size();
    for (int k = 0; k < n; k++)
    {
        for (int i = 0; i < n; i++)
        {
            if (!graph[i][k])
                continue;
            for (int j = 0; j < n; j++)
                graph[i][j] = graph[i][j] || (graph[i][k] && graph[k][j]);
        }
    }
    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < n; j++)
        {
            cout << graph[i][j] << ' ';
        }
        cout << endl;
    }
}
void FastPower(int a, int b, int p) // 快速幂
{
    int t = a, sum = 1, x = b;
    while (b)
    {
        if (b & 1)
        {
            sum *= t;
            sum %= p;
        }
        t *= t;
        t %= p;
        b >>= 1;
    }
    cout << a << '^' << x << " mod " << p << '=' << sum % p;
}
int gcd(int a, int b) // 最大公约数
{
    return b == 0 ? a : gcd(b, a % b);
}
int lcm(int a, int b) // 最小公倍数
{
    return a / gcd(a, b) * b;
}
int Binary(vector<int> nums, int lo, int hi, int target)
{
    int res;
    while (lo <= hi)
    {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] <= target)
        {
            lo = mid + 1;
            res = mid;
        }
        else
            hi = mid - 1;
    }
    return res;
}

// void show(vector<int> nums)
// {
//     for (int i = 1; i < nums.size(); i++)
//     {
//         cout << nums[i] << ' ';
//     }
//     cout << endl;
// }
// void test()
// {
//     vector<int> nums = {0, 4, 6, 2, 6, 2, 7, 1, 8};
//     int sum = 0;
//     for (int i = 1; i < nums.size(); i++)
//         sum += i;
//     Fenwick fw(nums.size());
//     fw.build(nums);
//     fw.update(3, 5);
//     fw.update(2, 2);
//     cout << sum << endl;
//     cout << fw.query(3) << endl;
//     cout << fw.rangeQuery(1, nums.size());
// }

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    test();
}