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
        loaded: false
    }),

    getters: {
        isLogin: (state) => !!state.profile,
        role: (state) => (state.profile ? state.profile.role : null),
        roleName: (state) => (state.profile ? (ROLE_NAMES[state.profile.role] || state.profile.role) : '访客'),
        // 科研人员与管理员可以录入、修改数据
        canEdit: (state) => !!state.profile && ['ADMIN', 'RESEARCHER'].includes(state.profile.role),
        isAdmin: (state) => !!state.profile && state.profile.role === 'ADMIN'
    },

    actions: {
        // 刷新页面后先用会话向服务端确认身份，失败说明未登录
        async load() {
            if (this.loaded) {
                return this.profile
            }
            try {
                this.profile = await authApi.me()
            } catch (e) {
                this.profile = null
            }
            // 只有确认过身份才置位：否则一次网络抖动就会把 loaded 永久置为 true，
            // 之后所有路由守卫都拿不到 profile，用户被卡在登录页只能刷新
            this.loaded = this.profile !== null
            return this.profile
        },

        async login(form) {
            this.profile = await authApi.login(form)
            this.loaded = true
            return this.profile
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
        }
    }
})

export { ROLE_NAMES }
