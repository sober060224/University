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
    Fenwick(int x)
    {
        tree.resize(x + 1);
        n = x;
    }
    int lowbit(int n)
    {
        return n & (-n);
    }
    void update(int index, int x)
    {
        for (int i = index; i <= n; i += lowbit(i))
            tree[i] += x;
    }
    int query(int index)
    {
        int sum = 0;
        for (int i = index; i > 0; i -= lowbit(i))
            sum += tree[i];
        return sum;
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

void solve()
{
    // vector<int> a = {1, 3, 4, 2, 5};
    // Fenwick fw(a.size());
    // fw.build(a);
    // cout << fw.query(3);
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    // solve();
    int n, m;
    cin >> n >> m;
    vector<int> a(n + 1, 0);

    while (m--)
    {
        int f;
        cin >> f;

        // 区间修改，区间查询
        if (!f)
        {
            // 实时修改，要知道每次修改之后哪个是奇偶
            int s, t, o, y;
            cin >> s >> t >> o >> y;
            for (int i = s; i <= t; i++)
            {
                if (a[i] % 2 == o)
                    a[i] += y;
            }
        }
        else
        {
            int s, t;
            cin >> s >> t;
            // 查询数字和
            int sum = 0;
            for (int i = s; i <= t; i++)
                sum += a[i];
            cout << sum << endl;
        }
    }
}