# 海洋生物多样性信息管理系统

面向广东海洋大学海洋与水产学科的科研、教学与科普需求，整合分散的物种数据，提供物种管理、观测记录、数据可视化报表与基于大模型的智能服务。

系统由 **Spring Boot 3.5.6 + Spring Security + JPA + MySQL 8** 后端和 **Vue 3 + Vite** 前端组成，前端构建产物已随后端 jar 一并打包，**单进程即可运行**。

系统原型报告见 [`report.md`](lab01/report.md)。

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
    url: jdbc:mysql://localhost:3306/marine_biodiv?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

`characterEncoding` 必须写 Java 字符集名 `UTF-8`，**不能写成 `utf8mb4`**：驱动不认这个名字，
会直接抛 `Unsupported character encoding`；写成 `utf8` 则被当作 `utf8mb3`，
插入 emoji 与部分生僻字时会失败。`allowPublicKeyRetrieval=true` 与 `useSSL=false` 是一对，
MySQL 8 默认使用 `caching_sha2_password`，明文连接下客户端要去服务端取 RSA 公钥，缺这个开关会连不上。

口令只从环境变量 `DB_PASSWORD` 读取，仓库里不留任何默认值；未设置时是空串，
连接会以 MySQL 原生的 `Access denied` 失败（对应下面第 8 节的排查条目），
而不会在别人机器上意外连上某个库。

口令用环境变量 `DB_PASSWORD` 传入，配置里只保留默认值占位，不把真实口令写死在仓库中。

### 2.3 配置大模型 API Key（可选）

智能服务需要大模型服务的 API Key（OpenAI 兼容端点，默认指向阿里云百炼兼容模式）。编辑 `backend/src/main/resources/application.yml`：

```yaml
ai:
  api-key: ${AI_API_KEY:sk-请填写}
```

或在启动前设置环境变量 `$env:AI_API_KEY = "sk-你的Key"`。Key 仅保存在服务端配置中，不会随前端产物下发。**未配置时系统其余功能完全正常**，仅图像识别、智能补全、智能问答、翻译等接口返回「未配置大模型 API Key」提示。

> Windows 上 `$env:AI_API_KEY` 只对**当前那个终端**有效，从别的终端或 IDE 起的进程读不到。
> 想让它持久，用 `setx AI_API_KEY "sk-你的Key"`，然后**重开一个终端**再启动应用（`setx` 不影响已存在的
> 进程环境）。代价是它会以明文写进注册表 `HKCU\Environment`，要清掉就执行
> `reg delete "HKCU\Environment" /v AI_API_KEY /f`。

`sk-请填写` 这个占位值不是随手写的示例：`DashScopeClient.isConfigured()` 正是按这个前缀判断「还没配 Key」，
从而给出中文提示而不是拿占位值去请求。请把 Key 配成别的内容（或留这个占位值不配），
不要填 `sk-你的Key` 之类的字面量——那会被当成已配置，然后收到一个 401。

#### 模型与端点怎么换

`ai.base-url` / `ai.vision-model` / `ai.text-model` 三个值决定实际调用哪个模型，当前都是
`deepseek-v4.1-flash`（它同时支持 `image_url` 输入，识别与文本共用一个型号；换回阿里原生档位就填
`qwen-vl-max` / `qwen-plus`）。注意 `application.yml` 是打进 jar 的，改完这三个值或 `retry-deadline-ms`
之后要重新 `./mvnw clean package`，只重启不会生效；临时试值可以用命令行覆盖，例如
`java -jar target/marine-bio-backend-1.0.0.jar --ai.text-model=qwen-plus`。

配好之后可以直接验五条能力真的通（不需要任何额外依赖，浏览器登录后界面操作，或用 `curl` 带会话调用
`/api/ai/identify`、`/complete`、`/analyze`、`/ask`、`/translate`），调用结果连同耗时、模型名与成功与否
都会写进 `ai_records`，界面上「AI 调用记录」页能看到。图像识别只接受本页面上传后返回的
`/uploads/...` 路径，直接给一个公网地址会被拒。

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

