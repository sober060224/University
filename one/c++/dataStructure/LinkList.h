#include <iostream>
#include <vector>
#include <algorithm>

#define int long long
using namespace std;

class Lnode
{
public:
    int data;
    Lnode *next;
    Lnode() : next(nullptr) {}
    Lnode(int t) : data(t), next(nullptr) {}
};

// 双指针检查链表成环
bool CheckCycle(Lnode *head);

/// @brief 头插法逆序建链表
/// @return 头指针
Lnode *headInsert();

/// @brief 尾插法正序建链表
/// @return 头指针
Lnode *EndInsert();

/// @brief 释放堆内存
/// @param head 被移动的指针
void release(Lnode *&head);

/// @brief 打印链表
/// @param head 被移动的指针
void show(Lnode *&head);

/// @brief 按序号查找
/// @param b 头结点
/// @return 找到的节点
Lnode *FindByIndex(Lnode *b);

/// @brief 按值查找
/// @param b 头结点
/// @return 找到的节点
Lnode *FindByValue(Lnode *b);

/// @brief 在指定节点后插入节点
/// @param t 指定节点
/// @param p 插入节点
void InsertBack(Lnode *t, Lnode *p);

/// @brief 插入节点测试函数
/// @param b 头结点
/// @param t 指定节点
void InsertNode(Lnode *&b, Lnode *t);