// 中华文脉网站 - 前端主脚本

// 全局状态
let currentUser = null;

// DOM加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    initNavigation();
    initCarousel();
    checkLoginStatus();
    initPageSpecific();
});

// 导航栏功能
function initNavigation() {
    // 滚动效果
    window.addEventListener('scroll', function() {
        const navbar = document.querySelector('.navbar');
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });

    // 汉堡菜单
    const hamburger = document.querySelector('.hamburger');
    const navMenu = document.querySelector('.nav-menu');
    
    if (hamburger) {
        hamburger.addEventListener('click', function() {
            navMenu.classList.toggle('active');
        });
    }
}

// 轮播图功能
function initCarousel() {
    const slides = document.querySelectorAll('.carousel-slide');
    const dots = document.querySelectorAll('.dot');
    
    if (slides.length === 0) return;

    let currentSlide = 0;

    function showSlide(index) {
        slides.forEach((slide, i) => {
            slide.classList.toggle('active', i === index);
        });
        dots.forEach((dot, i) => {
            dot.classList.toggle('active', i === index);
        });
    }

    function nextSlide() {
        currentSlide = (currentSlide + 1) % slides.length;
        showSlide(currentSlide);
    }

    // 自动播放
    setInterval(nextSlide, 5000);

    // 点击圆点切换
    dots.forEach((dot, index) => {
        dot.addEventListener('click', () => {
            currentSlide = index;
            showSlide(currentSlide);
        });
    });
}

// 检查登录状态
async function checkLoginStatus() {
    try {
        const response = await fetch('/api/user');
        const data = await response.json();
        
        if (data.success) {
            currentUser = data.user;
            updateAuthUI();
        }
    } catch (err) {
        console.log('未登录');
    }
}

// 更新认证UI
function updateAuthUI() {
    const authButtons = document.querySelector('.auth-buttons');
    if (!authButtons) return;

    if (currentUser) {
        let adminLink = '';
        if (currentUser.isAdmin) {
            adminLink = '<a href="/admin.html" class="btn btn-outline">管理后台</a>';
        }
        
        authButtons.innerHTML = `
            <div class="user-info">
                <span class="username">欢迎，${currentUser.username}</span>
                ${adminLink}
                <button class="btn btn-outline" onclick="logout()">退出</button>
            </div>
        `;
    }
}

// 登出功能
async function logout() {
    try {
        await fetch('/api/logout', { method: 'POST' });
        currentUser = null;
        window.location.href = '/index.html';
    } catch (err) {
        console.error('登出失败', err);
    }
}

// 刷新验证码
function refreshCaptcha() {
    const captchaImg = document.getElementById('captchaImage');
    if (captchaImg) {
        captchaImg.src = '/api/captcha?' + Date.now();
    }
}

// 显示消息
function showMessage(elementId, message, isSuccess) {
    const msgEl = document.getElementById(elementId);
    if (msgEl) {
        msgEl.textContent = message;
        msgEl.className = 'message ' + (isSuccess ? 'success' : 'error');
        setTimeout(() => {
            msgEl.className = 'message';
        }, 3000);
    }
}

// 页面特定初始化
function initPageSpecific() {
    const path = window.location.pathname;

    if (path.includes('register.html')) {
        initRegisterPage();
    } else if (path.includes('login.html')) {
        initLoginPage();
    } else if (path.includes('article.html')) {
        initArticlePage();
    } else if (path.includes('admin.html')) {
        initAdminPage();
    } else if (path.includes('.html') && !path.includes('index')) {
        initListPage();
    }
}

// 注册页面初始化
function initRegisterPage() {
    refreshCaptcha();
    
    const captchaImg = document.getElementById('captchaImage');
    if (captchaImg) {
        captchaImg.addEventListener('click', refreshCaptcha);
    }

    const form = document.getElementById('registerForm');
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(form);
            const password = formData.get('password');
            const confirmPassword = formData.get('confirmPassword');

            if (password !== confirmPassword) {
                showMessage('registerMessage', '两次输入的密码不一致', false);
                return;
            }

            try {
                const response = await fetch('/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: formData.get('username'),
                        email: formData.get('email'),
                        password: password,
                        captcha: formData.get('captcha')
                    })
                });

                const data = await response.json();
                
                if (data.success) {
                    showMessage('registerMessage', data.message, true);
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 1500);
                } else {
                    showMessage('registerMessage', data.message, false);
                    refreshCaptcha();
                }
            } catch (err) {
                showMessage('registerMessage', '注册失败，请重试', false);
            }
        });
    }
}

