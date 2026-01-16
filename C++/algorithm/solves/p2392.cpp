#include <queue>
#include <iostream>
#include <climits>
#include <vector>
#define N 1001
using namespace std;
void solve(vector<int> &s, vector<vector<int>> &subjects, int index, int i, int left, int right, int &mintime)
{
    int maxValue = max(left, right);
    if (maxValue > mintime)
    {
        return;
    }
    if (index == s[i])
    {
        mintime = min(mintime, maxValue);
        return;
    }

    solve(s, subjects, index + 1, i, left + subjects[i][index], right, mintime);
    solve(s, subjects, index + 1, i, left, right + subjects[i][index], mintime);
}
int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    vector<int> s(4);
    for (int i = 0; i < 4; i++)
    {
        cin >> s[i];
    }
    vector<vector<int>> subjects(4);
    for (int i = 0; i < 4; i++)
    {
        for (int j = 0; j < s[i]; j++)
        {
            int t;
            cin >> t;
            subjects[i].push_back(t);
        }
    }
    int sum = 0;
    for (int i = 0; i < 4; i++)
    {
        int mintime = INT_MAX;
        solve(s, subjects, 0, i, 0, 0, mintime);
        sum += mintime;
    }
    cout << sum;

    return 0;
}