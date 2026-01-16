#include <climits>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class stu
{
public:
    int x, y, z;
};

void solve(vector<int> a, vector<stu> stues)
{
    

    // int n = a.size();
    // vector<int> diff(n + 1, 0);
    // for (int i = 1; i <= n; i++)
    // {
    //     diff[i] += a[i - 1];
    //     if (i + 1 <= n)
    //         diff[i + 1] -= a[i - 1];
    // }

    // for (stu s : stues)
    // {
    //     diff[s.x] += s.z;
    //     if (s.y + 1 <= n)
    //         diff[s.y + 1] -= s.z;
    // }

    // a[0] = diff[1];
    // int res = INT_MAX;
    // for (int i = 1; i < a.size(); i++)
    // {
    //     a[i] = diff[i + 1] + a[i - 1];
    //     res = min(a[i], res);
    // }

    // cout << res;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, p;
    cin >> n >> p;
    vector<int> a(n + 2);
    for (int i = 1; i <= n; i++)
        cin >> a[i];
    vector<stu> stues(p);
    for (int i = 0; i < p; i++)
        cin >> stues[i].x >> stues[i].y >> stues[i].z;

    solve(a, stues);

    return 0;
}