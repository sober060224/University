// #include <climits>
// #include <vector>
// #include <iostream>
// using namespace std;
// #define N 100
// void show(vector<int> a)
// {
//     for (int i = 1; i <= 10; i++)
//     {
//         cout << a[i] << ' ';
//     }
//     cout << endl
//          << "-----------------------------\n";
// }

// void seleteSort(vector<int> a)
// {
//     for (int i = 1; i <= 10; i++)
//     {
//         int a[0] = i;
//         for (int j = i + 1; j <= 10; j++)
//         {
//             if (a[j] < a[t])
//             {
//                 a[0] = j; // 找到最小值的位置
//             }
//         }
//         swap(a[a[0]], a[i]);
//     }
//     show(a);
// }

// void insertSort(vector<int> a)
// {
//     for (int i = 2; i <= 10; i++)
//     {
//         int index = 0;
//         for (int j = 1; j <= i && a[j] < a[i]; j++)
//         {
//             index = j;
//         }
//         index++;                                           // 找到要插入的位置
//         int cur = a[index], next = a[index + 1], t = a[i]; // t存储要插入的值，cur保存后移的前一个元素，next保存被覆盖的元素
//         // 从前往后地后移，从后往前后移可以减少思维量、代码量，避免考虑覆盖
//         for (int j = index + 1; j <= i; j++)
//         {
//             a[j] = cur;
//             cur = next;
//             next = a[j + 1];
//         }
//         a[index] = t;
//     }
//     show(a);
// }

// void bubbleSort(vector<int> a)
// {
//     for (int i = 1; i <= 9; i++)
//     {
//         for (int j = 1; j <= 10 - i; j++)
//         {
//             if (a[j] > a[j + 1])
//             {
//                 swap(a[j], a[j + 1]);
//             }
//         }
//     }
//     show(a);
// }

// void shellSort(vector<int> a)
// {
//     int h = 1;
//     while (h < 3)
//     {
//         h = 3 * h + 1;
//     }
//     while (h >= 1)
//     {
//         for (int i = h; i <= 10; i++)
//         {
//             for (int j = i; j >= h && a[j] < a[j - h]; j-= h)
//             {
//                 swap(a[j], a[j - h]);
//             }
//         }
//         h /= 3;
//     }
//     show(a);
// }

// void quickSort(vector<int> &a, int lo, int hi)
// {
//     if (lo >= hi)
//     {
//         return;
//     }

//     int index = lo + rand() % (hi - lo + 1), pivot = a[index];
//     swap(a[hi], a[index]);
//     int u = lo, v = lo, w = hi - 1;
//     while (w >= v) // =，处理最后一个元素
//     {
//         if (a[v] < pivot)
//         {
//             swap(a[u++], a[v++]);
//         }
//         else if (a[v] > pivot)
//         {
//             swap(a[w--], a[v]);
//         }
//         else
//         {
//             v++;
//         }
//     }
//     swap(a[hi], a[v]);

//     quickSort(a, lo, u - 1);
//     quickSort(a, v + 1, hi);
// }

// void down(vector<int> &a, int i, int n)
// {
//     int ma = i;
//     while (true)
//     {
//         if (i * 2 <= n && a[ma] < a[i * 2])
//         {
//             ma = 2 * i;
//         }
//         if (2 * i + 1 <= n && a[ma] < a[2 * i + 1])
//         {
//             ma = 2 * i + 1;
//         }
//         if (ma == i)
//         {
//             break;
//         }
//         swap(a[ma], a[i]);
//         i = ma;
//     }
// }
// void heapSort(vector<int> &a)
// {
//     for (int i = 5; i >= 1; i--)
//     {
//         down(a, i, 10);
//     }
//     for (int i = 10; i >= 2; i--)
//     {
//         swap(a[1], a[i]);
//         down(a, 1, i - 1);
//     }
// }