> ⚠️ **仅供本机演示，部署到任何可被外部访问的环境前必须先处理掉这些口令。**
> 下表里的明文口令同时印在登录页上（`frontend/src/views/LoginView.vue` 的「演示账号」区块，
> 且已包含在打包产物里），而自助注册只允许申请学生与公众两种身份——
> 也就是说，**这一页是拿到管理员身份的唯一途径**，任何看到登录页的人都能直接以
> `admin` 登录，进而删除物种、审核账号、重置他人密码。
> 课程演示需要保留这层便利，但真要放到公网或校园网上，请至少做完以下三件事：
> 删掉登录页的演示账号区块并重新构建前端、改掉 `database.sql` 里四个账号的 BCrypt 密文、
> 给管理员换一个不在任何文档里出现的强口令。

| 角色 | 用户名 | 密码 | 可体验的功能 |
| :--- | :--- | :--- | :--- |
| 系统管理员 | `admin` | `admin123` | 全部功能，含用户审核、角色分配、日志追溯 |
| 科研人员 | `researcher` | `research123` | 物种与观测记录的新增、编辑（观测记录限本人）、全部统计 |
| 学生 | `student` | `student123` | 浏览物种与观测数据、查看统计与地图 |
| 公众 | `visitor` | `public123` | 浏览公开物种与分布地图、图像识别、翻译 |

公众账号看不到综合看板、统计分析与智能问答——权限矩阵（`report.md` 表 3-2）把这三项对公众标为 ×，
后端 `SecurityConfig` 与前端路由守卫都照此拦截，导航菜单里也不会出现这三项入口。

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

`SmokeTest` 会执行「管理员登录 → 新增物种 → 新建观测记录 → 读取综合数据看板 → 清理测试数据」的完整链路，结束时不留残留数据。**它需要本机 MySQL 已启动并已导入 `database.sql`**，否则会在建立数据源时失败（`Communications link failure`）。

另外五个测试类不需要数据库，随时可以直接跑：

```powershell
./mvnw test -Dtest=AuthorizationMatrixTest,AdminGuardTest,DataVisibilityTest,PageResultTest,JpqlBootstrapTest
```

- `AuthorizationMatrixTest` 是报告 表 3-2 的可执行版本：装载真实的 `SecurityConfig` 过滤器链，
  业务 Bean 全部 mock，逐个断言「这个 URL + 这个方法 + 这个角色，鉴权放不放行」。
  `SecurityConfig` 的规则是「先注册先生效」的顺序敏感列表，这类越权在编译期发现不了，
  而 `SmokeTest` 全程用 admin 登录，admin 对所有 URL 都放行，所以「公众也能进看板」这种回归
  它永远测不到。
- `AdminGuardTest` 盯的是「系统至少保留一名在岗管理员」这条保命规则：
  它失手的后果不是报错而是后台永久锁死，而改角色与审核注册申请两条路都能改角色。
- `DataVisibilityTest` 盯的是数据行级过滤与几处「静默失败」：门类下拉的 `is_public` 过滤、
  看板对角色下发哪些统计键、模块五审计日志的超长截断、置信度夹取，以及请求体的取值范围。
  其中一条断言专门盯着校验提示：`@Size` 不写 `message` 时会退回框架默认的「个数必须在0和N之间」,
  既不指名字段、又把「个数」用在字符串长度上，而全局异常处理会把多个字段的消息拼成一句返回。
  另有两条钉住智能服务的行为：片段兜底命中时上下文必须写明「这条是用哪个片段模糊搜来的」
  （否则问「中国鲎」会把「中国对虾」当依据交出去，实测复现过），以及大模型调用失败时
  `ai_records` 必须留下 `success=false` 的那一行——原先五个能力的 `success` 一律硬编码为 `true`
  且只在成功时写日志，这张「质量分析」表于是永远看不到失败。
- `PageResultTest` 钉住分页契约：接口页码是 **0 基**（报告 表 8-1 原来写成「从 1 开始」，
  照文档对接的人会整整错开一页，已改）、负数与越界参数被归一化而不是抛 500、
  `size` 按上限夹值、响应体回显的 `page` / `total` / `totalPages` 三者自洽。

