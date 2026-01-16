#include <cmath>
#include <iostream>
#include <vector>
#define N 1001
using namespace std;
bool isPrime(int sum)
{
    for (int i = 2; i <= sqrt(sum); i++)
    {
        if (sum % i == 0)
        {
            return false;
        }
    }
    return true;
}

void test()
{
    vector<int> res(N, 0);
    res = {0, 1, 4, 3, 2, 5, 6};
    int sum = 9;
    if (isPrime(sum))
    {
        cout << "ok";
    }
    else
    {
        cout << "not ok";
    }
}

void solve(vector<int> &res, vector<bool> &visited, vector<bool> Prime, int n, int index)
{
    // 当下标等于n时已经存了n个数
    if (index == n)
    {
        // 如果第一个和最后一个数的和是素数就输出，否则返回
        if (Prime[res[0] + res[n - 1]])
        {
            for (int i = 0; i < n; i++)
            {
                cout << res[i] << ' ';
            }
            cout << endl;
        }

        // 存了n个数后，无论最后一个和第一个数的和是不是素数都要返回，结束分支
        return;
    }
    for (int i = 2; i <= n; i++)
    {
        if (!visited[i] && Prime[i + res[index - 1]])
        {
            res[index] = i;
            visited[i] = true;
            solve(res, visited, Prime, n, index + 1);
            visited[i] = false;
        }
    }
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int n, t = 1;
    while(cin >> n)
    {
        vector<int> res(N);
        vector<bool> visited(N, false), Prime(50);
        res[0] = 1, visited[1] = true, Prime[1] = false;
        for (int i = 2; i <= 50; i++)
        {
            Prime[i] = isPrime(i);
        }
        cout << "Case " << t++ << ':' << endl;
        solve(res, visited, Prime, n, 1);
        cout << endl;
    }
    // test();

    return 0;
}