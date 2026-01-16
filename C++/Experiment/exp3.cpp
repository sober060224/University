#include <climits>
#include <queue>
#include <iostream>
#include <vector>
using namespace std;
class Point
{
public:
    int xi, yi, ci;
    Point(int x, int y, int c) : xi(x), yi(y), ci(c) {}
    Point(int x, int y) : xi(x), yi(y) {}
    Point() {}
};
int main()
{
    int n, m, k, d;
    cin >> n >> m >> k >> d;
    vector<vector<int>> graph(n + 1, vector<int>(n + 1, 0));// -1不能访问
    vector<vector<int>> dis(n + 1, vector<int>(n + 1, INT_MIN));
    queue<Point> q;
    vector<int> dx = {0, 0, -1, 1}, dy = {1, -1, 0, 0};
    for (int i = 0; i < m; i++)
    {
        int xi, yi;
        cin >> xi >> yi;
        q.push(Point(xi, yi));
        dis[xi][yi] = 0;
        graph[xi][yi] = -1;
    }
    vector<Point> customs;
    for (int i = 0; i < k; i++)
    {
        int xi, yi, ci;
        cin >> xi >> yi >> ci;
        customs.push_back(Point(xi, yi, ci));
    }
    for (int i = 0; i < d; i++)
    {
        int x, y;
        cin >> x >> y;
        graph[x][y] = -1;
    }
    while (!q.empty())
    {
        Point u = q.front();
        q.pop();
        for (int i = 0; i < 4; i++)
        {
            int x = u.xi + dx[i], y = u.yi + dy[i];
            if (x > 0 && x <= n && y > 0 && y <= n && graph[x][y] != -1)
            {
                graph[x][y] = -1;
                dis[x][y] = dis[u.xi][u.yi] + 1;
                q.push(Point(x, y));
            }
        }
        // if (x + 1 <= n && graph[x + 1][y] != -1)
        // {
        //     graph[x + 1][y] = -1;
        //     dis[x + 1][y] = dis[x][y] + 1;
        //     q.push(Point(x + 1, y));
        // }
        // if (y + 1 <= n && graph[x][y + 1] != -1)
        // {
        //     graph[x][y + 1] = -1;
        //     dis[x][y + 1] = dis[x][y] + 1;
        //     q.push(Point(x, y + 1));
        // }
        // if (x - 1 > 0 && graph[x - 1][y] != -1)
        // {
        //     graph[x - 1][y] = -1;
        //     dis[x - 1][y] = dis[x][y] + 1;
        //     q.push(Point(x - 1, y));
        // }
        // if (y - 1 > 0 && graph[x][y - 1] != -1)
        // {
        //     graph[x][y - 1] = -1;
        //     dis[x][y - 1] = dis[x][y] + 1;
        //     q.push(Point(x, y - 1));
        // }
    }
    int sum = 0;
    for (Point &custom : customs)
    {
        int x = custom.xi, y = custom.yi, c = custom.ci;
        sum += dis[x][y] * c;
    }
    cout << sum;
    // for (int i = 1; i <= n; i++)
    // {
    //     for (int j = 1; j <= n; j++)
    //     {
    //         cout << dis[i][j] << ' ';
    //     }
    //     cout << endl;
    // }
}   