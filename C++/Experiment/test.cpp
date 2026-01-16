#include <vector>
#include <ctime>
#include <iostream>
#include <cmath>
using namespace std;

class People
{
public:
    int num;

    virtual void say()
    {
        cout << "People say:" << endl;
    }
};

class Student : public People
{
public:
    int getNum()
    {
        return num;
    }

    void say()
    {
        cout << "Student say:" << endl;
    }
};

class Teacher : public People
{
public:
    void say()
    {
        cout << "Teacher say:" << endl;
    }
};

int main()
{
    vector<int> a(10, 0);
    srand((unsigned int)time(NULL));
    for (int i = 0; i < 10; i++)
        a[i] = rand() % 100;

    vector<int> prefixSum(11, 0);
    for (int i = 1; i <= 10; i++)
        prefixSum[i] = prefixSum[i - 1] + a[i - 1];

    for (int t : a)
        cout << t << ' ';
    cout << endl;
    for (int i = 1; i <= 10; i++)
        cout << prefixSum[i] << ' ';
    cout << endl;

    string x = "ohmiyou", y = "aaa";
    cout << x << ' ' << y << endl
         << x + y << endl;

    People *p = new Student();
    p->say();
    p = new Teacher();
    p->say();

    return 0;
}