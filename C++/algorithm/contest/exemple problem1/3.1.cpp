#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

class juzhen
{
public:
    vector<vector<int>> data;
    juzhen(int a, int b)
    {
        data.resize(a, vector<int>(b));
    }
};

juzhen solve(juzhen a, juzhen b)
{
    int p1 = a.data.size(), p2 = a.data[0].size(), p3 = b.data[0].size();
    juzhen c(p1, p3);

    // 叉乘
    for (int i = 0; i < p1; i++)
        for (int j = 0; j < p3; j++)
            for (int k = 0; k < p2; k++)
                c.data[i][j] += a.data[i][k] * b.data[k][j];

    return c;
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, d;
    cin >> n >> d;
    vector<juzhen> juzhens;

    for (int k = 0; k < 3; k++)
    {
        juzhen t(n, d);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < d; j++)
            {
                int a;
                cin >> a;
                t.data[i][j] = a;
            }
        }
        juzhens.push_back(t);
    }
    vector<int> w(n);
    for (int i = 0; i < n; i++)
        cin >> w[i];

    // 转置，叉乘
    juzhen &k = juzhens[1];
    juzhen t(d, n);
    for (int i = 0; i < d; i++)
        for (int j = 0; j < n; j++)
            t.data[i][j] = k.data[j][i];
    k = t;

    juzhen res = solve(juzhens[1], juzhens[2]);
    for (int i = 0; i < n; i++)
    {
        int t = w[i];
        for (int j = 0; j < n; j++)
            juzhens[0].data[i][j] *= t;
    }

    res = solve(juzhens[0], res);
    
    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < d; j++)
        {
            cout << res.data[i][j] << ' ';
        }
        cout << endl;
    }
    // p1 = 10^4, p2 = 20, p3 = 10^4; p1 = 10^4, p2 = 10^4, p3 = 20
}