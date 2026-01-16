#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class rec
{
public:
    int i, j, e;
    rec(int a, int b, int c) : i(a), j(b), e(c) {}
};

class UnionFind
{
private:
    vector<int> fa, height;

public:
    UnionFind(int n)
    {
        fa.resize(2 * n, 0);
        for (int i = 1; i <= n; i++)
            fa[i] = i;
        height.resize(2 * n, 0);
    }

    bool Union(int x, int y)
    {
        int x_par = Find(x), y_par = Find(y);
        if (x_par == y_par)
            return false;

        if (height[x_par] <= height[y_par])
        {
            fa[x_par] = y_par;
            if (height[x_par] == height[y_par])
                height[y_par]++;
        }
        else
            fa[y_par] = x_par;

        return true;
    }

    int Find(int x)
    {
        if (x != fa[x])
            fa[x] = Find(fa[x]);
        return fa[x];
    }
};

void solve(vector<rec> nodes, vector<int> values)
{
    sort(values.begin(), values.end());
    values.erase(unique(values.begin(), values.end()), values.end());
    int size = values.size(), n = nodes.size();

    UnionFind uf(size);
    for (int i = 0; i < n; i++)
    {
        if (nodes[i].e == 1)
        {
            int pos1 = lower_bound(values.begin(), values.end(), nodes[i].i) - values.begin(), pos2 = lower_bound(values.begin(), values.end(), nodes[i].j) - values.begin();

            uf.Union(pos1, pos2);
        }
    }

    bool valid = true;
    for (int i = 0; i < n; i++)
    {
        if (nodes[i].e == 0)
        {
            int pos1 = lower_bound(values.begin(), values.end(), nodes[i].i) - values.begin(), pos2 = lower_bound(values.begin(), values.end(), nodes[i].j) - values.begin();

            if (uf.Find(pos1) == uf.Find(pos2))
            {
                valid = false;
                break;
            }
        }
    }

    if (valid)
        cout << "YES" << endl;
    else
        cout << "NO" << endl;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int t;
    cin >> t;
    while (t--)
    {
        int n;
        cin >> n;

        bool conflict = false;
        vector<rec> nodes;
        vector<int> values;

        for (int x = 0; x < n; x++)
        {
            int i, j, e;
            cin >> i >> j >> e;
            nodes.push_back(rec(i, j, e));
            values.push_back(i);
            values.push_back(j);
            // if (e)
            // {
            //     uf1.Union(i, j);
            //     if (uf2.Find(i) != i || uf2.Find(j) != j)
            //     {
            //         conflict = true;
            //     }
            // }
            // else
            // {
            //     uf2.Union(i, j);
            //     if (uf1.Find(i) != i || uf1.Find(j) != j)
            //     {
            //         conflict = true;
            //     }
            // }
        }

        solve(nodes, values);

        // if (conflict)
        //     cout << "NO" << endl;
        // else
        //     cout << "YES" << endl;
    }

    return 0;
}