- `JpqlBootstrapTest` 把方言钉死并关掉 JDBC 元数据访问，于是**不连数据库也能完成上下文装配**，
  从而让 Hibernate 把每一条 `@Query` 里的 JPQL 真正解析一遍。JPQL 写错属性名编译期不报错，
  而没有数据库时又谁都不会去执行它，这个空档正好盖住了新增的那几条反向匹配查询。
  它验证的是「能否解析」，不验证「结果是否正确」。

这些断言做过变异测试校验，确认它们真的会失败：把统计规则退回 `authenticated()`、
把 `/api/upload` 退回只放行科研人员、去掉本人资料那条规则、关掉审核路径上的管理员判据、
把管理员判据换回旧的「按条数算」、让门类下拉不过滤、去掉 `input_text` 的截断、
把登录状态判断挪回口令校验之前、删掉长度校验的中文 message、往任一 JPQL 里塞一个不存在的属性名、
把兜底检索那句「这是模糊搜来的」标注删掉、把失败时的 `success=false` 记录改回只在成功时写日志、
把 `ObservationService.search` 里的物种可见性预检查去掉（改成直接查观测表）、
把 `listLinkedSpecies` 的 `speciesId` 换回不判角色的写法——
对应测试都会红。跑完即恢复原状。

其中最后两条是本轮实测时顺手验的：删掉标注后 `fallbackHitIsDisclosedToTheModel` 立刻红，
而在标注写成「替换掉数据而不是标在数据之前」的中间版本里，它同样以断言失败的形式报了回来
（那一版会把模糊命中的物种整段丢掉，属于自己引入的回归，被测试抓到）。
另有一条**测不出来**的缺陷值得记住教训：`accessDeniedHandler` 用 `request.getUserPrincipal()`
判角色，真机上已登录用户令牌过期会回 401 被踢下线，但 MockMvc 的 `authentication()` 
会顺手把 `request.userPrincipal` 设好，所以这条只有打真实 HTTP 才能暴露——新用例改用
`SecurityContextHolder` 判据后，同一个用例在测试与真机上才一致。

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
│       │   ├── config/          SecurityConfig、SessionFreshnessFilter、WebConfig
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
        ├── store/               Pinia 状态
        ├── views/               全部页面
        ├── components/          图表、分页等复用组件
        └── layouts/             主框架
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
| 智能服务 | OpenAI 兼容端点 + `deepseek-v4.1-flash`（识别与文本共用，端点与型号见 2.3 节） |
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

**Q：公众账号点「综合看板」「统计分析」「智能问答」没有反应，或提示没有权限。**
A：这是预期行为。权限矩阵（`report.md` 表 3-2）把这三项对公众标为 ×，后端 `SecurityConfig` 与前端路由守卫都按此拦截，导航菜单里也不会出现这三个入口。公众可以正常浏览公开物种、分布地图，并使用图像识别与多语言翻译。

**Q：登录时提示「请通过本页面的上传功能选择图片后再识别」之类的说法从哪来。**
A：图像识别只接受本系统 `/uploads/` 目录里的图片，不再接受任意公网地址，详见 `report.md` 3.3 节与 `DashScopeClient` 的注释。

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

## 9 安全边界与已知限制

本课程原型只在单机 `http://localhost:8088` 上运行，以下限制是**按这个前提刻意做出的取舍**，
不是遗漏；换成公网或校园网部署时必须重新评估。