// 登录页面初始化
function initLoginPage() {
    refreshCaptcha();
    
    const captchaImg = document.getElementById('captchaImage');
    if (captchaImg) {
        captchaImg.addEventListener('click', refreshCaptcha);
    }

    const form = document.getElementById('loginForm');
    if (form) {
        form.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(form);

            try {
                const response = await fetch('/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: formData.get('username'),
                        password: formData.get('password'),
                        captcha: formData.get('captcha')
                    })
                });

                const data = await response.json();
                
                if (data.success) {
                    showMessage('loginMessage', data.message, true);
                    
                    // 获取重定向参数
                    const params = new URLSearchParams(window.location.search);
                    const redirect = params.get('redirect') || '/index.html';
                    
                    setTimeout(() => {
                        window.location.href = redirect;
                    }, 1000);
                } else {
                    showMessage('loginMessage', data.message, false);
                    refreshCaptcha();
                }
            } catch (err) {
                showMessage('loginMessage', '登录失败，请重试', false);
            }
        });
    }
}

// 列表页面初始化
function initListPage() {
    const path = window.location.pathname;
    const categoryMap = {
        'classics.html': 'classics',
        'ich.html': 'ich',
        'art.html': 'art',
        'music.html': 'music',
        'tech.html': 'tech'
    };
    
    const category = categoryMap[path.split('/').pop()];
    if (category) {
        loadArticles(category);
    }
}

// 加载文章列表
async function loadArticles(category) {
    try {
        const response = await fetch(`/api/articles?category=${category}`);
        const data = await response.json();
        
        if (data.success) {
            renderArticleList(data.articles);
        }
    } catch (err) {
        console.error('加载文章失败', err);
    }
}

// 渲染文章列表
function renderArticleList(articles) {
    const listContainer = document.getElementById('articleList');
    if (!listContainer) return;

    if (articles.length === 0) {
        listContainer.innerHTML = '<p style="text-align: center; color: #888;">暂无文章</p>';
        return;
    }

    listContainer.innerHTML = articles.map(article => `
        <div class="article-item" onclick="window.location.href='/article.html?id=${article.id}'">
            <h3>${article.title}</h3>
            <div class="article-meta">
                <span>📅 ${new Date(article.create_time).toLocaleDateString()}</span>
            </div>
        </div>
    `).join('');
}

// 文章详情页面初始化
async function initArticlePage() {
    const params = new URLSearchParams(window.location.search);
    const articleId = params.get('id');

    if (!articleId) {
        window.location.href = '/index.html';
        return;
    }

    await loadArticleDetail(articleId);
    await loadComments(articleId);
    
    // 绑定点赞事件
    const likeBtn = document.getElementById('likeBtn');
    if (likeBtn) {
        likeBtn.addEventListener('click', () => toggleLike(articleId));
    }

    // 绑定转发事件
    const shareBtn = document.getElementById('shareBtn');
    if (shareBtn) {
        shareBtn.addEventListener('click', () => shareArticle(articleId));
    }

    // 绑定评论提交事件
    const commentForm = document.getElementById('commentForm');
    if (commentForm) {
        commentForm.addEventListener('submit', (e) => {
            e.preventDefault();
            submitComment(articleId);
        });
    }
}

// 加载文章详情
async function loadArticleDetail(articleId) {
    try {
        const response = await fetch(`/api/articles/${articleId}`);
        const data = await response.json();
        
        if (data.success) {
            renderArticleDetail(data);
        } else {
            window.location.href = '/index.html';
        }
    } catch (err) {
        console.error('加载文章失败', err);
    }
}

// 渲染文章详情
function renderArticleDetail(data) {
    const { article, likeCount, commentCount, isLiked } = data;
    
    document.getElementById('articleTitle').textContent = article.title;
    document.getElementById('articleAuthor').textContent = article.author;
    document.getElementById('articleContent').textContent = article.content;
    document.getElementById('articleSource').textContent = '来源：' + (article.source || '本站整理');
    
    document.getElementById('likeCount').textContent = likeCount;
    document.getElementById('commentCount').textContent = commentCount;
    
    const likeBtn = document.getElementById('likeBtn');
    if (isLiked) {
        likeBtn.classList.add('liked');
    }
}

