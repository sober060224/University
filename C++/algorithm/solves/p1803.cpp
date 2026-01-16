#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class contest
{
public:
    int a, b;
};

void solve()
{
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int n;
    cin >> n;
    vector<contest> x(n);
    for (int i = 0; i < n; i++)
        cin >> x[i].a >> x[i].b;

    sort(x.begin(), x.end(), [](contest a, contest b)
         { return a.b < b.b; });

    int sum = 1, end = x[0].b;
    for (int i = 1; i < n; i++)
    {
        if (x[i].a >= end)
        {
            sum++;
            end = x[i].b;
        }
    }
    cout << sum;

    return 0;
}