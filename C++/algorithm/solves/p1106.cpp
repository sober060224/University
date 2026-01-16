#include <queue>
#include <iostream>
#include <vector>
#include <list>
#include <algorithm>
using namespace std;

void solve1(string n, int k)
{
    vector<int> a;

    int index = 0;

    // 高精度的正整数 n
    // 去掉其中任意 k 个数字后剩下的数字按原左右次序将组成一个新的非负整数
    // 使得剩下的数字组成的新数最小
    for (char c : n)
        a.push_back(int(c - '0'));

    deque<int> q;
    for (int i = 0; i < a.size(); i++)
    {
        while (!q.empty() && q.back() > a[i] && k)// 必须严格大于，相等的时候要保留
        {
            q.pop_back();
            k--;
        }
        q.push_back(a[i]);
    }

    while (k-- && q.size() > 1)
        q.pop_back();

    bool start = true;
    while (!q.empty())
    {
        if (start && !q.front() && q.size() > 1)
            q.pop_front();
        else
        {
            start = false;
            cout << q.front();
            q.pop_front();
        }
    }

    // while (k--)
    //     a.pop_back();

    // sort(a.begin(), a.end(), [](pair<int, int> a, pair<int, int> b)
    //      { return a.second < b.second; });

    // // 去掉若干数字后剩下的数可以存在前导零，而输出时不要输出前导零。
    // reverse(a.begin(), a.end());
    // while (!a.back().first && a.size() > 1)
    //     a.pop_back();

    // for (int i = a.size() - 1; i >= 0; i--)
    //     cout << a[i].first;

    // bool isZero = false, start = true;
    // for (int i = 1; i < a.size(); i++)
    // {
    //     if (!a[i].first && start && a.size() > 2)
    //     {
    //         isZero = true;
    //         continue;
    //     }
    //     else
    //     {
    //         start = false;
    //         cout << a[i].first;
    //     }
    // }
}

void solve2(vector<pair<int, int>> a, int k)// 错解
{
    sort(a.begin(), a.end(), [](pair<int, int> a, pair<int, int> b)
         { 
        if (a.first == b.first)
            return a.second > b.second;
        return a.first < b.first; });

    while (k--)
        a.pop_back();

    sort(a.begin(), a.end(), [](pair<int, int> a, pair<int, int> b)
         { return a.second < b.second; });

    // 去掉若干数字后剩下的数可以存在前导零，而输出时不要输出前导零。
    reverse(a.begin(), a.end());
    while (!a.back().first && a.size() > 1)
        a.pop_back();

    for (int i = a.size() - 1; i >= 0; i--)
        cout << a[i].first;
    cout << endl;
    // bool isZero = false, start = true;
    // for (int i = 1; i < a.size(); i++)
    // {
    //     if (!a[i].first && start && a.size() > 2)
    //     {
    //         isZero = true;
    //         continue;
    //     }
    //     else
    //     {
    //         start = false;
    //         cout << a[i].first;
    //     }
    // }
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    // int index = 0;
    // srand((unsigned int)time(nullptr));

    // for (int i = 0; i < 4; i++)
    // {
    //     vector<pair<int, int>> t;
    //     for (int i = 0; i < 5; i++)
    //         t.push_back({rand() % 10, index++});

    //     for (int i = 0; i < t.size(); i++)
    //         cout << t[i].first;
    //     cout << endl
    //          << k << endl;

    //     solve2(t, k);
    // }
    string n;
    int k;
    cin >> n >> k;
    solve1(n, k);

    return 0;
}