// 点赞/取消点赞
async function toggleLike(articleId) {
    if (!currentUser) {
        alert('请先登录');
        window.location.href = '/login.html?redirect=/article.html?id=' + articleId;
        return;
    }

    try {
        const response = await fetch(`/api/articles/${articleId}/like`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const data = await response.json();
        
        if (data.success) {
            const likeBtn = document.getElementById('likeBtn');
            const likeCount = document.getElementById('likeCount');
            let count = parseInt(likeCount.textContent);
            
            if (data.liked) {
                likeBtn.classList.add('liked');
                likeCount.textContent = count + 1;
            } else {
                likeBtn.classList.remove('liked');
                likeCount.textContent = count - 1;
            }
        }
    } catch (err) {
        console.error('点赞失败', err);
    }
}

// 转发文章
async function shareArticle(articleId) {
    if (!currentUser) {
        alert('请先登录');
        window.location.href = '/login.html?redirect=/article.html?id=' + articleId;
        return;
    }

    try {
        // 复制链接到剪贴板
        await navigator.clipboard.writeText(window.location.href);
        
        // 记录转发
        await fetch(`/api/articles/${articleId}/share`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        alert('链接已复制，分享给朋友吧！');
    } catch (err) {
        alert('复制失败，请手动复制链接');
    }
}

// 加载评论
async function loadComments(articleId) {
    try {
        const response = await fetch(`/api/articles/${articleId}/comments`);
        const data = await response.json();
        
        if (data.success) {
            renderComments(data.comments);
        }
    } catch (err) {
        console.error('加载评论失败', err);
    }
}

// 渲染评论
function renderComments(comments) {
    const commentList = document.getElementById('commentList');
    if (!commentList) return;

    if (comments.length === 0) {
        commentList.innerHTML = '<p style="text-align: center; color: #888;">暂无评论，快来发表第一条评论吧！</p>';
        return;
    }

    commentList.innerHTML = comments.map(comment => `
        <div class="comment-item">
            <div class="comment-header">
                <span class="comment-author">${comment.username}</span>
                <span class="comment-time">${new Date(comment.create_time).toLocaleString()}</span>
            </div>
            <p>${comment.content}</p>
        </div>
    `).join('');
}

// 提交评论
async function submitComment(articleId) {
    if (!currentUser) {
        alert('请先登录');
        window.location.href = '/login.html?redirect=/article.html?id=' + articleId;
        return;
    }

    const textarea = document.getElementById('commentContent');
    const content = textarea.value.trim();

    if (!content) {
        alert('请输入评论内容');
        return;
    }

    try {
        const response = await fetch(`/api/articles/${articleId}/comment`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content })
        });

        const data = await response.json();
        
        if (data.success) {
            textarea.value = '';
            await loadComments(articleId);
            
            // 更新评论计数
            const commentCount = document.getElementById('commentCount');
            commentCount.textContent = parseInt(commentCount.textContent) + 1;
        } else {
            alert(data.message);
        }
    } catch (err) {
        console.error('评论失败', err);
    }
}

// 后台管理页面初始化
async function initAdminPage() {
    await loadAdminStats();
}

// 加载后台统计数据
async function loadAdminStats() {
    try {
        const response = await fetch('/api/admin/stats');
        const data = await response.json();
        
        if (data.success) {
            renderAdminStats(data);
        } else {
            alert('无权限访问');
            window.location.href = '/index.html';
        }
    } catch (err) {
        console.error('加载统计数据失败', err);
    }
}

// 渲染后台统计
function renderAdminStats(data) {
    const { stats, categoryStats, topArticles } = data;

    // 更新统计数字
    document.getElementById('totalUsers').textContent = stats.totalUsers;
    document.getElementById('totalLikes').textContent = stats.totalLikes;
    document.getElementById('totalComments').textContent = stats.totalComments;
    document.getElementById('totalShares').textContent = stats.totalShares;

    // 渲染分类统计图表
    renderCategoryChart(categoryStats);
    
    // 渲染热门文章图表
    renderTopArticlesChart(topArticles);
}

// 渲染分类统计图表
function renderCategoryChart(categoryStats) {
    const categoryNames = {
        'classics': '经典典籍',
        'ich': '非遗传承',
        'art': '翰墨丹青',
        'music': '礼乐风雅',
        'tech': '传统科技'
    };

    const chartDom = document.getElementById('categoryChart');
    if (!chartDom || typeof echarts === 'undefined') return;

    const myChart = echarts.init(chartDom);
    
    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        legend: {
            data: ['点赞数', '评论数']
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: categoryStats.map(c => categoryNames[c.category] || c.category)
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                name: '点赞数',
                type: 'bar',
                data: categoryStats.map(c => c.like_count),
                itemStyle: { color: '#b22222' }
            },
            {
                name: '评论数',
                type: 'bar',
                data: categoryStats.map(c => c.comment_count),
                itemStyle: { color: '#667eea' }
            }
        ]
    };

    myChart.setOption(option);
    
    // 响应式
    window.addEventListener('resize', () => myChart.resize());
}

// 渲染热门文章图表
function renderTopArticlesChart(topArticles) {
    const chartDom = document.getElementById('topChart');
    if (!chartDom || typeof echarts === 'undefined') return;

    const myChart = echarts.init(chartDom);
    
    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        legend: {
            data: ['点赞数', '评论数']
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'value'
        },
        yAxis: {
            type: 'category',
            data: topArticles.map(a => a.title.length > 10 ? a.title.substring(0, 10) + '...' : a.title)
        },
        series: [
            {
                name: '点赞数',
                type: 'bar',
                data: topArticles.map(a => a.likes),
                itemStyle: { color: '#b22222' }
            },
            {
                name: '评论数',
                type: 'bar',
                data: topArticles.map(a => a.comments),
                itemStyle: { color: '#667eea' }
            }
        ]
    };

    myChart.setOption(option);
    
    // 响应式
    window.addEventListener('resize', () => myChart.resize());
}
