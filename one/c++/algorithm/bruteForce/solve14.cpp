#include <queue>
#include <iostream>
#include <vector>
#include <cstring>
#include <algorithm>
#include <map>
#define N 1001
using namespace std;

class Node
{
public:
    int s[9], d;
    Node() : d(0) {}

    // 添加比较运算符，使State对象可以作为unordered_map的键
    bool operator==(const Node &other) const
    {
        return memcmp(s, other.s, sizeof(s)) == 0;
    }
};

// 哈希函数用于State
class StateHash
{
public:
    size_t operator()(const Node &state) const
    {
        size_t hash = 0;
        for (int i = 0; i < 9; i++)
            hash = hash * 10 + state.s[i];
        return hash % 1000003;
    }
};

int solve(Node &start, int goal[])
{
    queue<Node> q;
    unordered_map<Node, int, StateHash> um; // 使用State作为键
    q.push(start);
    vector<int> dx = {0, 0, -1, 1};
    vector<int> dy = {1, -1, 0, 0};

    while (!q.empty())
    {
        Node state = q.front();
        q.pop();
        um[state] = 1;

        // 比较数组内容而不是整个state对象
        if (memcmp(state.s, goal, sizeof(state.s)) == 0)
            return state.d;

        int z;
        for (z = 0; z < 9; z++)
            if (!state.s[z])
                break;

        int y = z / 3, x = z % 3;
        for (int i = 0; i < 4; i++)
        {
            int ax = x + dx[i], ay = y + dy[i], az = ay * 3 + ax;
            if (ax < 0 || ax > 2 || ay < 0 || ay > 2)
                continue;

            Node t;
            memcpy(t.s, state.s, sizeof(state.s)); // 只复制数组，不包括d
            swap(t.s[z], t.s[az]);                 // 交换位置
            t.d = state.d + 1;

            if (um.find(t) != um.end())
                continue;

            um[t] = 1;
            q.push(t);
        }
    }

    return -1;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    string a;
    cin >> a;

    Node start;
    int goal[9] = {1, 2, 3, 8, 0, 4, 7, 6, 5}, index = 0;
    for (char c : a)
    {
        start.s[index++] = int(c - '0');
    }
    cout << solve(start, goal);
    // priority_queue<int> pq;
    // pq.push(4);
    // pq.push(1);
    // pq.push(9);
    // while (!pq.empty())
    // {
    //     cout << pq.top();
    //     pq.pop();
    // }

    return 0;
}