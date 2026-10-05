import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import MainLayout from '../layouts/MainLayout.vue'

// meta.auth  = 需要 ADMIN 或 RESEARCHER 才能进入
// meta.role  = 只有该角色能进入
// meta.public= 免登录页
const routes = [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true, title: '登录' } },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { public: true, title: '注册申请' } },
    {
        path: '/',
        component: MainLayout,
        children: [
            { path: '', redirect: '/dashboard' },
            { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '综合数据看板', nav: 'dashboard' } },
            { path: 'species', name: 'species', component: () => import('../views/SpeciesListView.vue'), meta: { title: '物种信息管理', nav: 'species' } },
            { path: 'species/new', name: 'species-new', component: () => import('../views/SpeciesFormView.vue'), meta: { title: '新增物种信息', auth: true } },
            { path: 'species/:id', name: 'species-detail', component: () => import('../views/SpeciesDetailView.vue'), meta: { title: '物种详情' } },
            { path: 'species/:id/edit', name: 'species-edit', component: () => import('../views/SpeciesFormView.vue'), meta: { title: '编辑物种信息', auth: true } },
            { path: 'ecosystems', name: 'ecosystems', component: () => import('../views/EcosystemView.vue'), meta: { title: '生态系统管理', nav: 'ecosystems' } },
            { path: 'observations', name: 'observations', component: () => import('../views/ObservationListView.vue'), meta: { title: '观测记录', nav: 'observations' } },
            { path: 'observations/new', name: 'observation-new', component: () => import('../views/ObservationFormView.vue'), meta: { title: '新增观测记录', auth: true } },
            { path: 'observations/:id/edit', name: 'observation-edit', component: () => import('../views/ObservationFormView.vue'), meta: { title: '编辑观测记录', auth: true } },
            { path: 'stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { title: '统计分析', nav: 'stats' } },
            { path: 'map', name: 'map', component: () => import('../views/MapView.vue'), meta: { title: '分布地图', nav: 'map' } },
            { path: 'ai/identify', name: 'ai-identify', component: () => import('../views/IdentifyView.vue'), meta: { title: '图像智能识别', nav: 'ai-identify' } },
            { path: 'ai/chat', name: 'ai-chat', component: () => import('../views/ChatView.vue'), meta: { title: '智能问答', nav: 'ai-chat' } },
            { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人资料', nav: 'profile' } },
            { path: 'admin/users', name: 'admin-users', component: () => import('../views/UserManageView.vue'), meta: { title: '用户与权限管理', role: 'ADMIN' } },
            { path: 'admin/logs', name: 'admin-logs', component: () => import('../views/LogView.vue'), meta: { title: '操作日志', role: 'ADMIN' } }
        ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to) => {
    const store = useUserStore()
    await store.load()

    if (to.meta.public) {
        // 已登录的人不必再看登录页
        return to.name === 'login' && store.isLogin ? { path: '/dashboard' } : true
    }

    if (!store.isLogin) {
        return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (to.meta.role && store.role !== to.meta.role) {
        return { path: '/dashboard' }
    }

    if (to.meta.auth && !store.canEdit) {
        return { path: '/dashboard' }
    }

    return true
})

router.afterEach((to) => {
    document.title = to.meta.title
        ? `${to.meta.title} - 海洋生物多样性信息管理系统`
        : '海洋生物多样性信息管理系统'
})

export default router
