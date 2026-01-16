#include <queue>
#include <iostream>
#include <vector>
#include <algorithm>
#define N 150
using namespace std;

// a,b,c每个都是0-255的数字，用位运算合并
inline int ID(int &a, int &b, int &c)
{
  return (a << 16) | (b << 8) | c;
}

// 没有一个以上的鬼在同一个格子里；
// 没有一对鬼在一步里交换了位置
inline bool conflict(int &a, int &b, int &a2, int &b2)
{
  return (a == b2 && b == a2) || (a2 == b2);
}

int solve(vector<vector<int>> &G, vector<int> &s, vector<int> &g)
{
  queue<int> q;
  vector<vector<vector<int>>> dis(N, vector<vector<int>>(N, vector<int>(N, -1)));
  q.push(ID(s[0], s[1], s[2]));
  dis[s[0]][s[1]][s[2]] = 0;

  while (!q.empty())
  {
    int t = q.front();
    q.pop();
    int a = (t >> 16) & 0xff, b = (t >> 8) & 0xff, c = t & 0xff;
    if (a == g[0] && b == g[1] && c == g[2])
      return dis[a][b][c];

    // 图遍历
    for (int a2 : G[a])
    {
      for (int b2 : G[b])
      {
        if (conflict(a, b, a2, b2))
          continue;
        for (int c2 : G[c])
        {
          // 图判重
          if (conflict(a, c, a2, c2) || conflict(b, c, b2, c2) || dis[a2][b2][c2] != -1)
            continue;
          dis[a2][b2][c2] = dis[a][b][c] + 1;
          q.push(ID(a2, b2, c2));
        }
      }
    }
  }
  return 0;
}

int main()
{
  ios::sync_with_stdio(false);
  cin.tie(0);
  cout.tie(0);

  int w, h, n;
  while (true)
  {
    cin >> w >> h >> n;
    if (w == 0)
      break;

    vector<string> maze;
    string line;
    getline(cin, line);
    
    for (int i = 0; i < h; i++)
    {
      getline(cin, line);
      maze.push_back(line);
    }
      
    int cnt = 0;
    vector<vector<int>> id(20, vector<int>(20)), G(N);
    vector<int> start(3), goal(3), x(N), y(N), dx = {-1, 1, 0, 0, 0}, dy = {0, 0, -1, 1, 0};
    
    for (int i = 0; i < h; i++)
    {
      for (int j = 0; j < w; j++)
      {
        if (maze[i][j] != '#')
        {
          x[cnt] = i, y[cnt] = j, id[i][j] = cnt;
          if (islower(maze[i][j]))
            start[maze[i][j] - 'a'] = cnt;
          else if (isupper(maze[i][j]))
            goal[maze[i][j] - 'A'] = cnt;
          cnt++;
        }
      }
    }
    for (int i = 0; i < cnt; i++)
    {
      for (int j = 0; j < 5; j++)
      {
        int ax = x[i] + dx[j], ay = y[i] + dy[j];
        if (maze[ax][ay] != '#')
          G[i].push_back(id[ax][ay]);
      }
    }

    if (n <= 2)
    {
      G[cnt].push_back(cnt);
      start[2] = goal[2] = cnt++;
    }
    if (n <= 1)
    {
      G[cnt].push_back(cnt);
      start[1] = goal[1] = cnt++;
    }

    cout << solve(G, start, goal) << endl;
  }

  return 0;
}