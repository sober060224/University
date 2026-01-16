#include <climits>
#include <iostream>
#include <vector>
#include <queue>
#include <algorithm>
using namespace std;

// // DFS BFS深、广搜
// void DFS(vector<vector<int>> &articles, int index, vector<bool> &visited)
// {
//     cout << index << ' ';
//     visited[index] = true;
//     for (int neighbor : articles[index])
//         if (!visited[neighbor])
//             DFS(articles, neighbor, visited);
// }
// void BFS(vector<vector<int>> &articles, vector<bool> &visited)
// {
//     queue<int> q;
//     q.push(1);
//     visited[1] = true;
//     while (!q.empty())
//     {
//         int t = q.front();
//         q.pop();
//         cout << t << ' ';
//         for (int neighbor : articles[t])
//         {
//             if (!visited[neighbor])
//             {
//                 visited[neighbor] = true;
//                 q.push(neighbor);
//             }
//         }
//     }
// }
// void test()
// {
//     int n, m;
//     cin >> n >> m;
//     vector<vector<int>> articles(n + 1);
//     for (int i = 0; i < m; i++)
//     {
//         int X, Y;
//         cin >> X >> Y;
//         articles[X].push_back(Y);
//     }
//     for (int i = 1; i <= n; i++)
//     {
//         sort(articles[i].begin(), articles[i].end());
//     }
//     vector<bool> visited(n + 1, false);
//     DFS(articles, 1, visited);
//     cout << endl;
//     fill(visited.begin(), visited.end(), false);
//     BFS(articles, visited);
// }

// // Topological sorting拓扑排序
// int TopologicalSorting(vector<vector<int>> &graph, int index, vector<int> &mem)
// {
//     if (graph[index].empty())
//         return 1;
//     if (mem[index] != 0)
//         return mem[index];
//     long long sum = 0;
//     for (int neighbor : graph[index])
//         sum += TopologicalSorting(graph, neighbor, mem);
//     return mem[index] = sum % 80112002;
// }
// void test()
// {
//     ios::sync_with_stdio(false);
//     cin.tie(0);
//     cout.tie(0);
//     int n, m;
//     cin >> n >> m;
//     vector<vector<int>> graph(n + 1);
//     vector<int> indegree(n + 1, 0);
//     for (int i = 0; i < m; i++)
//     {
//         int A, B;
//         cin >> A >> B;
//         graph[B].push_back(A);
//         indegree[A]++;
//     }
//     long long sum = 0;
//     vector<int> mem(n + 1, 0);
//     for (int i = 1; i <= n; i++)
//     {
//         if (indegree[i] == 0)
//         {
//             sum = (sum + TopologicalSorting(graph, i, mem)) % 80112002;
//         }
//     }
//     cout << sum % 80112002;
// }

// // Strongly Connected Component强连通分量
// void DFS1(vector<vector<int>> &graph, int index, vector<int> &visited, vector<int> &topo)
// {
//     visited[index] = 1;
//     for (int &v : graph[index])
//         if (visited[v] == 0)
//             DFS1(graph, v, visited, topo);
//     visited[index] = 2;
//     topo.push_back(index);
// }
// void DFS2(vector<vector<int>> &reverse_graph, int index, vector<int> &visited, vector<int> &component)
// {
//     visited[index] = 1;
//     component.push_back(index);
//     for (int &v : reverse_graph[index])
//         if (visited[v] == 0)
//             DFS2(reverse_graph, v, visited, component);
//     visited[index] = 2;
// }
// void StronglyComponent(vector<vector<int>> &graph, vector<vector<int>> &reverse_graph)
// {
//     int n = graph.size() - 1;      // n
//     vector<int> visited(n + 1, 0); // 0未访问 1正在访问 2完成访问
//     vector<int> topo;
//     for (int i = 1; i <= n; i++)
//         if (visited[i] == 0)
//             DFS1(graph, i, visited, topo);
//     reverse(topo.begin(), topo.end()); // 得到原图拓扑序
//     visited.assign(n + 1, 0);
//     vector<int> component;
//     int sum = 0;
//     for (int &i : topo)
//     {
//         if (visited[i] == 0)
//         {
//             component.clear();
//             DFS2(reverse_graph, i, visited, component);
//             if (component.size() > 1)
//                 sum++;
//         }
//     }
//     cout << sum;
// }
// void test()
// {
//     ios::sync_with_stdio(false);
//     cin.tie(0);
//     cout.tie(0);
//     int n, m;
//     cin >> n >> m;
//     vector<vector<int>> graph(n + 1); // 0~n
//     vector<vector<int>> reverse_graph(n + 1);
//     for (int i = 0; i < m; i++)
//     {
//         int a, b;
//         cin >> a >> b;
//         // 构造逆图
//         graph[a].push_back(b);
//         reverse_graph[b].push_back(a);
//     }
//     StronglyComponent(graph, reverse_graph);
// }

