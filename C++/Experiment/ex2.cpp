#include <limits>
#include <cctype>
#include <algorithm>
#include <unordered_map>
#include <Windows.h>
#include <vector>
#include <memory>
#include <iostream>
#include <fstream>
#include <string>

using namespace std;

struct Data
{
public:
	string parent, child;
};

class TreeNode
{
public:
	string name;
	vector<shared_ptr<TreeNode>> children;

	TreeNode() {}
	TreeNode(string name) : name(name) {}
};

class UniversityManager
{
public:
	shared_ptr<TreeNode> root;
	UniversityManager() : root(make_shared<TreeNode>("某大学")) {}

	// 从txt文件读数据到数组中
	vector<Data> readDataToArray(const string &filename)
	{
		ifstream is(filename);
		if (!is.is_open())
		{
			cerr << "文件打开失败" << endl;
			return {};
		}

		vector<Data> datas;
		string line;
		while (getline(is, line))
			parseLineToArray(datas, line);

		is.close();
		return datas;
	}

	// 由数组创建树t的链式存储结构
	void createTreeFromArray(const vector<Data> &datas)
	{
		unordered_map<string, shared_ptr<TreeNode>> nodemap;
		nodemap[root->name] = root;

		// 创建节点
		for (Data data : datas)
		{
			string pre = data.parent;
			shared_ptr<TreeNode> preNode = make_shared<TreeNode>(pre);
			if (nodemap.find(pre) == nodemap.end())
				nodemap[pre] = preNode;

			string cur = data.child;
			shared_ptr<TreeNode> curNode = make_shared<TreeNode>(cur);
			if (nodemap.find(cur) == nodemap.end())
				nodemap[cur] = curNode;
		}

		// 建立关系
		for (Data data : datas)
		{
			string pre = data.parent;
			string cur = data.child;
			nodemap[pre]->children.push_back(nodemap[cur]);
		}
	}

	// 采用括号表示输出树t
	void printTree()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
			printTreeNode(root);
	}

	// 求各学院的专业数以及名称
	void printCollegeMajors()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
		{
			for (shared_ptr<TreeNode> college : root->children)
			{
				int count = 0;
				cout << college->name << ':';
				for (shared_ptr<TreeNode> major : college->children)
				{
					count++;
					cout << major->name << ' ';
				}
				cout << "专业数：" << count << endl;
			}
		}
	}

	// 求各学院的班数以及名称
	void printCollegeClass()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
		{
			for (shared_ptr<TreeNode> college : root->children)
			{
				int count = 0;
				cout << college->name << ':';
				for (shared_ptr<TreeNode> major : college->children)
				{
					for (shared_ptr<TreeNode> studentclass : major->children)
					{
						count++;
						cout << studentclass->name << ' ';
					}
				}
				cout << "班数：" << count << endl;
			}
		}
	}

	// 统计各学院的学生人数
	void printCollegeStudent()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
		{
			for (shared_ptr<TreeNode> college : root->children)
			{
				int count = 0;
				cout << college->name << "人数" << ':';
				for (shared_ptr<TreeNode> major : college->children)
				{
					for (shared_ptr<TreeNode> studentclass : major->children)
					{
						int t = 0;
						for (char c : studentclass->children[0]->name)
						{
							t *= 10;
							c -= '0';
							t += c;
						}
						count += t;
					}
				}
				cout << count << endl;
			}
		}
	}

	// 修改指定信息点的数据，并更新txt文件
	void updateData()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
		{
			string target, newValue;
			cout << "请指定要修改的节点名称：" << endl;
			getline(cin, target);
			getline(cin, target);
			cout << "请输入新值：" << endl;
			getline(cin, newValue);
			if (updateTreeNode(root, target, newValue))
			{
				cout << "成功更新" << target << "为" << newValue << endl;
				ofstream os("university.txt");
				if (!os.is_open())
					cerr << "无法读取文件" << endl;
				else
				{
					updateDataFile(root, os);
					os.close();
				}
			}
			else
				cout << "未找到节点：" << target << endl;
		}
	}

	// 退出操作，并销毁树
	void destroyTree()
	{
		if (!root)
			cout << "当前树为空" << endl;
		else
		{
			root = nullptr;
			cout << "销毁成功" << endl;
		}
	}

