#include <vector>
#include <iostream>
using namespace std;

class UnionFind
{
private:
	vector<int> parent;
	vector<int> rank;
public:
	UnionFind(int n)
	{
		parent.resize(n);
		rank.resize(n, 0);
		for (int i = 0; i < n; i++)
			parent[i] = i;
	}
	int Find(int target)
	{
		return parent[target] == target ? target : Find(parent[target]);
	}
	void Union(int x, int y)
	{
		int x_parent = Find(x), y_parent = Find(y);
		if (rank[x_parent] <= rank[y_parent])
		{
			if (rank[x_parent] = rank[y_parent])
				rank[x_parent]++;
			parent[x_parent] = y_parent;
		}
		else
			parent[y_parent] = x_parent;
	}
};

int main()
{
	UnionFind uf(5);
	uf.Union(0, 1);
	uf.Union(2, 1);
	uf.Union(1, 3);
	cout << uf.Find(0) << endl;
	cout << uf.Find(4) << endl;
	return 0;
}