// // Minimum Spanning Tree最小生成树
// class Edge
// {
// public:
//     int u, v, w;
//     Edge(int u, int v, int w) : u(u), v(v), w(w) {}
// };
// class UnionFind
// {
// private:
//     vector<int> parent;
//     vector<int> rank;
// public:
//     UnionFind(int n)
//     {
//         parent.resize(n + 1);
//         rank.resize(n + 1, 0);
//         for (int i = 1; i <= n; i++)
//             parent[i] = i;
//     }
//     int Find(int a)
//     {
//         if (parent[a] != a)
//             parent[a] = Find(parent[a]);
//         return parent[a];
//     }
//     void Union(int x, int y)
//     {
//         int x_parent = Find(x), y_parent = Find(y);
//         if (rank[x_parent] >= rank[y_parent])
//         {
//             if (rank[x_parent] == rank[y_parent])
//                 rank[x_parent]++;
//             parent[y_parent] = x_parent;
//         }
//         else
//             parent[x_parent] = y_parent;
//     }
// };
// void MinimumTree(vector<Edge> &graph, int n)
// {
//     sort(graph.begin(), graph.end(), [](Edge &a, Edge &b)
//          { return a.w < b.w; });
//     // 并查集记录森林，边的点在森林内不添加，直到成为一棵树
//     UnionFind uf(n);
//     int sum = 0, edge = 0;
//     for (Edge &e : graph)
//     {
//         int u = e.u, v = e.v, w = e.w;
//         if (uf.Find(u) != uf.Find(v) && edge < n - 1)
//         {
//             uf.Union(u, v);
//             sum += w;
//             edge++;
//         }
//     }
//     if (edge == n - 1)
//         cout << sum;
//     else
//         cout << "orz";
// }
// void test()
// {
//     int N, M;
//     cin >> N >> M;
//     vector<Edge> graph(M);
//     for (int i = 0; i < M; i++)
//     {
//         int X, Y, Z;
//         cin >> X >> Y >> Z;
//         graph.push_back(Edge(X, Y, Z));
//     }
//     MinimumTree(graph, N);
// }

// // Single-source Shortest Path单源最短路径Dijkstra
// class Edge// 状态
// {
// public:
//     int v, w;
//     Edge(int v, int w) : v(v), w(w) {}
//     bool operator<(Edge &other)
//     {
//         return w > other.w;
//     }
// };
// void ShortestPath_Dijkstra(vector<vector<Edge>> &Graph, int n, int s)
// {
//     vector<int> dis(n + 1, INT_MAX);
//     priority_queue<Edge> pq;
//     dis[s] = 0;// 最优答案
//     pq.push(Edge(s, 0));
//     while (!pq.empty())
//     {
//         Edge e = pq.top();
//         pq.pop();
//         if (dis[e.v] != e.w) // 判重
//             continue;
//         for (Edge &edge : Graph[e.v])// 遍历
//         {
//             int v = edge.v;
//             int w = edge.w;
//             if (dis[v] > dis[e.v] + w)// 松弛
//             {
//                 dis[v] = dis[e.v] + w;
//                 pq.push(Edge(v, dis[v]));
//             }
//         }
//     }
//     for (int i = 1; i <= n; i++)
//         cout << dis[i] << ' ';
// }
// void test()
// {
//     int n, m, s;
//     cin >> n >> m >> s;
//     vector<vector<Edge>> Graph(n + 1);
//     for (int i = 0; i < m; i++)
//     {
//         int u, v, w;
//         cin >> u >> v >> w;
//         Graph[u].push_back(Edge(v, w));
//     }
//     ShortestPath_Dijkstra(Graph, n, s);
// }