| 限制 | 说明 | 真要上线时怎么处理 |
| :--- | :--- | :--- |
| 演示账号口令公开 | 登录页印有四个账号的明文口令，且自助注册只能申请学生 / 公众，所以它是拿到管理员身份的唯一途径 | 删除 `LoginView.vue` 的演示账号区块并重新构建前端，替换 `database.sql` 里的 BCrypt 密文 |
| 登录接口不限流、不锁定 | 没有失败次数限制与验证码，口令可以在线反复尝试。BCrypt 单次校验约几十毫秒，算是一道天然减速带，但挡不住持续爆破 | 加按用户名 + 来源 IP 的失败计数与退避，或挂在前端网关之后由网关限流 |
| 注册接口会暴露用户名占用 | `POST /api/auth/register` 对已存在的用户名直接回「用户名「xxx」已被占用」，这是不需要口令的探测面。登录一侧已经修好（状态判断移到口令校验之后，两种失败对外同一条消息），注册一侧保留是为了课程演示的可用性 | 统一成一句不含信息的提示，或在注册前加图形验证码 |
| 会话 Cookie 未加 `Secure` | 站点是明文 HTTP，加了 `Secure` 浏览器会直接丢弃会话 Cookie，登录立即失效。已设置 `SameSite=Lax` 与 `HttpOnly` | 换成 HTTPS 后在 `application.yml` 的 `server.servlet.session.cookie` 下补 `secure: true` |
| 上传目录是相对路径 | `app.upload-dir: ./uploads` 相对于 jar 的启动目录，谁都能启动，磁盘占用也没有按用户配额限制 | 改为独立的绝对路径并放在 Web 根目录之外，加容量水位告警 |
| 图片按公开资源对待 | `/uploads/**` 不校验登录，因为公开的物种详情页要显示图片（矩阵中「浏览公开物种信息」对公众是 √） | 若物种图片本身需要保密，改为签名 URL 或按角色校验后再放行 |
| 物种图片地址不做同源限制 | 物种的 `imageUrl` / `videoUrl` 是自由文本，`videoUrl` 只放行 http(s) 协议才允许生成可点击链接，但仍可指向外部站点 | 只允许 `/uploads/` 前缀，或加外链白名单 |
| 智能服务的延迟由外部模型决定 | 实测（真 Key + `deepseek-v4.1-flash`）图像识别 5.0～14.4 秒、补全最长 56.7 秒，报告「图像识别 < 5 秒」这一条**并未稳定达成**；调用是非流式的，也没有「首字」指标可言。重试受 `ai.retry-deadline-ms`（50 秒）总时限约束，必须小于前端 axios 的 60 秒 | 换不产生思考链的模型、给请求加 `max_tokens`，或改为 SSE 流式转发并让 `ChatView` 逐字追加 |
| 大模型失败只留日志、不降级 | 上游 4xx / 超时会在 `ai_records` 写下 `success=false` 与原因并向前端报一句中文，但不会退回「本地拼一个答案」，识别与问答在外部服务不可用时就是不可用 | 按能力分别设降级：识别退回候选列表，问答退回纯检索结果拼装 |
| 删除生态系统会级联删除观测记录 | `observations.ecosystem_id` 的外键是 `ON DELETE CASCADE`，界面删除时会明确二次确认并写操作日志 | 改为 `RESTRICT` 并在服务层先检查影响面 |

另外，导出 Excel / PDF 完全在浏览器端生成（`write-excel-file` + 打印样式），服务端没有对应的导出接口。
按钮现已按 表 3-2 收口（`store.canEdit`，即只给管理员与科研人员渲染），但这**只是界面可达性、不是访问控制**：
任何能读到列表数据的角色，理论上都能把读到的那部分自行存档（浏览器开发者工具里改一下 `v-if` 即可）。
真正要挡住这件事只能在服务端做——按角色裁剪接口返回的字段，或者提供带鉴权的导出接口。

---

## 10 报告转 PDF

`report.md` 使用 Markdown 编写，含 Mermaid 图表。转换为 PDF 有两种方式：

1. **VS Code**：安装 Markdown PDF 插件或 Markdown Preview Enhanced 插件，直接导出。Mermaid 需在预览中渲染后再导出。
2. **Typora**：打开文件后使用「文件 → 导出 → PDF」，Mermaid 图表会自动渲染。

导出后请按 `report.md` 附录 A 的对照表统一排版格式（标题黑体小四、正文宋体小四、行距 1.25 倍、正文首行缩进 2 字符、图题在图下、表题在表上并居中），再替换封面与项目成员表中的占位符，并按第 5 章的图号提示插入界面截图。
