#include <vector>   // 引入标准库中的 vector 类型
#include <ctime>    // 引入 time 函数所在的头文件
#include <iostream> // 引入 iostream 头文件，用于输入输出操作
#include <cmath>    // 引入数学函数所在的头文件

using namespace std; // 使用标准命名空间，简化代码

class People // 定义一个名为 People 的基类
{
public:
    int num; // 基类的一个成员变量

    virtual void say() // 定义一个虚函数 say
    {
        cout << "People say:" << endl;
    }
};

class Student : public People // 从 People 类派生出 Student 类
{
public:
    int getNum() // 返回 num 成员变量的值
    {
        return num;
    }

    void say() // 重写父类中的 say 函数
    {
        cout << "Student say:" << endl;
    }
};

class Teacher : public People // 从 People 类派生出 Teacher 类
{
public:
    void say() // 重写父类中的 say 函数
    {
        cout << "Teacher say:" << endl;
    }
};

int main()
{
    vector<int> a(10, 0); // 初始化一个包含10个元素的向量，每个元素都为0

    srand((unsigned int)time(NULL)); // 使用当前时间作为随机数生成器的种子
    for (int i = 0; i < 10; i++)     // 遍历前10次
        a[i] = rand() % 100;         // 将随机整数（范围为0到99）存入向量

    vector<int> prefixSum(11, 0);                   // 初始化一个包含1个元素的向量，每个元素都为0
    for (int i = 1; i <= 10; i++)                   // 遍历前10次
        prefixSum[i] = prefixSum[i - 1] + a[i - 1]; // 计算前缀和

    for (int t : a)       // 使用范围for循环遍历向量a中的每个元素
        cout << t << ' '; // 输出当前元素的值并打印空格
    cout << endl;         // 打印换行符

    for (int i = 1; i <= 10; i++)    // 遍历前10次
        cout << prefixSum[i] << ' '; // 输出当前前缀和的值并打印空格
    cout << endl;                    // 打印换行符

    string x = "ohmiyou", y = "aaa"; // 初始化两个字符串变量
    cout << x << ' ' << y << endl;   // 输出第一个字符串，并在后面加上一个空格，再输出第二个字符串
    cout << (x + y) << endl;         // 输出两个字符串的拼接结果

    People *p = new Student(); // 动态分配一个指向Student对象的指针
    p->say();                  // 调用 say 函数
    p = new Teacher();         // 动态分配一个新的指向Teacher对象的指针
    p->say();                  // 再次调用 say 函数

    return 0; // 返回0，表示程序正常结束
}