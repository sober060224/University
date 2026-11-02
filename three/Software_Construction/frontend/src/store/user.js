import { defineStore } from 'pinia'
import { authApi } from '../api'

const ROLE_NAMES = {
    ADMIN: '系统管理员',
    RESEARCHER: '科研人员',
    STUDENT: '学生',
    PUBLIC: '公众'
}

export const useUserStore = defineStore('user', {
    state: () => ({
        profile: null,
        loaded: false,
        // 身份已确认过（含「确认是未登录」）。
        // 只用 loaded 记录「拿到过 profile」时，匿名会话永远 loaded=false，
        // 每次路由守卫与 landingPath 都会再发一次 /auth/me。
        checked: false
    }),

    getters: {
        isLogin: (state) => !!state.profile,
        role: (state) => (state.profile ? state.profile.role : null),
        roleName: (state) => (state.profile ? (ROLE_NAMES[state.profile.role] || state.profile.role) : '访客'),
        // 科研人员与管理员可以录入、修改数据
        canEdit: (state) => !!state.profile && ['ADMIN', 'RESEARCHER'].includes(state.profile.role),
        isAdmin: (state) => !!state.profile && state.profile.role === 'ADMIN',
        // 报告 表 3-2 把「查看综合数据看板」「查看统计报表与分布地图」「智能问答」
        // 对公众标为 ×，后端 SecurityConfig 也照此拦了下来。前端必须跟着藏掉入口，
        // 否则公众点进这些页面只会拿到一句 403 提示，看起来像系统的 bug。
        canViewStats: (state) => !!state.profile && ['ADMIN', 'RESEARCHER', 'STUDENT'].includes(state.profile.role)
    },

    actions: {
        // 刷新页面后先用会话向服务端确认身份，失败说明未登录
        async load() {
            // checked：身份已确认过（含「确认是未登录」）。只用 loaded 时匿名访客
            // 每导航一次、每次 landingPath 都会再发一次 /auth/me。
            if (this.loaded || this.checked) {
                return this.profile
            }
            try {
                this.profile = await authApi.me()
                this.loaded = true
            } catch (e) {
                // 只有 401 是「确定未登录」，这个否定结果可以缓存（checked）；
                // 网络抖动/后端 5xx 不能当成未登录，否则一次瞬断就把用户踢到登录页。
                this.profile = null
                this.loaded = false
                this.checked = !!e && e.status === 401
            }
            return this.profile
        },

        async login(form) {
            try {
                this.profile = await authApi.login(form)
                this.loaded = true
                return this.profile
            } catch (e) {
                // 登录失败必须清掉旧快照：上一个账号的 profile 留在 state 里时，
                // isLogin 仍为 true，接口却已是未登录，store 与真实会话不一致。
                this.clear()
                throw e
            }
        },

        // 资料类接口直接回最新用户信息，用它同步 store。
        // 不能靠 load()：load() 只在首次确认身份时请求，loaded 为 true 时是空转，
        // 会把旧 profile 又铺回界面。
        setProfile(profile) {
            this.profile = profile
            this.loaded = true
        },

        async logout() {
            try {
                await authApi.logout()
            } finally {
                this.clear()
            }
        },

        // loaded 必须置回 false：本文件 load() 上方的注释说明了原因——一旦置 true，
        // load() 就会一直空转，之后所有路由守卫都拿不到 profile，只能整页刷新才能恢复。
        clear() {
            this.profile = null
            this.loaded = false
            // 到 clear 就意味着「确定未登录」（退出/401/登录失败），不必再发 /auth/me
            this.checked = true
        }
    }
})

export { ROLE_NAMES }
