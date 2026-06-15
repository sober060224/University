#include <cstdio>  // 标准输入输出，用于 scanf/printf
#include <cstring> // C 字符串函数库，用于 memset
#include <vector>  // 向量库，用于动态存储树节点
using namespace std;

/**
 * @brief 树节点结构体
 * 记录每个子集划分方案下的左右距离
 */
struct Tree
{
  double L = 0, R = 0; // L: 左边界距离，R: 右边界距离
};

const int maxn = 6; // 最大元素数量（状态压缩上限：2^6 = 64 个子集）

int n, vis[1 << maxn];             // n: 元素个数，vis: 子集访问标记数组
double r, w[maxn], sum[1 << maxn]; // r: 半径约束，w: 权重数组，sum: 子集权重和
vector<Tree> tree[1 << maxn];      // tree[i]: 存储子集 i 的所有可行树结构

/**
 * @brief 递归计算子集的树结构
 * @param subset 当前处理的子集（用二进制位表示）
 */
void dfs(int subset)
{
  if (vis[subset])
    return;           // 剪枝：已访问的子集直接返回
  vis[subset] = true; // 标记当前子集为已访问

  bool have_children = false; // 标记当前子集是否有子划分

  // 枚举当前子集的所有非空真子集作为左子集
  // 技巧：(subset-1) & subset 可以枚举 subset 的所有子集
  for (int left = (subset - 1) & subset; left; left = (left - 1) & subset)
  {
    have_children = true;      // 存在子划分
    int right = subset ^ left; // 右子集 = 当前子集 XOR 左子集（剩余部分）

    // 计算左右子集占当前子集的权重比例
    double d1 = sum[right] / sum[subset]; // 右子集比例
    double d2 = sum[left] / sum[subset];  // 左子集比例

    // 递归处理左右子集
    dfs(left);
    dfs(right);

    // 合并左右子树的所有可行方案
    for (int i = 0; i < tree[left].size(); i++)
    {
      for (int j = 0; j < tree[right].size(); j++)
      {
        Tree t;
        // 计算合并后的左右边界距离
        // t.L: 左边界 = max(左子树左边界 + 右比例，右子树左边界 - 左比例)
        t.L = max(tree[left][i].L + d1, tree[right][j].L - d2);
        // t.R: 右边界 = max(右子树右边界 + 左比例，左子树右边界 - 右比例)
        t.R = max(tree[right][j].R + d2, tree[left][i].R - d1);

        // 剪枝：只有总距离小于半径 r 的方案才保留
        if (t.L + t.R < r)
          tree[subset].push_back(t);
      }
    }
  }

  // 基础情况：叶子节点（单个元素），添加默认树节点
  if (!have_children)
    tree[subset].push_back(Tree());
}

int main()
{
  int T;
  scanf("%d", &T); // 读取测试用例数量

  while (T--)
  {
    // 读取半径约束和元素个数
    scanf("%lf%d", &r, &n);

    // 读取每个元素的权重
    for (int i = 0; i < n; i++)
      scanf("%lf", &w[i]);

    // 预处理：计算所有子集的权重和
    for (int i = 0; i < (1 << n); i++)
    {
      sum[i] = 0;
      tree[i].clear(); // 清空上一轮测试的数据

      // 位运算枚举子集包含的元素
      for (int j = 0; j < n; j++)
        if (i & (1 << j)) // 如果第 j 位在子集 i 中
          sum[i] += w[j]; // 累加对应权重
    }

    int root = (1 << n) - 1;     // 根节点：包含所有元素的完整子集
    memset(vis, 0, sizeof(vis)); // 重置访问标记数组

    dfs(root); // 从完整子集开始递归计算

    // 在所有可行方案中找最大值
    double max_value = -1;
    for (int i = 0; i < tree[root].size(); i++)
      max_value = max(max_value, tree[root][i].L + tree[root][i].R);

    printf("%.10f\n", max_value); // 输出结果（保留 10 位小数）
  }

  return 0;
}