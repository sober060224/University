# AGENTS.md

面向 AI 编码代理的仓库指引（DSH 等工具会加载项目根的 `AGENTS.md` / `CLAUDE.md`）。人类读者请看 `README.md`。

## 仓库性质

大学课程资料归档（大一 `one/`、大二 `two/`），**不是单一可构建工程**：没有根级构建脚本、没有 CI、没有测试框架。各部分相互独立 —— 改哪一部分，就只用管那部分的工具链，不要试图统一。

目录布局与各部分内容见 `README.md`，此处不重复。

## 改完必须跑的验证

| 改动对象 | 命令（本机已验证可用） |
| --- | --- |
| C++ 单文件 | `g++ -std=gnu++17 -fsyntax-only <file>` |
| Web 后端 | `node --check two/javaweb/proj/server.js` |
| 静态实验页 | 浏览器直接打开 `index.html`（无构建步骤） |
| Java 源码 | 见下方 Maven 说明 |
| 笔记 Markdown | 无校验工具，与现有文件风格保持一致即可 |

Java 构建：`mvn` **不在本机 PATH 上**，仓库也没有 `mvnw`。需要编译时用 IDE 内置 Maven，或先把 Maven 加入 PATH，再执行：

```bash
mvn -f two/Java/Projects/Java/pom.xml compile
```

## 陷阱（动手前先看）

- **笔记目录大小写不一致**：git 索引记录的是 `Markdown/`，磁盘上是 `markdown/`，`core.ignorecase=true` 掩盖了差异。新增或引用笔记文件时以 git 索引的 `Markdown/` 为准；不要做仅大小写的重命名，在大小写敏感的系统上会生成重复目录。
- **没有 `.gitignore`**：`two/javaweb/proj/node_modules/`、Maven `target/`、Quartus 产物（`db/`、`incremental_db/`、`*.rpt`、`*.sof`、`*.pof` 等）都已入库。不要顺手删除"看起来像编译产物"的文件。
- `.vscode/c_cpp_properties.json` 的 `compilerPath` 指向不存在的 `D:/AAA_Software/Code/mingw64/bin/g++.exe`；本机真正的 g++ 在 `D:/AAA_Software/Code/Environment/mingw64/bin/g++.exe`（11.2.0）。
- **Quartus 专有/二进制格式**（`.bdf`、`.qpf`、`.vwf`、`.qws`、`.bsf`）不要手工编辑或凭空生成，改动应在 Quartus 中完成；可安全编辑的文本是 `.vhd`、`.tdf`、`.mif`。
- 报告与文档为 docx/pptx/pdf，不要用文本编辑方式改写；文件名含课程、学号、姓名（如 `软件1244 陈伟生 202411701404.docx`），不要重命名。

## 约定

- 文档、注释、提交信息用中文，与仓库现状一致。
- Java 练习沿用现有 `com.<主题>` 包结构（`example` / `GUI` / `Server` / `Experiment` / `Runnable_demo` / `Test`），新代码放进已有包。
- `two/javaweb/proj/database.sql` 是库结构与初始数据的唯一来源；改表结构要同步 `server.js` 中的查询与接口。
- 不为课程作业引入新依赖、新构建工具，也不重构目录结构或统一代码风格；保持改动最小。