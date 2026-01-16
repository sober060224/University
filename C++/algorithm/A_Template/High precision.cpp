#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class p
{
public:
    int l, r;
};

void show(vector<int> a)
{
    for (int i = a.size() - 1; i >= 0; i--)
        cout << a[i];
    cout << endl;
}

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

void subOne(vector<int> &a)
{
    if (a[0] > 0)
        a[0] -= 1;
    else
    {
        int i = 0;
        for (; a[i] < 1; i++)
            a[i] = 9;
        a[i] -= 1;
    }
    removeZero(a);
}

vector<int> factorial(int n)
{
    vector<int> res(1, 1);

    for (int i = 1; i <= n; i++)
    {
        vector<int> t = intToVector(i);
        res = multi(res, t);
    }
    // int n, sum = 1;
    // for (int i = 1; i <= n ; i++)
    //     sum *= n;

    return res;
}

void test()
{
    int n;
    cin >> n;
    vector<int> res = factorial(n);
    show(res);
    cout << res.size();

    // vector<int> N, M;
    // string a, b;
    // cin >> a >> b;
    // for (int i = 0; i < a.size(); i++)
    //     N.push_back(int(a[i] - '0'));
    // reverse(N.begin(), N.end());
    // for (int i = 0; i < b.size(); i++)
    //     M.push_back(int(b[i] - '0'));
    // reverse(M.begin(), M.end());
    // vector<int> res = multi(N, M);
    // show(res);
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    test();
    // int n, a, b;
    // cin >> n;
    // cin >> a >> b;
    // vector<p> ps(n);
    // for (int i = 0; i < n; i++)
    // {
    //     cin >> ps[i].l >> ps[i].r;
    // }

    // // 排在该大臣前面的所有人的左手上的数的乘积除以他自己右手上的数，然后向下取整得到的结果。
    // // 获得奖赏最多的大臣，所获奖赏尽可能的少
    // sort(ps.begin(), ps.end(), [](p &a, p &b)
    //      { return max(b.r, a.l * a.r) < max(b.l * b.r, a.r); });

    // // int a = 1;
    // vector<int> sum = intToVector(a), ans(1, 0);
    // for (int i = 0; i < n; i++)
    // {
    //     vector<int> right = intToVector(ps[i].r);
    //     ans = MAX(ans, divide(sum, right));
    //     vector<int> left = intToVector(ps[i].l);
    //     sum = multi(sum, left);
    // }

    // for (int i = ans.size() - 1; i >= 0; i--)
    // {
    //     cout << ans[i];
    // }
    // // int sum = a, ans = 0;
    // // for (int i = 0; i < n; i++)
    // // {
    // //     ans = max(ans, sum / ps[i].r);
    // //     sum *= ps[i].l;
    // // }
    // // cout << ans;

    return 0;
}