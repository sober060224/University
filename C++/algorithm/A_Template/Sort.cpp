#include <algorithm>
#include <iostream>
#include <vector>
using namespace std;

void show(vector<int> a)
{
    for (int i : a)
        cout << i << ' ';
    cout << endl;
}

void quickSort_3way(vector<int> &a, int lo, int hi)
{
    if (lo >= hi)
        return;

    // pivot
    int rand_idx = lo + rand() % (hi - lo + 1);
    swap(a[lo], a[rand_idx]);

    int pivot = a[lo];
    int lt = lo;    // a[lo..lt-1] < pivot
    int gt = hi;    // a[gt+1..hi] > pivot
    int i = lo + 1; // a[lt..i-1] == pivot

    while (i <= gt)
    {
        if (a[i] < pivot)
            swap(a[lt++], a[i++]);
        else if (a[i] > pivot)
            swap(a[i], a[gt--]);
        else
            i++;
    }

    quickSort_3way(a, lo, lt - 1);
    quickSort_3way(a, gt + 1, hi);
}

void down(vector<int> &a, int n, int i)
{
    while (true)
    {
        int l = 2 * i + 1;
        int r = 2 * i + 2;
        int ma = i;
        if (l < n && a[l] > a[ma])
            ma = l;
        if (r < n && a[r] > a[ma])
            ma = r;
        if (ma == i)
            break;
        swap(a[ma], a[i]);
        i = ma;
    }
}
void heapSort(vector<int> &a)
{
    for (int i = a.size() / 2 - 1; i >= 0; i--)
        down(a, a.size(), i);
    for (int i = a.size() - 1; i > 0; i--)
    {
        swap(a[0], a[i]);
        down(a, i, 0);
    }
}

void merge(vector<int> &nums, int lo, int mid, int hi)
{
    vector<int> t(hi - lo + 1);
    int i = lo, j = mid + 1, k = 0;
    while (i <= mid && j <= hi)
    {
        if (nums[i] <= nums[j])
            t[k++] = nums[i++];
        else
            t[k++] = nums[j++];
    }
    while (i <= mid)
        t[k++] = nums[i++];
    while (j <= hi)
        t[k++] = nums[j++];
    k = 0;
    for (int i = lo; i <= hi; i++)
        nums[i] = t[k++];
}
void mergeSort(vector<int> &nums, int lo, int hi)
{
    if (lo >= hi)
        return;
    int mid = lo + (hi - lo) / 2;
    mergeSort(nums, lo, mid);
    mergeSort(nums, mid + 1, hi);
    merge(nums, lo, mid, hi);
}

void test01()
{
    vector<int> a;
    srand((unsigned int)time(nullptr));
    for (int i = 0; i < 10; i++)
    {
        a.push_back(rand() % 100 + 1);
    }
    show(a);
    quickSort_3way(a, 0, a.size() - 1);
    // mergeSort(a, 0, a.size() - 1);
    show(a);
}

int main()
{
    test01();
}