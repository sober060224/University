#include "LinkList.h"

void solve()
{
    // 头插法创建链表（逆序）
    Lnode *a = headInsert();
    release(a);
    cout << endl;

    // 尾插法创建链表（正序）
    Lnode *b = EndInsert();
    cout << endl;

    // 按序号查找、按值查找
    Lnode *t = FindByIndex(b);
    if (t != nullptr)
        cout << t->data << endl;
    else
        cout << "找不到" << endl;

    t = FindByValue(b);
    if (t != nullptr)
        cout << t->data << endl;
    else
        cout << "找不到" << endl;

    // 插入结点（在指定结点前/后插入）
    InsertNode(b, t);

    // 删除结点（删除指定结点，并释放内存）

    // 双指针检查链表成环
    bool hasCycle = CheckCycle(b);
    if (hasCycle)
        cout << "链表存在环" << endl;
    else
        cout << "链表不存在环" << endl;
}

void InsertNode(Lnode *&b, Lnode *t)
{
    // 节点尾插
    Lnode *i = b;
    int x;
    cin >> x;
    Lnode *p = new Lnode(x);
    InsertBack(t, p);
    show(i);
    cout << endl;

    // 节点头插
    i = b;
    Lnode *q = new Lnode(t->data);
    t->data = x;
    InsertBack(t, q);
    show(i);
}

void InsertBack(Lnode *t, Lnode *p)
{
    if (t == nullptr)
    {
        cout << "指定节点为空" << endl;
        return;
    }

    p->next = t->next;
    t->next = p;
}

Lnode *FindByValue(Lnode *b)
{
    cout << "FindByValue:" << endl;
    int x;
    cin >> x;
    Lnode *t = b->next;

    while (t != nullptr)
    {
        if (t->data == x)
            break;
        t = t->next;
    }

    if (t == nullptr)
        return nullptr;
    else
        return t;
}

Lnode *FindByIndex(Lnode *b)
{
    cout << "FindByIndex:" << endl;
    int x;
    cin >> x;
    Lnode *t = b;

    while (x-- && t != nullptr)
        t = t->next;

    if (t == nullptr)
        return nullptr;
    else
        return t;
}

Lnode *EndInsert()
{
    Lnode *head = new Lnode(), *end = head;

    for (int i = 0; i < 10; i++)
    {
        Lnode *t = new Lnode(i);
        t->next = end->next;
        end->next = t;
        end = t;
    }

    Lnode *t = head;
    show(t);
    return head;
}

Lnode *headInsert()
{
    Lnode *head = new Lnode();
    for (int i = 0; i < 10; i++)
    {
        Lnode *t = new Lnode(i);
        t->next = head->next;
        head->next = t;
    }

    Lnode *t = head;
    show(t);
    return head;
}

void release(Lnode *&head)
{
    Lnode *node;

    while (head != nullptr)
    {
        node = head;
        head = head->next;
        delete node;
    }
    head = nullptr;
}

void show(Lnode *&head)
{
    Lnode *t = head->next;

    while (t != nullptr)
    {
        cout << t->data << ' ';
        t = t->next;
    }
}

class treeNode
{
public:
    int data;
    treeNode *left, *right;
    treeNode(int data) : left(nullptr), right(nullptr), data(data) {}
};

treeNode *build(string s, int lo, int hi)
{
    if (lo >= hi)
        return nullptr;
    int mid = (lo + hi) / 2;
    treeNode *root = new treeNode(s[mid] - '0');
    root->left = build(s, lo, mid);
    root->right = build(s, mid + 1, hi);
    return root;
}

void print(treeNode *n)
{
    if (n == nullptr)
        return;
    print(n->left);
    cout << n->data;
    print(n->right);
}

// 双指针检查链表成环
bool CheckCycle(Lnode *head)
{
    if (head == nullptr || head->next == nullptr)
        return false;

    Lnode *slow = head;
    Lnode *fast = head->next;

    while (fast != nullptr && fast->next != nullptr)
    {
        if (slow == fast)
            return true;

        slow = slow->next;
        fast = fast->next->next;
    }

    return false;
}

void test01()
{
    string s = "23415433";
    treeNode *root = build(s, 0, s.size());
    print(root);
}

signed main()
{
    ios::sync_with_stdio(false);
    cin.tie(0), cout.tie(0);

    solve();
    // test01();
    // string s = "123";
    // int a = s[1] - '0';
    // cout << a;
}