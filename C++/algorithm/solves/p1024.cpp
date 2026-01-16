#include <iomanip>
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

class func
{
public:
    double a, b, c, d;

    double function(double x)
    {
        return a * x * x * x + b * x * x + c * x + d;
    }

    func(double a, double b, double c, double d) : a(a), b(b), c(c), d(d) {}
};

void solve(double a, double b, double c, double d)
{
    func f(a, b, c, d);
    vector<double> res;

    for (double i = -100; i <= 100; i++)
    {
        double j = i + 1, leftValue = f.function(i), rightValue = f.function(j);

        if (!leftValue)
            res.push_back(i);

        if (leftValue * rightValue < 0)
        {
            while (j - i >= 0.001)
            {
                double mid = i + (j - i) / 2;

                if (f.function(mid) * f.function(i) <= 0)
                    j = mid;
                else
                    i = mid;
            }
            res.push_back(j);
        }

        if (res.size() == 3)
            break;
    }

    for (double i : res)
        cout << fixed << setprecision(2) << i << ' ';
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    double a, b, c, d;
    cin >> a >> b >> c >> d;

    solve(a, b, c, d);

    return 0;
}