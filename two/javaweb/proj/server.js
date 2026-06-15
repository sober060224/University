const express = require('express');
const mysql = require('mysql2/promise');
const svgCaptcha = require('svg-captcha');
const session = require('express-session');
const bcrypt = require('bcryptjs');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = 3000;

// 中间件
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Session配置
app.use(session({
    secret: 'chinese-culture-secret-key-2026',
    resave: false,
    saveUninitialized: false,
    cookie: {
        maxAge: 24 * 60 * 60 * 1000, // 24小时
        httpOnly: true
    }
}));

// 数据库连接池配置
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: 'Chenweisheng.1',
    database: 'chinese_culture',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// 测试数据库连接
async function testDbConnection() {
    try {
        const connection = await pool.getConnection();
        console.log('数据库连接成功！');
        connection.release();
    } catch (err) {
        console.log('数据库连接失败，请检查配置:', err.message);
        console.log('请先执行 database.sql 创建数据库和表结构');
    }
}
testDbConnection();

// 权限验证中间件
function authMiddleware(req, res, next) {
    if (!req.session.user) {
        return res.status(401).json({ success: false, message: '请先登录' });
    }
    next();
}

// 管理员权限验证中间件
function adminMiddleware(req, res, next) {
    if (!req.session.user || !req.session.user.isAdmin) {
        return res.status(403).json({ success: false, message: '需要管理员权限' });
    }
    next();
}

// ========== 验证码接口 ==========
app.get('/api/captcha', (req, res) => {
    const captcha = svgCaptcha.create({
        size: 4,
        noise: 2,
        color: true,
        background: '#fbf7f0'
    });
    req.session.captcha = captcha.text.toLowerCase();
    res.type('svg');
    res.send(captcha.data);
});

// ========== 注册接口 ==========
app.post('/api/register', async (req, res) => {
    const { username, email, password, captcha } = req.body;

    // 验证验证码
    if (!captcha || captcha.toLowerCase() !== req.session.captcha) {
        return res.json({ success: false, message: '验证码错误' });
    }

    if (!username || !email || !password) {
        return res.json({ success: false, message: '请填写完整信息' });
    }

    try {
        // 检查用户名是否已存在
        const [users] = await pool.query('SELECT id FROM users WHERE username = ? OR email = ?', [username, email]);
        if (users.length > 0) {
            return res.json({ success: false, message: '用户名或邮箱已存在' });
        }

        // 密码加密
        const passwordHash = await bcrypt.hash(password, 10);

        // 插入用户
        await pool.query(
            'INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)',
            [username, email, passwordHash]
        );

        res.json({ success: true, message: '注册成功，请登录' });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '注册失败，请重试' });
    }
});

// ========== 登录接口 ==========
app.post('/api/login', async (req, res) => {
    const { username, password, captcha } = req.body;

    // 验证验证码
    if (!captcha || captcha.toLowerCase() !== req.session.captcha) {
        return res.json({ success: false, message: '验证码错误' });
    }

    try {
        const [users] = await pool.query('SELECT * FROM users WHERE username = ?', [username]);
        if (users.length === 0) {
            return res.json({ success: false, message: '用户名或密码错误' });
        }

        const user = users[0];
        const validPassword = await bcrypt.compare(password, user.password_hash);
        
        if (!validPassword) {
            return res.json({ success: false, message: '用户名或密码错误' });
        }

        // 设置session
        req.session.user = {
            id: user.id,
            username: user.username,
            isAdmin: user.is_admin === 1
        };

        res.json({ 
            success: true, 
            message: '登录成功',
            user: {
                id: user.id,
                username: user.username,
                isAdmin: user.is_admin === 1
            }
        });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '登录失败，请重试' });
    }
});

// ========== 登出接口 ==========
app.post('/api/logout', (req, res) => {
    req.session.destroy();
    res.json({ success: true, message: '已退出登录' });
});

// ========== 获取当前用户信息 ==========
app.get('/api/user', (req, res) => {
    if (req.session.user) {
        res.json({ success: true, user: req.session.user });
    } else {
        res.json({ success: false, message: '未登录' });
    }
});

// ========== 获取文章列表 ==========
app.get('/api/articles', async (req, res) => {
    const { category } = req.query;
    let sql = 'SELECT id, title, category, create_time FROM articles';
    let params = [];
    
    if (category) {
        sql += ' WHERE category = ?';
        params.push(category);
    }

    try {
        const [articles] = await pool.query(sql, params);
        res.json({ success: true, articles });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '获取文章失败' });
    }
});

// ========== 获取文章详情 ==========
app.get('/api/articles/:id', async (req, res) => {
    const { id } = req.params;
    try {
        const [articles] = await pool.query('SELECT * FROM articles WHERE id = ?', [id]);
        if (articles.length === 0) {
            return res.json({ success: false, message: '文章不存在' });
        }

        // 获取点赞数
        const [likes] = await pool.query('SELECT COUNT(*) as count FROM likes WHERE article_id = ?', [id]);
        
        // 获取评论数
        const [comments] = await pool.query('SELECT COUNT(*) as count FROM comments WHERE article_id = ?', [id]);

        // 检查当前用户是否已点赞
        let isLiked = false;
        if (req.session.user) {
            const [userLikes] = await pool.query(
                'SELECT id FROM likes WHERE user_id = ? AND article_id = ?',
                [req.session.user.id, id]
            );
            isLiked = userLikes.length > 0;
        }

        res.json({ 
            success: true, 
            article: articles[0],
            likeCount: likes[0].count,
            commentCount: comments[0].count,
            isLiked
        });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '获取文章失败' });
    }
});

