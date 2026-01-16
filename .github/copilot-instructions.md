# AI 编码代理指令

## 项目概述
此工作区包含多种语言的大学编程项目：
- **C++**：算法实现和竞技编程解决方案（UVa 问题等）
- **Java**：多人中国象棋游戏（Swing UI + socket 服务器）和通用 Java 实验
- **Maven 项目**：具有依赖项如 MySQL JDBC 的结构化 Java 应用程序

## 架构与关键组件

### 中国象棋游戏 (`chinese-chess---java-swing-master/`)
- **客户端-服务器模型**：服务器在端口 8080 上运行，通过线程处理多个客户端
- **包结构**：
  - `view/`：UI 组件（MainFrame 大厅、GameFrame、GamePanel_LD 游戏棋盘）
  - `model/`：数据模型（MyMessage、User、Record 用于移动）
  - `server/`：ChessServer、ClientThread、ClientReceiveThread
  - `game/`：棋子类（Chess 抽象基类、Ma/马、Che/车等）
  - `util/`：SocketUtil 用于网络通信
- **游戏逻辑**：
  - GamePanel_LD 中的 32 棋子数组
  - 通过每个子类中的 `isAbleMove(Point, GamePanel_LD)` 验证移动
  - 网络消息：LOGIN、MOVE、EAT、FIGHT、PEACE、HuiQi（悔棋）
  - 轮流制与棋盘锁定机制

### C++ 算法 (`C++/`)
- 算法模板和问题解决方案的独立 `.cpp` 文件
- 常见模式：动态规划、贪心、图算法、排序
- 通过 VS Code 任务使用 g++ 构建：`C/C++: g++.exe 生成活动文件`

### Java Maven 项目 (`Projects/`)
- 标准 Maven 结构，使用 Java 24
- 依赖项：JUnit 4.11、MySQL Connector/J 8.0.33
- 通过 VS Code 任务构建：`java (build): Build Workspace`

## 开发者工作流程

### 构建与运行
- **C++**：使用 VS Code 任务 "C/C++: g++.exe 生成活动文件" 编译活动文件
- **Java 象棋游戏**：首先运行 `ChessServer.main()`，然后为客户端运行 `MainFrame.main()`
- **Maven 项目**：使用 VS Code 任务 "java (build): Build Workspace" 或 `mvn compile`

### 调试
- 象棋游戏：使用 `System.out.println()` 输出网络消息和游戏状态
- C++：g++ 编译后使用 gdb 调试
- Java：Swing 应用的标准 IDE 调试

## 代码模式与约定

### 象棋游戏特定
- **坐标系统**：1-9 x，1-10 y 网格（红方在底部，黑方在顶部）
- **玩家颜色**：0 = 红，1 = 黑
- **移动验证**：每个棋子实现 `isAbleMove()` 检查棋盘状态
- **网络协议**：MyMessage 枚举类型用于所有游戏动作
- **UI 更新**：GamePanel_LD 中状态变化后调用 `repaint()`

### 通用模式
- **错误处理**：网络代码中的基本 try-catch，JOptionPane 用于用户警报
- **线程**：ExecutorService 用于服务器客户端处理
- **序列化**：棋子实现 Serializable 用于网络传输

## 关键参考文件
- `GamePanel_LD.java`：核心游戏逻辑和 UI 交互
- `ChessServer.java`：服务器消息处理和客户端管理
- `Chess.java`：所有棋子的基类
- `Ma.java`：棋子实现的示例，带有移动规则
- `C++/solves/uva11054.cpp`：竞技编程解决方案示例

## 依赖项与环境
- Maven 项目使用 Java 24
- C++ 使用 MinGW g++
- 数据库项目使用 MySQL（通过 JDBC 连接）
- 无外部 API；全部本地多人象棋

修改象棋游戏功能时，重点维护客户端-服务器架构。对于算法问题，确保代码高效并遵循标准竞技编程实践。</content>
<parameter name="filePath">.github/copilot-instructions.md