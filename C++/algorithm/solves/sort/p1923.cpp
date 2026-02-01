#include <vector>
#include <iostream>
using namespace std;

void solve(vector<int> &a, int lo, int hi, int mid)
{
    int i = lo, j = lo, k = mid + 1;
    vector<int> t = a;

    while (j <= mid && k <= hi)
    {
        if (t[j] <= t[k])
        {
            a[i] = t[j];
            j++;
        }
        else
        {
            a[i] = t[k];
            k++;
        }
        i++;
    }

    while (j <= mid)
    {
        a[i] = t[j];
        i++, j++;
    }
    while (k <= hi)
    {
        a[i] = t[k];
        i++, k++;
    }
}

void mergeSort(vector<int> &a, int lo, int hi)
{
    if (lo >= hi)
        return;

    int mid = (lo + hi) / 2;

    mergeSort(a, lo, mid);
    mergeSort(a, mid + 1, hi);
    solve(a, lo, hi, mid);
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, k;
    cin >> n >> k;
    vector<int> a(n);
    for (int i = 0; i < n; i++)
        cin >> a[i];

    mergeSort(a, 0, a.size() - 1);
    cout << a[k];
}