#include <iostream>
#include <vector>
#include <algorithm>
#define N 1001
using namespace std;

// index：串的下标。
int solve(vector<int> &s, int L, int index, int n, int &count)
{
    // 只有构成困难串才会递归到下一个
    if (count++ == n)
    {
        /*
        有可能前一个字符构成了困难串，但递归到下一个字符的时候不可能构成困难串，就要回
        溯到上一个字符，接着判断下一个字符是否构成困难串。所以第n个困难串不等于下标为
        n的困难串
        */
        for (int i = 0; i < index; i++)
            cout << char('A' + s[i]);
        cout << endl;
        // 找到了第n个困难串
        return 0;
    }

    // 判断新加入的字符能不能和前面的构成困难串
    for (int i = 0; i < L; i++)
    {
        s[index] = i;
        bool ok = true;

        for (int j = 1; j * 2 <= index + 1; j++)
        {
            bool isEqual = true;
            for (int k = 0; k < j; k++)
            {
                // 判断新加入字符的偶数后缀字符串是否相同，有一个不同就不是简单串
                if (s[index - k] != s[index - j - k])
                {
                    isEqual = false;
                    break;
                }
            }

            // 是简单串就继续当前下标字符判断是否构成困难串
            if (isEqual)
            {
                ok = false;
                break;
            }
        }

        /*
        构成困难串就递归到下一个困难串判断，否则循环到下一个字符接着判断是否构
        成困难串直到找到第n个困难串返回了0才能结束，返回1说明这个字符构成困难串
        后下一个字符不可能构成困难串，需要改变当前字符为下一个字符继续递归判断
        */
        
        if (ok && !solve(s, L, index + 1, n, count))
            return 0;
        // 不需要判重，也不需要考虑回溯，不输出超过index的字符
    }
    // 本分支所有字符都找不到解，返回上一层继续判断
    return 1;
}

int main()
{
    ios::sync_with_stdio(false);
    cin.tie(0);
    cout.tie(0);

    int n, L, count = 0;
    vector<int> s(N);
    cin >> n >> L;
    solve(s, L, 0, n, count);

    return 0;
}