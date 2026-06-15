#include <iomanip>
#include <iostream>
#include <vector>
#include <algorithm>
#include <queue>
using namespace std;

// // Fractional Knapsack Problem部分背包问题
// class Coin
// {
// public:
//     int m, v;
//     double ratio;
//     Coin(int m, int v, double ratio) : m(m), v(v), ratio(ratio) {}
//     Coin() {}
// };
// double FractionalKnapsack(vector<Coin> &Coins, int T)
// {
//     sort(Coins.begin(), Coins.end(), [](Coin &a, Coin &b)
//          { return a.ratio > b.ratio; });
//     double res = 0.00;
//     for (int i = 0; i < Coins.size(); i++)
//     {
//         if (Coins[i].m < T)
//         {
//             res += Coins[i].v;
//             T -= Coins[i].m;
//         }
//         else
//         {
//             res += Coins[i].ratio * T;
//             break;
//         }
//     }
//     return res;
// }
// void test()
// {
//     int N, T;
//     cin >> N >> T;
//     vector<Coin> Coins;
//     for (int i = 0; i < N; i++)
//     {
//         int m, v;
//         cin >> m >> v;
//         double ratio = double(v) / m;
//         Coins.push_back(Coin(m, v, ratio));
//     }
//     cout << fixed << setprecision(2) << FractionalKnapsack(Coins, T);
// }

// // HuffmanTree哈弗曼编码问题
// class Node
// {
// public:
//     int value;
//     Node *left, *right;
//     Node(int v) : value(v), left(nullptr), right(nullptr) {}
//     Node() {}
// };
// struct mycompare
// {
//     bool operator()(Node *a, Node *b)
//     {
//         return a->value > b->value;
//     }
// };
// void DeleteTree(Node *t)
// {
//     if (!t)
//     {
//         return;
//     }
//     DeleteTree(t->left);
//     DeleteTree(t->right);
//     delete t;
//     t = nullptr;
// }
// int BuildHuffmanTree(vector<int> a)
// {
//     priority_queue<Node *, vector<Node *>, mycompare> pq;
//     int res = 0;
//     for (int i = 0; i < a.size(); i++)
//     {
//         pq.push(new Node(a[i]));
//     }
//     while (pq.size() >= 1)
//     {
//         Node *a = pq.top();
//         pq.pop();
//         Node *b = pq.top();
//         pq.pop();
//         res = res + a->value + b->value;
//         Node *t = new Node(a->value + b->value);
//         t->left = a;
//         t->right = b;
//         pq.push(t);
//     }
//     DeleteTree(pq.top());
//     return res;
// }
// void test()
// {
//     int n;
//     cin >> n;
//     vector<int> a;
//     int t;
//     for (int i = 0; i < n; i++)
//     {
//         cin >> t;
//         a.push_back(t);
//     }
//     cout << BuildHuffmanTree(a);
// }

int main()
{
    // test();
    return 0;
}