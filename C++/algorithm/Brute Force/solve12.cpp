#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;
const int N = 1001;
void test(vector<int> nodes, vector<vector<int>> graph)
{
    for (int i : nodes)
    {
        cout << i;
    }
    cout << endl;
    int index = 0;
    for (vector<int> nodes : graph)
    {
        cout << index++ << ':';
        for (int node : nodes)
        {
            cout << node;
        }
        cout << endl;
    }
}
void solve(vector<vector<int>> &graph, vector<int> &nodes, int index, vector<int> &current, vector<bool> &isUsed, int &minwidth, int cur_max, vector<int> &best, vector<int> &indexs)
{
    // 要判断所有情况的最小带宽值
    if (index == current.size())
    {
        minwidth = cur_max;
        // 就算找到了带宽值还要接着搜索是否存在更优的带宽值，current还是会变
        best = current;
        return;
    }

    // 选择
    for (int u : nodes)
    {
        if (isUsed[u])
            continue;
        int new_max = cur_max;

        // 检查当前点前面全部的邻接点
        for (int v : graph[u])
        {
            if (indexs[v] != -1)
            {
                new_max = max(new_max, index - indexs[v]);
            }
            // // 找前面邻接点的位置
            // for (int i = 0; i < index; i++)
            // {
            //     if (v == current[i])
            //     {
            //         new_max = max(new_max, index - i);
            //         break;
            //     }
            // }
        }

        // 新加入的点导致的带宽比之前带宽小才递归
        if (new_max < minwidth)
        {
            current[index] = u;
            indexs[u] = index;
            isUsed[u] = true;
            solve(graph, nodes, index + 1, current, isUsed, minwidth, new_max, best, indexs);

            // 回溯回来的时候要撤销选择并进行当前位置下一个数字的判断
            isUsed[u] = false;
            indexs[u] = -1;
        }
    }
}
int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    vector<vector<int>> graph(N);
    vector<int> nodes, best, indexs(8, -1);
    vector<bool> isUsed(N, false);
    int minwidth = INT_MAX;
    string s;

    // 读取数据
    while (cin >> s)
    {
        if (s == "#")
        {
            break;
        }
        for (int i = 0; i < s.size();)
        {
            int u = int(s[i] - 'A');
            if (!isUsed[u])
            {
                isUsed[u] = true;
                nodes.push_back(u);
            }
            i += 2;
            while (s[i] != ';' && i < s.size())
            {
                int v = int(s[i] - 'A');
                if (!isUsed[v])
                {
                    nodes.push_back(v);
                    isUsed[v] = true;
                }
                graph[u].push_back(v);
                graph[v].push_back(u);
                i++;
            }
            i++;
        }
    }
    sort(nodes.begin(), nodes.end());
    vector<int> current(nodes.size());
    fill(isUsed.begin(), isUsed.end(), false);
    solve(graph, nodes, 0, current, isUsed, minwidth, 0, best, indexs);

    for (int i : best)
    {
        cout << char(i + 'A') << ' ';
    }
    cout << "->" << minwidth;

    return 0;
}