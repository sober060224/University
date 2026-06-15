#include <iostream>
#include <vector>
#include <algorithm>
#define int long long
using namespace std;

class poker
{
public:
    char x;
    int y;
    poker(char x, int y) : x(x), y(y) {}
    poker() : y(0) {}
};

// 对子
bool duizi(vector<poker> res, int index)
{
    for (int i = index; i > 0; i--)
    {

    }
}
// 三同张
bool santongzhang()
{

}
// 顺子
bool shunzi()
{

}

void solve()
{
    
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    // 找到一种单张尽量少的组合方式n≥13,找到３到n中的单张数尽量少
    // A能当成比n大的当成顺子，2不能
    
    int n, m;
    cin >> n >> m;
    string a;
    getline(cin, a);

    while (m--)
    {
        vector<poker> cards;
        string card;
        // for (int i = 0; i < 4; i++)
        // {
        //     cin >> card;
        // }
        
        getline(cin, card);
        int i = 0;
        while (i < card.size())
        {
            poker t;
            char c = card[i];
            t.x = c;
            i += 2;
            while (card[i] != ' ' && i < card.size())
            {
                c = card[i];
                t.y = c - '0' + t.y * 10;
                i++;
            }
            cards.push_back(t);
            i++;
        }

        
        // for (int i =  0; i < card.size(); i++)
        // {
        //     poker t;
        //     if (i % 4 == 0)
        //         c = card[i];
        //     else if (i % 4 == 2)
        //         num = card[i] - '0';
        // }
    }
}