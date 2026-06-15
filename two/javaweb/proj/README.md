# 中华文脉 - 中华优秀传统文化网站

Web开发技术课程设计项目

## 项目简介

本项目是一个以"中华优秀传统文化"为主题的数字化平台，网站定名为"中华文脉"。平台包含五大栏目：经典典籍、非遗传承、翰墨丹青、礼乐风雅、传统科技，为用户提供一个可阅读、可互动、可分享的传统文化学习空间。

## 技术栈

- **前端**：HTML5 + CSS3 + JavaScript (ES6)
- **后端**：Node.js + Express
- **数据库**：MySQL
- **验证码**：svg-captcha
- **鉴权**：express-session
- **数据可视化**：ECharts

## 功能特性

✅ **响应式设计** - 适配手机、平板、PC等不同屏幕尺寸

✅ **用户注册** - 连接MySQL数据库，密码使用bcrypt加密存储

✅ **用户登录** - SVG验证码验证，数据库用户信息验证，session管理登录状态

✅ **权限控制** - 未登录用户只能访问首页，访问子页自动跳转登录

✅ **互动功能** - 注册用户可对内容点赞、评论、转发

✅ **后台管理** - 管理员可查看所有子页的点赞、评论数据，ECharts可视化展示

## 环境要求

- Node.js >= 14.0.0
- MySQL >= 5.7
- npm 或 yarn

## 安装与运行

### 1. 克隆项目

```bash
cd web-project
```

### 2. 安装依赖

```bash
npm install
```

### 3. 配置数据库

#### 方法一：使用MySQL命令行

```bash
# 登录MySQL
mysql -u root -p

# 执行数据库脚本
source database.sql
```

#### 方法二：使用图形化工具（如Navicat）

直接导入 `database.sql` 文件即可。

### 4. 修改数据库配置

编辑 `server.js` 文件中的数据库连接配置：

```javascript
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',      // 修改为你的MySQL用户名
    password: '',      // 修改为你的MySQL密码
    database: 'chinese_culture',
    // ...
});
```

### 5. 启动项目

```bash
npm start
```

### 6. 访问网站

打开浏览器访问：http://localhost:3000

## 默认账号

- **管理员账号**：admin / admin123

## 项目结构

```
web-project/
├── public/                 # 前端静态资源
│   ├── css/
│   │   └── style.css      # 主样式文件
│   ├── js/
│   │   └── main.js        # 前端主脚本
│   ├── images/            # 图片资源
│   ├── index.html         # 首页
│   ├── register.html      # 注册页
│   ├── login.html         # 登录页
│   ├── classics.html      # 经典典籍栏目
│   ├── ich.html           # 非遗传承栏目
│   ├── art.html           # 翰墨丹青栏目
│   ├── music.html         # 礼乐风雅栏目
│   ├── tech.html          # 传统科技栏目
│   ├── article.html       # 文章详情页
│   └── admin.html         # 后台管理页
├── server.js              # 后端主文件
├── database.sql           # 数据库脚本
├── package.json           # 项目配置
└── README.md              # 说明文档
```

## API接口说明

### 认证相关
- `GET /api/captcha` - 获取验证码
- `POST /api/register` - 用户注册
- `POST /api/login` - 用户登录
- `POST /api/logout` - 用户登出
- `GET /api/user` - 获取当前用户信息

### 文章相关
- `GET /api/articles` - 获取文章列表
- `GET /api/articles/:id` - 获取文章详情
- `POST /api/articles/:id/like` - 点赞/取消点赞
- `POST /api/articles/:id/comment` - 发表评论
- `GET /api/articles/:id/comments` - 获取评论列表
- `POST /api/articles/:id/share` - 转发文章

### 管理后台
- `GET /api/admin/stats` - 获取统计数据（需管理员权限）

## 注意事项

1. 请确保MySQL服务已启动
2. 首次运行前请先执行数据库脚本创建表结构
3. 生产环境请修改session secret为随机字符串
4. 所有外部素材均已注明来源，仅供学习使用

## 素材来源说明

- 典籍文本：中国哲学书电子化计划（ctext.org）
- 图片素材：故宫博物院官网（www.dpm.org.cn）
- 参考资料：中国非物质文化遗产网

## 开发说明

本项目为《Web开发技术》课程设计作业，完整实现了课程要求的所有功能：
- ✅ 响应式页面设计
- ✅ 注册功能（数据库连接、密码加密）
- ✅ 登录功能（验证码、session管理）
- ✅ 权限控制（子页屏蔽）
- ✅ 点赞、评论、转发功能
- ✅ 后台数据可视化（ECharts）
