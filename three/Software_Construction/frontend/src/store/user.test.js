import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// store/user.js 依赖 ../api 的 authApi，把它整个换成可控 mock，不碰网络
const me = vi.fn()
const login = vi.fn()
const logout = vi.fn()
vi.mock('../api', () => ({
    authApi: {
        me: (...a) => me(...a),
        login: (...a) => login(...a),
        logout: (...a) => logout(...a)
    }
}))

const { useUserStore, ROLE_NAMES } = await import('./user')

/** 造一个带 status 的失败，模拟 request.js 拦截器给出的错误 */
function failWith(status) {
    const err = new Error(status === 401 ? '未登录' : '服务器处理失败')
    err.status = status
    return err
}

const ADMIN = { id: 1, username: 'admin', role: 'ADMIN' }

describe('store/user 的 checked 语义（R5 #15 修过的地方）', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        me.mockReset()
        login.mockReset()
        logout.mockReset()
    })

    it('拿到 profile 后 loaded=true，后续调用不再请求 /auth/me', async () => {
        me.mockResolvedValue(ADMIN)
        const store = useUserStore()
        expect(await store.load()).toEqual(ADMIN)
        expect(store.loaded).toBe(true)
        await store.load()
        expect(me).toHaveBeenCalledTimes(1)
    })

    it('401 = 确定未登录，这个否定结果要缓存（checked=true），不再反复请求', async () => {
        me.mockRejectedValue(failWith(401))
        const store = useUserStore()
        expect(await store.load()).toBeNull()
        expect(store.checked).toBe(true)
        expect(store.loaded).toBe(false)
        await store.load()
        expect(me).toHaveBeenCalledTimes(1)
    })

    it('网络故障/500 不当成未登录：checked 保持 false，下次导航重试', async () => {
        me.mockRejectedValueOnce(failWith(500))
        const store = useUserStore()
        expect(await store.load()).toBeNull()
        expect(store.checked).toBe(false)
        expect(store.loaded).toBe(false)
        // 第二次 load 必须真的再请求 —— 这正是「一次瞬断不把用户踢到登录页」的关键
        me.mockResolvedValueOnce(ADMIN)
        expect(await store.load()).toEqual(ADMIN)
        expect(me).toHaveBeenCalledTimes(2)
    })

    it('clear() 之后 checked=true，退出登录不必再探一次 /auth/me', async () => {
        const store = useUserStore()
        store.profile = ADMIN
        store.loaded = true
        store.clear()
        expect(store.profile).toBeNull()
        expect(store.loaded).toBe(false)
        expect(store.checked).toBe(true)
        await store.load()
        expect(me).not.toHaveBeenCalled()
    })

    it('登录失败要清掉上一个账号的快照，避免 isLogin 与真实会话不一致', async () => {
        login.mockRejectedValue(new Error('用户名或密码错误'))
        const store = useUserStore()
        store.profile = ADMIN
        store.loaded = true
        await expect(store.login({ username: 'x', password: 'y' })).rejects.toThrow('用户名或密码错误')
        expect(store.profile).toBeNull()
        expect(store.loaded).toBe(false)
        expect(store.isLogin).toBe(false)
    })

    it('登录成功写入 profile 并置 loaded', async () => {
        login.mockResolvedValue(ADMIN)
        const store = useUserStore()
        expect(await store.login({ username: 'admin', password: '123456' })).toEqual(ADMIN)
        expect(store.loaded).toBe(true)
        expect(store.isLogin).toBe(true)
    })

    it('logout 即使接口失败也要清本地状态', async () => {
        logout.mockRejectedValue(new Error('网络异常'))
        const store = useUserStore()
        store.profile = ADMIN
        await expect(store.logout()).rejects.toThrow()
        expect(store.profile).toBeNull()
        expect(store.checked).toBe(true)
    })

    it('setProfile 同步最新资料，不靠 load() 空转', async () => {
        const store = useUserStore()
        store.loaded = true
        store.setProfile({ ...ADMIN, realName: '管理员' })
        expect(store.profile.realName).toBe('管理员')
        expect(store.loaded).toBe(true)
        expect(me).not.toHaveBeenCalled()
    })
})

describe('store/user 的权限派生（必须与后端 STATS_ROLES 一致）', () => {
    beforeEach(() => setActivePinia(createPinia()))

    it('能改数据的只有 ADMIN 与 RESEARCHER', () => {
        const store = useUserStore()
        for (const [role, expected] of [
            ['ADMIN', true],
            ['RESEARCHER', true],
            ['STUDENT', false],
            ['PUBLIC', false]
        ]) {
            store.profile = { id: 1, role }
            expect(store.canEdit).toBe(expected)
        }
    })

    it('canViewStats 与后端 STATS_ROLES（ADMIN/RESEARCHER/STUDENT）逐项对齐，PUBLIC 为 false', () => {
        const store = useUserStore()
        for (const [role, expected] of [
            ['ADMIN', true],
            ['RESEARCHER', true],
            ['STUDENT', true],
            ['PUBLIC', false]
        ]) {
            store.profile = { id: 1, role }
            expect(store.canViewStats).toBe(expected)
        }
    })

    it('匿名（profile=null）一律无权限，不是靠 role 为空字符串兜的', () => {
        const store = useUserStore()
        expect(store.canEdit).toBe(false)
        expect(store.canViewStats).toBe(false)
        expect(store.isAdmin).toBe(false)
        expect(store.isLogin).toBe(false)
        expect(store.role).toBeNull()
        expect(store.roleName).toBe('访客')
    })

    it('未知角色名回落到原始 role，不显示 undefined', () => {
        const store = useUserStore()
        store.profile = { id: 1, role: 'AUDITOR' }
        expect(store.roleName).toBe('AUDITOR')
        expect(ROLE_NAMES.PUBLIC).toBe('公众')
    })
})
