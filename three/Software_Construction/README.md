# 海洋生物多样性信息管理系统

面向广东海洋大学海洋与水产学科的科研、教学与科普需求，整合分散的物种数据，提供物种管理、观测记录、数据可视化报表与基于大模型的智能服务。

系统由 **Spring Boot 3.5.6 + Spring Security + JPA + MySQL 8** 后端和 **Vue 3 + Vite** 前端组成，前端构建产物已随后端 jar 一并打包，**单进程即可运行**。

系统原型报告见 [`report.md`](report.md)。

---

## 1 环境要求

| 项目 | 要求 |
| :--- | :--- |
| JDK | 17 及以上（开发验证使用 OpenJDK 24.0.2） |
| MySQL | 8.0 |
| Maven | 无需预装，项目自带 `mvnw` 包装器（Maven 3.9.9） |
| Node.js | 仅修改前端源码时需要（开发验证使用 v24.15.0） |

---

## 2 快速开始

### 2.1 启动 MySQL

Windows 下 MySQL 以服务方式运行，**需以管理员身份打开命令提示符或 PowerShell**：

```powershell
net start MySQL80
```

若服务未注册，可在「服务」管理面板中启动 `MySQL80`，或直接执行：

```powershell
net start MySQL80
```

Linux 下可执行 `service mysql start` 或 `systemctl start mysqld`。

### 2.2 导入数据库

```powershell
mysql -u root -p < database.sql
```

执行后按提示输入 MySQL 密码。

> **注意**：`database.sql` 会先 `DROP DATABASE IF EXISTS marine_biodiv` 再重建数据库，**每次导入都会清空原有数据**，请先备份。

脚本导入后包含：5 个用户、6 个生态系统、19 个物种、27 条观测记录、50 条观测—物种关联明细。其中 2 个物种设置为非公开，用于验证数据行级权限过滤效果。

如需修改数据库连接信息，编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/marine_biodiv?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:你的密码}
```

口令用环境变量 `DB_PASSWORD` 传入，配置里只保留默认值占位，不把真实口令写死在仓库中。

### 2.3 配置大模型 API Key（可选）

智能服务需要阿里云百炼（DashScope）的 API Key。编辑 `backend/src/main/resources/application.yml`：

```yaml
ai:
  api-key: ${AI_API_KEY:sk-你的Key}
```

或在启动前设置环境变量 `$env:AI_API_KEY = "sk-你的Key"`。Key 仅保存在服务端配置中，不会随前端产物下发。**未配置时系统其余功能完全正常**，仅图像识别、智能补全、智能问答、翻译等接口返回「未配置大模型 API Key」提示。

### 2.4 打包并运行

```powershell
cd backend
./mvnw clean package
java -jar target/marine-bio-backend-1.0.0.jar
```

启动成功后访问 <http://localhost:8088>。

Windows 下若 PowerShell 拒绝执行包装脚本，可改用 `.\mvnw.cmd` 或 `cmd /c mvnw.cmd`。

---

## 3 演示账号

| 角色 | 用户名 | 密码 | 可体验的功能 |
| :--- | :--- | :--- | :--- |
| 系统管理员 | `admin` | `admin123` | 全部功能，含用户审核、角色分配、日志追溯 |
| 科研人员 | `researcher` | `research123` | 物种与观测记录的新增、编辑（观测记录限本人）、全部统计 |
| 学生 | `student` | `student123` | 浏览物种与观测数据、查看统计与地图 |
| 公众 | `visitor` | `public123` | 浏览公开物种与分布地图、图像识别、翻译 |

---

## 4 开发模式

修改前端源码时采用前后端分离模式。后端 8088，前端 5173，Vite 开发服务器把 `/api` 代理到后端；浏览器视角下两者同源，会话 Cookie 与 CSRF 令牌正常工作，无需配置跨域。

```powershell
# 终端一：后端
cd backend
./mvnw spring-boot:run

# 终端二：前端
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5173>。

### 前端改动后同步到后端

前端产物已复制到后端静态资源目录，因此前端改动后需重新构建并拷贝：

```powershell
cd frontend
npm run build
Copy-Item -Path dist\* -Destination ..\backend\src\main\resources\static\ -Recurse -Force

cd ..\backend
./mvnw clean package
```

### 运行测试

```powershell
cd backend
./mvnw test
```

`SmokeTest` 会执行「管理员登录 → 新增物种 → 新建观测记录 → 读取综合数据看板 → 清理测试数据」的完整链路，结束时不留残留数据。

---

## 5 项目结构

