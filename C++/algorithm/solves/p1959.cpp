#include <queue>
#include <list>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve(int n, int w)
{
    priority_queue<int> maxHeap;
    priority_queue<int, vector<int>, greater<>> minHeap;
    vector<int> res;
    int sum = 0;
    for (int i = 0; i < n; i++)
    {
        int t;
        cin >> t;
        sum++;
        minHeap.push(t);
        int index = max(1, sum * w / 100);
        if ((!maxHeap.empty() && maxHeap.top() > minHeap.top()) || minHeap.size() > index)
        {
            maxHeap.push(minHeap.top());
            minHeap.pop();
        }
        if (minHeap.size() < index)
        {
            minHeap.push(maxHeap.top());
            maxHeap.pop();
        }
        res.push_back(minHeap.top());
    }
    for (int i : res)
        cout << i << ' ';

    // vector<int> a(1001, 0), res;
    // int size = 0;
    // for (int i = 0; i < n; i++)
    // {
    //     int t;
    //     cin >> t;
    //     size++, a[t]++;
    //     int index = max(1, size * w / 100), sum = 0;
    //     for (int i = 600; i >= 0; i--)
    //     {
    //         sum += a[i];
    //         if (sum >= index)
    //         {
    //             res.push_back(i);
    //             break;
    //         }
    //     }
    // }
    // for (int i : res)
    //     cout << i << ' ';
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, w;
    cin >> n >> w;
    
    solve(n, w);

    // vector<int> scores, lines;
    // cin >> a;
    // scores.push_back(a);
    // lines.push_back(a);
    // for (int i = 1; i < n; i++)
    // {
    //     int t;
    //     cin >> t;
    //     scores.push_back(t);
    //     sort(scores.begin(), scores.end());
    //     size++;
    //     int index = max(1, size * w / 100);
    //     lines.push_back(scores[size - index]);
    // }
    // for (int i = 0; i < n; i++)
    //     cout << lines[i] << ' ';

    return 0;
}