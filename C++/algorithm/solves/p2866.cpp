#include <stack>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(vector<int> a)
{
    int n = a.size() - 1, sum = 0;
    stack<int> s;

    for (int i = 1; i <= n; i++)
    {
        while (!s.empty() && a[i] >= a[s.top()])
            s.pop();
        sum += s.size();
        s.push(i);
    }

    // for (int i = n; i >= 1; i--)
    // {
    //     // while (!s.empty() && a[i] >= a[s.top()])
    //     // {
    //     //     sum += s.top() - i;
    //     //     s.pop();
    //     // }
    //     s.push(i);
    // }

    cout << sum;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int N;
    cin >> N;
    vector<int> a(N + 1);
    for (int i = 1; i <= N; i++)
        cin >> a[i];

    solve(a);

    return 0;
}