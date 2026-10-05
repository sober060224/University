# University

大学课程作业、实验报告与学习笔记的汇总仓库。按学年分目录：`one/` 大一，`two/` 大二。

## 目录结构

```
University/
├── one/
│   └── c++/                    # C++ 课程：算法、数据结构、实验
├── two/
│   ├── cpu/                    # 硬件基础实践：Quartus 运算器与 CPU 整机
│   ├── Java/                   # 面向对象程序设计实践：Maven 项目
│   └── javaweb/                # Web 开发技术：静态页面实验 + 课程设计项目
├── markdown/                   # 学习笔记（复习资料、生活杂记）
├── certificate/                # 证书（cacc.pdf、CET-4 成绩单）
├── .foam/                      # Foam 笔记模板
└── .github/copilot-instructions.md
```

## 各部分说明

### one/c++ — C++ 课程

| 目录 | 内容 |
| --- | --- |
| `algorithm/Template/` | 算法模板：动态规划、图论、贪心、高精度、逆序对、排序、并查集 |
| `algorithm/solves/` | UVa 题解：11054、11134、11572、210 |
| `algorithm/bruteForce/` | 暴力枚举练习题（`solve*.cpp`、uva129、uva1354） |
| `algorithm/contest/` | 竞赛示例题 `exemple problem1`、`exemple problem2` |
| `dataStructure/` | 链表实现（`LinkList.h` / `LinkList.cpp`） |
| `experiment/` | 课程实验源码 `ex2.cpp`、`exp3.cpp`、`exp4.cpp` + 数据文件 |
| `example/` | 入门示例（`hello.c` 与 `Makefile`） |
| `report/` | 实验 1–8 报告（docx） |

编译单个文件（项目使用 MinGW g++，标准 `gnu++17`）：

```bash
g++ -std=gnu++17 one/c++/algorithm/solves/uva11054.cpp -o uva11054
```

### two/cpu — 硬件基础实践（Quartus）

两个 Quartus II 工程：

- `alu/` — 运算器实验：`alu.bdf` 顶层原理图，含 ALU、寄存器/寄存器堆、ROM/RAM（`lpm_*` 宏功能）、四节拍脉冲发生器、译码器等；`*.vwf` 为仿真波形，`*.mif` 为存储器初始化数据。
- `proj/` — CPU 整机：`CPU.bdf` 顶层，含微命令 `MC.bdf`、指令寄存器 `IR.bdf`、程序计数器 `PC.bdf`、`MY_ALU.vhd`（VHDL 实现的 ALU）等。

用 Quartus II 打开对应的 `.qpf` 工程文件即可编译与仿真；`top.pdf`、`proj.docx` 与各实验报告记录了设计与结果。

### two/Java — 面向对象程序设计实践

Maven 项目位于 `two/Java/Projects/Java/`：Java 24，依赖 JUnit 4.11（test）与 MySQL Connector/J 8.0.33（runtime）。

源码按主题分包：

- `com.example` — 文件 IO、多线程（`Runnable`）、生产者示例、基础语法与类设计练习
- `com.GUI` — Swing 界面练习（窗口、登录框、事件监听）
- `com.Server` — TCP Socket 编程：回声、简易聊天室（客户端 + 服务端，端口 8888）
- `com.Experiment` — 综合管理系统：封装/继承/多态 + 集合 + 对象序列化 + 多线程 + Socket + Swing 界面
- `com.Runnable_demo` / `com.Test` — 多线程猜数字游戏、类与封装基础练习

构建与运行：

```bash
mvn -f two/Java/Projects/Java/pom.xml compile
java -cp two/Java/Projects/Java/target/classes com.GUI.MyFirstFrame
```

`report/` 为实验报告与课程设计文档，`task/` 为课程任务书。

### two/javaweb — Web 开发技术

**实验（纯静态页面）**，直接用浏览器打开各目录下的 `index.html`：

| 目录 | 主题 |
| --- | --- |
| `lab01/` | 灯火良宵 · 中国元宵节 |
| `lab02/` | 人工智能+ · 智创未来全景科普 |
| `lab03/` | 图书管理平台 |
| `lab04/` | TechMart 数码商城（商品列表、商品详情、购物车） |

各实验目录同时保留了提交用的 `.zip` 压缩包。

**课程设计 `proj/` ——「中华文脉」传统文化网站**（Express + MySQL 全栈）：

- 技术栈：HTML5/CSS3/ES6、Node.js + Express、MySQL、express-session、svg-captcha、bcryptjs、ECharts
- 功能：响应式布局、注册（密码加密）、登录（SVG 验证码 + session）、权限控制（未登录仅可访问首页）、点赞/评论/转发、后台 ECharts 数据可视化
- 主要接口：`/api/captcha`、`/api/register`、`/api/login`、`/api/user`、`/api/articles`（列表/详情/点赞/评论/转发）、`/api/admin/stats`

运行步骤：

```bash
cd two/javaweb/proj
npm install
# 导入 database.sql 建库建表（mysql -u root -p < database.sql）
# 在 server.js 中改数据库账号密码（默认 host: localhost，database: chinese_culture）
npm start          # http://localhost:3000
```

默认管理员账号：`admin` / `admin123`。详细说明见 `two/javaweb/proj/README.md`。

### markdown — 笔记

- `final/` — 期末复习：英语作文模板、计算机组成原理、概率论
- `life/` — 生活杂记：MBTI、科目三（理论 + 路考）、AI 提示词、浮点数表示笔记
- `Temporary/` — 临时草稿

### certificate — 证书

`cacc.pdf` 与 CET-4 成绩单。

## 环境要求

- MinGW g++（C++17）
- JDK 24 + Maven（Java 项目）
- Node.js ≥ 14 + MySQL ≥ 5.7（Web 课程设计）
- Quartus II（FPGA 工程）

## 说明

- 仓库未包含 `.gitignore`，`two/javaweb/proj/node_modules/`、Maven `target/` 以及 Quartus 编译产物（`db/`、`incremental_db/`、`.sof`、`.pof` 等）均已提交，克隆体积较大。
- AI 编码代理的仓库指引见 `AGENTS.md`（`.github/copilot-instructions.md` 仅为指向该文件的入口）。
- 所有课程素材仅供学习使用，引用来源已在相应项目文档中注明。