// // SPFA(Shortest Path Faster Algorithm)单源最短路径Bellman-Ford
// class Edge
// {
// public:
//     int v, w;
//     Edge(int v, int w) : v(v), w(w) {}
// };
// bool SPFA(vector<vector<Edge>> &graph)
// {
//     int n = graph.size() - 1;
//     queue<int> q;// 状态
//     vector<int> dis(n + 1, INT_MAX);
//     vector<bool> inQueue(n + 1, false);
//     vector<int> cnt(n + 1, 0);
//     q.push(1);
//     dis[1] = 0;
//     inQueue[1] = true;
//     while(!q.empty())
//     {
//         int u = q.front();
//         q.pop();
//         inQueue[u] = false;
//         for (Edge e : graph[u])// 遍历
//         {
//             if (dis[e.v] > dis[u] + e.w)
//             {
//                 dis[e.v] = dis[u] + e.w;// 松弛，更新入队节点数据信息
//                 if (!inQueue[e.v])// 防止节点重复入队，判重
//                 {
//                     q.push(e.v);
//                     inQueue[e.v] = true;
//                     if (++cnt[e.v] >= n)
//                         return true;
//                 }
//             }
//         }
//     }
//     return false;
// }
// void test()
// {
//     int T;
//     cin >> T;
//     while (T--)
//     {
//         int n, m;
//         cin >> n >> m;
//         vector<vector<Edge>> graph(n + 1);
//         for (int i = 0; i < m; i++)
//         {
//             int u, v, w;
//             cin >> u >> v >> w;
//             if (w >= 0)
//             {
//                 graph[u].push_back(Edge(v, w));
//                 graph[v].push_back(Edge(u, w));
//             }
//             else
//                 graph[u].push_back(Edge(v, w));
//         }
//         if(SPFA(graph))
//             cout << "YES" << endl;
//         else
//             cout << "NO" << endl;
//     }
// }

// // Transitive Closure传递闭包(Floyd-Warshall)
// void Floyd_Warshall(vector<vector<int>> &a)
// {
//     int n = a.size();
//     for (int k = 0; k < n; k++)
//     {
//         for (int i = 0; i < n; i++)
//         {
//             if (a[i][k] == 0)
//                 continue;
//             for (int j = 0; j < n; j++)
//             {
//                 if (a[k][j] == 0)
//                     continue;
//                 a[i][j] = a[i][j] || (a[i][k] && a[k][j]);
//             }
//         }
//     }
//     for (int i = 0; i < n; i++)
//     {
//         for (int j = 0; j < n; j++)
//         {
//             cout << a[i][j] << ' ';
//         }
//         cout << endl;
//     }
// }
// void test()
// {
//     int n;
//     cin >> n;
//     vector<vector<int>> a(n, vector<int>(n));
//     for (int i = 0; i < n; i++)
//     {
//         for (int j = 0; j < n; j++)
//         {
//             int x;
//             cin >> x;
//             a[i][j] = x;
//         }
//     }
//     Floyd_Warshall(a);
// }

// // Maximum Binary Matching最大二分匹配
// bool MaximumMatching(vector<vector<int>> &Graph, int u, vector<bool> &visited, vector<int> &match)
// {
//     for (int &v : Graph[u])
//     {
//         if (!visited[v])
//         {
//             visited[v] = true;
//             if (match[v] == 0 || MaximumMatching(Graph, v, visited, match))
//             {
//                 match[v] = u;
//                 return true;
//             }
//         }
//     }
//     return false;
// }

// Maximum Flow(Network Flow)最大流Edmonds-Karp
bool BFS(vector<vector<int>> &Graph, vector<int> &path, int s, int t)
{
    fill(path.begin(), path.end(), -1);
    int n = Graph.size() - 1;
    queue<int> q;
    q.push(s);
    path[s] = -2;
    while (!q.empty())
    {
        int u = q.front();
        q.pop();
        for (int i = 1; i <= n; i++)
        {
            if (path[i] == -1 && Graph[u][i] > 0)
            {
                path[i] = u;
                q.push(i);
                if (i == t)
                    return true;
            }
        }
    }
    return false;
}
void MaximumFlow(vector<vector<int>> &Graph, int s, int t)
{
    int n = Graph.size() - 1;
    vector<int> path(n + 1);
    int sum = 0;
    while (BFS(Graph, path, s, t))
    {
        int u = t, minflow = INT_MAX, v = t;
        while (v != s)
        {
            u = path[u];
            minflow = min(minflow, Graph[u][v]);
            v = u;
        }
        u = t, v = t;
        sum += minflow;
        while (v != s)
        {
            u = path[u];
            Graph[u][v] -= minflow;
            Graph[v][u] += minflow;
            v = u;
        }
    }
    cout << sum;
}
void test()
{
    int n, m, s, t, u, v, w;
    cin >> n >> m >> s >> t;
    vector<vector<int>> Graph(n + 1, vector<int>(n + 1, 0));
    for (int i = 0; i < m; i++)
    {
        cin >> u >> v >> w;
        Graph[u][v] += w;
    }
    MaximumFlow(Graph, s, t);
}

// void test()
// {
// }

int main()
{
    // test();
    return 0;
}