// void adjacent_insertSort(vector<int> a)
// {
//     for (int i = 2; i <= 10; i++)
//     {
//         int lo = 1, hi = i - 1;
//         while (lo <= hi)
//         {
//             int mid = lo + (hi - lo) / 2;
//             if (a[mid] <= a[i])
//             {
//                 lo = mid + 1;
//             }
//             else
//             {
//                 hi = mid - 1;
//             }
//         }
//         int a[0] = a[i];
//         for (int j = i; j > lo; j--)
//         {
//             a[j] = a[j - 1];
//         }
//         a[lo] = a[0];
//     }
//     show(a);
// }

// int main()
// {
//     vector<int> a(N, 0);
//     for (int i = 1; i <= 10; i++)
//     {
//         a[i] = rand() % 100 + 1;
//     }
//     show(a);
//     seleteSort(a);
//     insertSort(a);
//     bubbleSort(a);
//     srand((unsigned int)time(nullptr));
//     quickSort(a, 1, 10);
//     show(a);
//     for (int i = 1; i <= 10; i++)
//     {
//         a[i] = rand() % 100 + 1;
//     }
//     show(a);
//     heapSort(a);
//     show(a);
//     for (int i = 1; i <= 10; i++)
//     {
//         a[i] = rand() % 100 + 1;
//     }
//     show(a);
//     adjacent_insertSort(a);
//     shellSort(a);
// }
#include <iostream>
#include <cstdlib>
#include <ctime>
#include <vector>
using namespace std;

#define MAXSIZE 100 /*参加排序元素的最大个数*/

typedef struct list
{
    int key;
} RedType;

typedef struct
{
    RedType r[MAXSIZE + 1];
    int length; /*参加排序元素的实际个数*/
} SqList;

// 显示数组内容
void show(SqList L)
{
    for (int i = 1; i <= L.length; i++)
    {
        cout << L.r[i].key << ' ';
    }
    cout << endl
         << "-----------------------------\n";
}

// 1. 简单选择排序
void selectSort(SqList &L)
{
    for (int i = 1; i < L.length; i++)
    {
        int minIndex = i;
        for (int j = i + 1; j <= L.length; j++)
        {
            if (L.r[j].key < L.r[minIndex].key)
            {
                minIndex = j;
            }
        }
        if (minIndex != i)
        {
            swap(L.r[i], L.r[minIndex]);
        }
    }
}

// 2. 直接插入排序
void insertSort(SqList &L)
{
    for (int i = 2; i <= L.length; i++)
    {
        if (L.r[i].key < L.r[i - 1].key)
        {
            L.r[0] = L.r[i]; // 哨兵
            int j;
            for (j = i - 1; L.r[0].key < L.r[j].key; j--)
            {
                L.r[j + 1] = L.r[j];
            }
            L.r[j + 1] = L.r[0];
        }
    }
}

// 3. 冒泡排序
void bubbleSort(SqList &L)
{
    for (int i = 1; i < L.length; i++)
    {
        bool swapped = false;
        for (int j = 1; j <= L.length - i; j++)
        {
            if (L.r[j].key > L.r[j + 1].key)
            {
                swap(L.r[j], L.r[j + 1]);
                swapped = true;
            }
        }
        if (!swapped)
            break; // 如果没有发生交换，说明已经有序
    }
}

// 4. 希尔排序
void shellSort(SqList &L)
{
    for (int gap = L.length / 2; gap > 0; gap /= 2)
    {
        for (int i = gap + 1; i <= L.length; i++)
        {
            if (L.r[i].key < L.r[i - gap].key)
            {
                L.r[0] = L.r[i];
                int j;
                for (j = i - gap; j > 0 && L.r[0].key < L.r[j].key; j -= gap)
                {
                    L.r[j + gap] = L.r[j];
                }
                L.r[j + gap] = L.r[0];
            }
        }
    }
}

// 5. 快速排序分区函数
int partition(SqList &L, int low, int high)
{
    L.r[0] = L.r[low]; // 用第一个元素作为枢轴
    int pivotkey = L.r[low].key;

    while (low < high)
    {
        while (low < high && L.r[high].key >= pivotkey)
            high--;
        L.r[low] = L.r[high];
        while (low < high && L.r[low].key <= pivotkey)
            low++;
        L.r[high] = L.r[low];
    }
    L.r[low] = L.r[0];
    return low;
}