private:
	// 解析每一行数据
	void parseLineToArray(vector<Data> &datas, const string &line)
	{
		Data t;				  // 临时存储多格的值
		vector<string> cells; // 多格的值
		string cell;		  // 单格的值
		for (char c : line)
		{
			// 只有值、空格、换行符三种情况
			if (c != ' ')
				cell += c;
			else
			{
				cells.push_back(cell);
				cell.clear();
			}
		}

		// 最后一个字符不为空格还要加入
		if (!cell.empty())
			cells.push_back(cell);
		t.parent = cells[0];
		t.child = cells[1];
		datas.push_back(t);
	}

	// 递归树节点
	void printTreeNode(shared_ptr<TreeNode> node)
	{
		if (node->children.empty())
		{
			cout << node->name;
			return;
		}
		cout << node->name << '(';
		for (int i = 0; i < node->children.size(); i++)
		{
			printTreeNode(node->children[i]);
			if (i < node->children.size() - 1)
				cout << ',';
		}
		cout << ')';
	}

	// 递归遍历树节点
	bool updateTreeNode(shared_ptr<TreeNode> node, const string &target, const string &newValue)
	{
		if (!node)
			return false;
		if (node->name == target)
		{
			node->name = newValue;
			return true;
		}
		for (shared_ptr<TreeNode> &child : node->children)
			if (updateTreeNode(child, target, newValue))
				return true;
		return false;
	}

	// 递归写入文件数据
	void updateDataFile(shared_ptr<TreeNode> node, ofstream &os)
	{
		if (!node)
			return;
		for (shared_ptr<TreeNode> child : node->children)
			os << node->name << ' ' << child->name << endl;
		for (shared_ptr<TreeNode> child : node->children)
			updateDataFile(child, os);
	}
};

void menu()
{
	int choice;
	UniversityManager manager;
	vector<Data> datas;
	cout << "1.从txt文件读数据到R数组中。" << endl;
	cout << "2.由数组R创建树t的链式存储结构。" << endl;
	cout << "3.采用括号表示输出树t。" << endl;
	cout << "4.求各学院的专业数以及名称。" << endl;
	cout << "5.求各学院的班数以及名称。" << endl;
	cout << "6.统计各学院的学生人数。" << endl;
	cout << "7.修改指定信息点的数据，并更新txt文件。" << endl;
	cout << "8.退出操作，并销毁树。" << endl;
	cout << "请选择操作" << endl;
	while (true)
	{
		cin >> choice;
		if (cin.fail())
		{
			cin.clear();
			cin.ignore(1000, '\n');
			cout << "输入错误，请重新输入" << endl;
			continue;
		}

		switch (choice)
		{
		case 1:
		{
			datas = manager.readDataToArray("D:\\AAA_Software\\Code\\University\\C++\\Experiment\\university.txt");
			cout << "读取完成\n--------------------------\n";
			break;
		}
		case 2:
			manager.createTreeFromArray(datas);
			cout << "创建成功" << endl;
			cout << "--------------------------\n";
			break;
		case 3:
			manager.printTree();
			cout << "\n--------------------------\n";
			break;
		case 4:
			manager.printCollegeMajors();
			cout << "--------------------------\n";
			break;
		case 5:
			manager.printCollegeClass();
			cout << "--------------------------\n";
			break;
		case 6:
			manager.printCollegeStudent();
			cout << "--------------------------\n";
			break;
		case 7:
			manager.updateData();
			cout << "--------------------------\n";
			break;
		case 8:
			manager.destroyTree();
			cout << "--------------------------\n";
			return;
		default:
			cout << "输入错误" << endl;
			break;
		}
	}
}

int main()
{
	SetConsoleOutputCP(CP_UTF8);
	SetConsoleCP(CP_UTF8);
	menu();
	return 0;
}