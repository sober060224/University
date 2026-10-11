<template>
    <div class="layout">
        <nav class="navbar">
            <router-link to="/dashboard" class="navbar__brand">🌊 海洋生物多样性信息管理系统</router-link>

            <div class="navbar__menu">
                <router-link v-for="item in menu" :key="item.to" :to="item.to"
                             class="navbar__item" :class="{ active: active === item.nav }">
                    {{ item.label }}
                </router-link>
            </div>

            <div class="navbar__user">
                <span class="tag tag--muted">{{ store.roleName }}</span>
                <span class="navbar__name">{{ store.profile?.realName }}</span>
                <button class="btn btn--sm btn--ghost" @click="onLogout">退出</button>
            </div>
        </nav>

        <main class="page">
            <!-- 必须带 key：SpeciesFormView / ObservationFormView 各同时挂在
                 /xxx/new 与 /xxx/:id/edit 两条路由上，Vue 默认会在两者之间复用同一个
                 组件实例，onMounted 不再触发，于是打开编辑链接后表单还是上一个页面的
                 内容、isEdit 也停留在旧值，点保存会写成一条新记录。 -->
            <router-view :key="route.fullPath"/>
        </main>

        <footer class="footer">
            海洋生物多样性信息管理系统 · 广东海洋大学 软件构造与体系结构课程设计
        </footer>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const active = computed(() => route.meta.nav)

const menu = computed(() => {
    const items = [
        { nav: 'dashboard', to: '/dashboard', label: '综合看板', stats: true },
        { nav: 'species', to: '/species', label: '物种信息' },
        { nav: 'ecosystems', to: '/ecosystems', label: '生态系统' },
        { nav: 'observations', to: '/observations', label: '观测记录' },
        { nav: 'stats', to: '/stats', label: '统计分析', stats: true },
        { nav: 'map', to: '/map', label: '分布地图' },
        { nav: 'ai-identify', to: '/ai/identify', label: '图像识别' },
        { nav: 'ai-chat', to: '/ai/chat', label: '智能问答', stats: true },
        { nav: 'profile', to: '/profile', label: '个人资料' }
    ]
    if (store.isAdmin) {
        items.push({ nav: 'users', to: '/admin/users', label: '用户管理' })
        items.push({ nav: 'logs', to: '/admin/logs', label: '操作日志' })
    }
    // 公众看不到看板/统计/问答（后端同样会返回 403），菜单里直接藏掉，
    // 否则点进去只是一句「当前角色没有执行该操作的权限」，像是坏了。
    return items.filter((item) => !item.stats || store.canViewStats)
})

async function onLogout() {
    // 退出接口失败也要跳回登录页：store.logout() 已在 finally 里清空本地状态，
    // 若此时仍停在布局里，profile 为 null 会让整棵布局渲染失败（白屏且无法自救）。
    try {
        await store.logout()
    } finally {
        router.push('/login')
    }
}
</script>

<style scoped>
.layout {
    display: flex;
    flex-direction: column;
    min-height: 100%;
}

.page {
    flex: 1;
    width: 100%;
}
</style>