// 快速排序递归函数
void quickSort(SqList &L, int low, int high)
{
    if (low < high)
    {
        int pivotloc = partition(L, low, high);
        quickSort(L, low, pivotloc - 1);
        quickSort(L, pivotloc + 1, high);
    }
}

// 6. 堆排序调整函数
void heapAdjust(SqList &L, int s, int m)
{
    RedType rc = L.r[s];
    for (int j = 2 * s; j <= m; j *= 2)
    {
        if (j < m && L.r[j].key < L.r[j + 1].key)
            j++;
        if (rc.key >= L.r[j].key)
            break;
        L.r[s] = L.r[j];
        s = j;
    }
    L.r[s] = rc;
}

// 堆排序
void heapSort(SqList &L)
{
    // 构建大顶堆
    for (int i = L.length / 2; i > 0; i--)
    {
        heapAdjust(L, i, L.length);
    }

    // 进行堆排序
    for (int i = L.length; i > 1; i--)
    {
        swap(L.r[1], L.r[i]);
        heapAdjust(L, 1, i - 1);
    }
}

// 7. 折半插入排序
void binaryInsertSort(SqList &L)
{
    for (int i = 2; i <= L.length; i++)
    {
        L.r[0] = L.r[i]; // 哨兵
        int low = 1, high = i - 1;

        // 折半查找插入位置
        while (low <= high)
        {
            int mid = (low + high) / 2;
            if (L.r[0].key < L.r[mid].key)
                high = mid - 1;
            else
                low = mid + 1;
        }

        // 移动元素
        for (int j = i - 1; j >= low; j--)
        {
            L.r[j + 1] = L.r[j];
        }
        L.r[low] = L.r[0];
    }
}

// 初始化数据
void initData(SqList &L, int n)
{
    L.length = n;
    srand((unsigned int)time(nullptr));
    for (int i = 1; i <= n; i++)
    {
        L.r[i].key = rand() % 100 + 1;
    }
}

// 复制数据
void copyData(SqList &dest, SqList src)
{
    dest.length = src.length;
    for (int i = 1; i <= src.length; i++)
    {
        dest.r[i] = src.r[i];
    }
}

int main()
{
    SqList L, temp;
    int choice, n;

    cout << "请输入要排序的元素个数: ";
    cin >> n;

    if (n > MAXSIZE || n <= 0)
    {
        cout << "元素个数超出范围或无效!" << endl;
        return 1;
    }

    initData(L, n);

    do
    {
        cout << "\n========== 排序算法菜单 ==========\n";
        cout << "1. 简单选择排序\n";
        cout << "2. 直接插入排序\n";
        cout << "3. 冒泡排序\n";
        cout << "4. 希尔排序\n";
        cout << "5. 快速排序\n";
        cout << "6. 堆排序\n";
        cout << "7. 折半插入排序\n";
        cout << "0. 退出\n";
        cout << "请选择排序算法: ";
        cin >> choice;

        if (choice == 0)
            break;

        // 复制原始数据到临时数组
        copyData(temp, L);

        cout << "\n排序前: ";
        show(temp);

        switch (choice)
        {
        case 1:
            selectSort(temp);
            cout << "简单选择排序后: ";
            break;
        case 2:
            insertSort(temp);
            cout << "直接插入排序后: ";
            break;
        case 3:
            bubbleSort(temp);
            cout << "冒泡排序后: ";
            break;
        case 4:
            shellSort(temp);
            cout << "希尔排序后: ";
            break;
        case 5:
            quickSort(temp, 1, temp.length);
            cout << "快速排序后: ";
            break;
        case 6:
            heapSort(temp);
            cout << "堆排序后: ";
            break;
        case 7:
            binaryInsertSort(temp);
            cout << "折半插入排序后: ";
            break;
        default:
            cout << "无效选择!\n";
            continue;
        }

        show(temp);

    } while (choice != 0);

    return 0;
}