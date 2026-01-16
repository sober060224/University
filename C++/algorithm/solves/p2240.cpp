#include <iomanip>
#include <iostream>
#include <vector>
#include <algorithm>
#define N 501
using namespace std;

class rec
{
public:
    int value = 0, index = 0;
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
    vector<rec> T(n + 1);
    int sum = 0;
    for (int i = 0; i < n; i++)
    {
        cin >> T[i].value;
        T[i].index = i + 1;
    }
    sort(T.begin(), T.end(), [](rec a, rec b)
         { return a.value < b.value; });
    
    int t = n;
    for (int i = 1; i < n; i++)
    {
        t--;
        int v = T[i].value;
        if (t > 0)
            sum = sum + v * t;
    }
    for (int i = 1; i <= n; i++)
    {
        cout << T[i].index << ' ';
    }
    cout << endl << fixed << setprecision(2) << double(sum) / n;

    return 0;
}