// ========== 点赞接口 ==========
app.post('/api/articles/:id/like', authMiddleware, async (req, res) => {
    const articleId = req.params.id;
    const userId = req.session.user.id;

    try {
        // 检查是否已点赞
        const [existing] = await pool.query(
            'SELECT id FROM likes WHERE user_id = ? AND article_id = ?',
            [userId, articleId]
        );

        if (existing.length > 0) {
            // 取消点赞
            await pool.query('DELETE FROM likes WHERE user_id = ? AND article_id = ?', [userId, articleId]);
            res.json({ success: true, liked: false, message: '已取消点赞' });
        } else {
            // 添加点赞
            await pool.query('INSERT INTO likes (user_id, article_id) VALUES (?, ?)', [userId, articleId]);
            res.json({ success: true, liked: true, message: '点赞成功' });
        }
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '操作失败' });
    }
});

// ========== 评论接口 ==========
app.post('/api/articles/:id/comment', authMiddleware, async (req, res) => {
    const articleId = req.params.id;
    const userId = req.session.user.id;
    const { content } = req.body;

    if (!content || content.trim() === '') {
        return res.json({ success: false, message: '评论内容不能为空' });
    }

    try {
        await pool.query(
            'INSERT INTO comments (user_id, article_id, content) VALUES (?, ?, ?)',
            [userId, articleId, content]
        );

        res.json({ success: true, message: '评论成功', comment: {
            username: req.session.user.username,
            content,
            create_time: new Date()
        }});
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '评论失败' });
    }
});

// ========== 获取评论列表 ==========
app.get('/api/articles/:id/comments', async (req, res) => {
    const articleId = req.params.id;
    try {
        const [comments] = await pool.query(`
            SELECT c.*, u.username 
            FROM comments c 
            JOIN users u ON c.user_id = u.id 
            WHERE c.article_id = ? 
            ORDER BY c.create_time DESC
        `, [articleId]);

        res.json({ success: true, comments });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '获取评论失败' });
    }
});

// ========== 转发接口 ==========
app.post('/api/articles/:id/share', authMiddleware, async (req, res) => {
    const articleId = req.params.id;
    const userId = req.session.user.id;

    try {
        await pool.query(
            'INSERT INTO shares (user_id, article_id) VALUES (?, ?)',
            [userId, articleId]
        );
        res.json({ success: true, message: '转发记录已保存' });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '操作失败' });
    }
});

// ========== 后台统计数据 ==========
app.get('/api/admin/stats', adminMiddleware, async (req, res) => {
    try {
        // 总统计
        const [totalLikes] = await pool.query('SELECT COUNT(*) as count FROM likes');
        const [totalComments] = await pool.query('SELECT COUNT(*) as count FROM comments');
        const [totalShares] = await pool.query('SELECT COUNT(*) as count FROM shares');
        const [totalUsers] = await pool.query('SELECT COUNT(*) as count FROM users');

        // 各分类统计
        const [categoryStats] = await pool.query(`
            SELECT 
                a.category,
                COUNT(DISTINCT a.id) as article_count,
                COUNT(DISTINCT l.id) as like_count,
                COUNT(DISTINCT c.id) as comment_count
            FROM articles a
            LEFT JOIN likes l ON a.id = l.article_id
            LEFT JOIN comments c ON a.id = c.article_id
            GROUP BY a.category
        `);

        // 热门文章TOP5
        const [topArticles] = await pool.query(`
            SELECT 
                a.id,
                a.title,
                COUNT(DISTINCT l.id) as likes,
                COUNT(DISTINCT c.id) as comments
            FROM articles a
            LEFT JOIN likes l ON a.id = l.article_id
            LEFT JOIN comments c ON a.id = c.article_id
            GROUP BY a.id
            ORDER BY (likes + comments) DESC
            LIMIT 5
        `);

        res.json({
            success: true,
            stats: {
                totalLikes: totalLikes[0].count,
                totalComments: totalComments[0].count,
                totalShares: totalShares[0].count,
                totalUsers: totalUsers[0].count
            },
            categoryStats,
            topArticles
        });
    } catch (err) {
        console.error(err);
        res.json({ success: false, message: '获取统计数据失败' });
    }
});

// 子页面权限检查中间件 - 重定向到登录页
app.get(['/classics.html', '/ich.html', '/art.html', '/music.html', '/tech.html', '/article.html', '/admin.html'], (req, res, next) => {
    if (!req.session.user) {
        return res.redirect('/login.html?redirect=' + req.path);
    }
    // 管理员页面额外检查
    if (req.path === '/admin.html' && !req.session.user.isAdmin) {
        return res.redirect('/index.html');
    }
    next();
});

// 启动服务器
app.listen(PORT, () => {
    console.log(`
========================================
  中华文脉网站已启动！
  访问地址: http://localhost:${PORT}
  
  配置说明:
  1. 请先配置 MySQL 数据库连接(server.js)
  2. 执行 database.sql 创建数据库表
  3. 默认管理员账号: admin / admin123
========================================
    `);
});
