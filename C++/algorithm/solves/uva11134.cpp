#include <cstring>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class Line
{
public:
    int s, e, index;
    bool vis = false;
};

vector<int> solve(vector<Line> &a)
{
    int n = a.size() - 1;
    vector<int> ans(n + 1);

    for (int i = 1; i <= n; i++)
    {
        bool isfind = false;
        // 找比当前要找位置小于等于开始的点，没访问过
        for (int j = 1; j <= n; j++)
        {
            if (a[j].s <= i && !a[j].vis && a[j].e >= i)
            {
                // 找到了j，将这个点的位置信息存下来
                ans[a[j].index] = i;
                // j = a[j].s;
                a[j].vis = true;
                isfind = true;
                break;
            }
        }
        if (!isfind)
            return vector<int>(1, -1);
    }

    return ans;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int n;
    while (true)
    {
        cin >> n;
        if (n == 0)
            break;

        vector<Line> x(n + 1), y(n + 1);
        for (int i = 1; i <= n; i++)
        {
            cin >> x[i].s >> y[i].s >> x[i].e >> y[i].e;
            x[i].index = y[i].index = i;
        }

        sort(x.begin(), x.end(), [](Line a, Line b)
             { return a.e < b.e; });
        sort(y.begin(), y.end(), [](Line a, Line b)
             { return a.e < b.e; });

        vector<int> ax = solve(x), ay = solve(y);
        
        if (ax == vector<int>(1, -1) || ay == vector<int>(1, -1))
            cout << "IMPOSSIBLE" << endl;
        else
            for (int i = 1; i <= n; i++)
                cout << ax[i] << ' ' << ay[i] << endl;
    }

    return 0;
}