```
three/Software_Contruction/
├── database.sql                 建库建表脚本 + 演示数据
├── report.md                    系统原型报告（角色设计、活动图、原型界面、数据库与接口文档）
├── 实验1--系统原型设计.docx      课程实验要求原件
├── 海洋生物多样性信息管理系统.pdf  课程需求文档原件
└── 《软件构造与体系结构实验》报告_系统原型_班级_学号_姓名.doc  报告排版模板
├── backend/                     Spring Boot 后端
│   ├── mvnw / mvnw.cmd          Maven 包装器
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/gdou/marinebio/
│       │   ├── common/          统一响应 Result、分页 PageResult、全局异常处理
│       │   ├── config/          SecurityConfig、WebConfig、CorsConfig
│       │   ├── entity/          8 个实体
│       │   ├── repository/      Spring Data JPA 仓储
│       │   ├── service/         业务层、LogService、AiService
│       │   ├── controller/      9 个控制器
│       │   └── ai/              DashScopeClient（大模型调用、超时重试）
│       └── resources/
│           ├── application.yml  端口、数据库、Session、AI、上传配置
│           └── static/          前端构建产物（打包后由 Spring Boot 提供）
└── frontend/                    Vue 3 前端源码
    ├── package.json
    ├── vite.config.js           5173 端口 + /api → 8088 代理
    └── src/
        ├── api/                 接口封装
        ├── router/              路由与角色守卫
        ├── stores/              Pinia 状态
        ├── views/               全部页面
        └── layout/              主框架
```

---

## 6 技术选型

| 层次 | 选型 |
| :--- | :--- |
| 后端框架 | Spring Boot 3.5.6 |
| 安全框架 | Spring Security 6.5（Session + CSRF + RBAC） |
| 持久层 | Spring Data JPA |
| 数据库 | MySQL 8.0 |
| 前端框架 | Vue 3.5 + Vite 8.3 + Pinia 2.3 |
| 图表 / 地图 | ECharts 6.1 / Leaflet 1.9 + OpenStreetMap |
| 智能服务 | 阿里云百炼 `qwen-vl-max`、`qwen-plus` |
| Excel 导出 | `write-excel-file` 4.1（浏览器端生成，无需服务端 POI） |

---

## 7 功能模块

| 模块 | 主要功能 |
| :--- | :--- |
| 模块一：用户与权限管理 | 注册申请、管理员审核、角色分配、密码重置、个人资料、操作日志追溯 |
| 模块二：物种信息管理 | 物种增删改查、六级分类阶元、多条件组合筛选、图片上传、Excel 导出 |
| 模块三：生态系统与观测记录 | 生态系统管理、观测记录增删改查、**一次观测关联多个物种**（数量与行为）、归属校验 |
| 模块四：数据可视化与报表 | 综合数据看板、六组统计图表、物种与观测分布地图、Excel 导出、PDF 打印 |
| 模块五：智能服务 | 图像识别与物种鉴定、物种信息智能补全、观测智能标签与异常检测、智能问答、多语言翻译 |

角色与功能点权限详见 `report.md` 第 3 章的权限矩阵。

---

## 8 常见问题

**Q：启动报 `Access denied for user`。**
A：`application.yml` 中的 `spring.datasource.username` / `password` 与实际 MySQL 账号不一致，或该账号没有 `marine_biodiv` 库的权限。

**Q：启动报 `Unknown database 'marine_biodiv'`。**
A：尚未导入 `database.sql`，先执行第 2.2 步。

**Q：访问 8088 端口被占用。**
A：本项目固定使用 8088 端口。若端口被其他程序占用，请先释放，或修改 `application.yml` 中的 `server.port` 并同步修改 `frontend/vite.config.js` 的代理目标。

**Q：智能服务提示「未配置大模型 API Key」。**
A：按第 2.3 步配置 `ai.api-key`。这是预期行为，不影响其他功能。

**Q：接口返回 401 或 403。**
A：401 表示会话失效或未登录，前端会自动跳转登录页；403 表示当前角色无权执行该操作，属权限控制的正常表现。

**Q：CSRF 校验失败。**
A：确认通过 `http://localhost:8088` 访问而非直接双击打开 HTML 文件。CSRF 令牌依赖同源的 Cookie，跨源访问会校验失败。

**Q：端口 8080 而不是 8088？**
A：本机 8080 端口被其他程序占用，故统一改为 8088，数据库连接配置不涉及端口修改。

---

## 9 报告转 PDF

`report.md` 使用 Markdown 编写，含 Mermaid 图表。转换为 PDF 有两种方式：

1. **VS Code**：安装 Markdown PDF 插件或 Markdown Preview Enhanced 插件，直接导出。Mermaid 需在预览中渲染后再导出。
2. **Typora**：打开文件后使用「文件 → 导出 → PDF」，Mermaid 图表会自动渲染。

导出后请按 `report.md` 附录 A 的对照表统一排版格式（标题黑体小四、正文宋体小四、行距 1.25 倍、正文首行缩进 2 字符、图题在图下、表题在表上并居中），再替换封面与项目成员表中的占位符，并按第 5 章的图号提示插入界面截图。
