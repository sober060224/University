#include <iostream>
#include <vector>
#define N 100
using namespace std;
void Recursion(int cur, int n, vector<vector<int>> &solutions, vector<int> &queens, vector<vector<bool>> &visited)
{
    if (cur == n + 1)
    {
        solutions.push_back(queens);
        return;
    }
    else
    {
        for (int i = 1; i <= n; i++)
        {
            if (!visited[0][i] && !visited[1][cur + i] && !visited[2][cur - i + n])
            {
                queens[cur] = i;
                visited[0][i] = visited[1][cur + i] = visited[2][cur - i + n] = true;
                Recursion(cur + 1, n, solutions, queens, visited);
                visited[0][i] = visited[1][cur + i] = visited[2][cur - i + n] = false;
            }
        }
    }
}
// y1-y2=-x1+x2
void solve()
{
    int n, total = 0;
    cin >> n;
    vector<vector<int>> solutions;
    vector<vector<bool>> visited(3, vector<bool>(N, false));
    vector<int> queens(N);
    Recursion(1, n, solutions, queens, visited);
    for (int i = 0; i < 3; i++)
    {
        vector<int> solution = solutions[i];
        for (int j = 1; j <= n; j++)
        {
            cout << solution[j] << ' ';
        }
        cout << endl;
    }
    cout << solutions.size();
}
int main()
{
    solve();
}