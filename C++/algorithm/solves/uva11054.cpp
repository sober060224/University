#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

void solve()
{

}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int n;
    while(true)
    {
        cin >> n;
        if (n == 0)
            break;
        vector<int> k(n);
        int sum = 0, t = 0;
        for (int i = 0; i < n; i++)
        {
            cin >> k[i];
            sum += abs(t);
            t += k[i];
        }
        cout << sum << endl;
    }

    return 0;
}