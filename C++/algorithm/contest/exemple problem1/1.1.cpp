// #include <iostream>
// #include <vector>
// #include <algorithm>
// #define int long long
// using namespace std;
// void solve()
// {
// }
// signed main()
// {
//     ios::sync_with_stdio(false);
//     cin.tie(0), cout.tie(0);
//     // n ≤ 100,(0, 0),(a, b)
//     int n, a, b;
//     cin >> n >> a >> b;
//     // 田地的位置
//     int sum = 0;
//     for (int i = 0; i < n; i++)
//     {
//         int x1, y1, x2, y2;
//         cin >> x1 >> y1 >> x2 >> y2;
//         // 在矩形内，田地左右上、下角在里面
//         // 计算狻狻选定区域内已经存在的田地面积,左下角坐标和右上角坐标唯一确定,( x1 <x2 )、( y1 < y2 )
//         // 找到每个矩形在大矩形内有效的面积，任意两块的交集面积均为 0，仅边界处可能有所重叠
//         bool lefttop = y2 > 0 && x1 < a && x2 > a && y1 < 0, righttop = x2 > 0 && y2 > 0 && x1 < 0 && y1 < 0, leftdown = x1 < a && y1 < b && x2 > a && y2 > b;
//         bool rightdown = x2 > 0 && y1 < b && x1 < 0 && y2 > b, inSqure = x1 >= 0 && y1 >= 0 && x2 <= a && y2 <= b;
//         if (lefttop && !rightdown)
//             y1 = 0, x2 = a;
//         if (righttop && !leftdown)
//             x1 = 0, y1 = 0;
//         if (rightdown && !lefttop)
//             x1 = 0, y2 = b;
//         if (leftdown && !righttop)
//             x2 = a, y2 = b;
//         if (lefttop || leftdown || righttop || rightdown || inSqure)
//             sum = (x2 - x1) * (y2 - y1) + sum;
//     }
//     // 输出值
//     cout << sum;
// }

#include <iostream>
#include <algorithm>
#define int long long
using namespace std;

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    int n, a, b;
    cin >> n >> a >> b;

    int sum = 0;
    for (int i = 0; i < n; i++)
    {
        int x1, y1, x2, y2;
        cin >> x1 >> y1 >> x2 >> y2;
        
        // 计算矩形与目标区域(0,0)-(a,b)的交集
        // 如果完全没有交集，这些值会使计算结果为0或负数
        int left = max(x1, 0LL);      // 交集左边界
        int right = min(x2, a);       // 交集右边界
        int bottom = max(y1, 0LL);    // 交集下边界
        int top = min(y2, b);         // 交集上边界
        
        // 只有当交集有效（right>left且top>bottom）时才计算面积
        if (right > left && top > bottom) {
            sum += (right - left) * (top - bottom);
        }
    }
    
    cout << sum;
    return 0;
}