import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// 拦截器是所有请求的唯一出入口：401 处理错一次，用户就会被无限踢或永远不踢。
// 这里不引入 axios-mock-adapter，直接取出注册好的拦截器回调来调 —— 少一个依赖，
// 而且测的正是生产代码里那两个函数。

const clear = vi.fn()
vi.mock('../store/user', () => ({
    useUserStore: () => ({ clear })
}))

const { default: request } = await import('./request')

/** 取出 interceptor 已注册的成功/失败回调 */
function handlers() {
    const h = request.interceptors.response.handlers[0]
    return { ok: h.fulfilled, fail: h.rejected }
}

/** 造一个 axios 风格的错误 */
function axiosError(status, data, code) {
    return { response: status ? { status, data } : undefined, code, message: 'Request failed' }
}

describe('request 响应拦截器', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        clear.mockReset()
    })

    it('success:true 时把 data 拆出来（调用方直接拿到业务数据）', async () => {
        const { ok } = handlers()
        const out = await ok({ data: { success: true, message: 'ok', data: { id: 7 } }, status: 200 })
        expect(out).toEqual({ id: 7 })
    })

    it('业务失败（HTTP 200 + success:false）也 reject，并带上 status', async () => {
        const { ok } = handlers()
        await expect(ok({ data: { success: false, message: '物种不存在' }, status: 200 }))
            .rejects.toMatchObject({ message: '物种不存在', status: 200 })
    })

    it('success:false 且无 message 时给默认文案（不能显示 undefined）', async () => {
        const { ok } = handlers()
        await expect(ok({ data: { success: false }, status: 200 }))
            .rejects.toMatchObject({ message: '操作失败' })
    })

    it('非统一响应体（例如纯文本/文件流）原样透传', async () => {
        const { ok } = handlers()
        expect(await ok({ data: 'plain', status: 200 })).toBe('plain')
        expect(await ok({ data: null, status: 200 })).toBeNull()
    })

    it('401 会清空本地状态（避免过期会话还在界面上显示已登录）', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(401, { message: '登录状态已失效' }))).rejects.toThrow()
        expect(clear).toHaveBeenCalledTimes(1)
    })

    it('非 401 不动本地状态（网络抖动不能把用户踢下线）', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(500, null))).rejects.toThrow()
        await expect(fail(axiosError(403, null))).rejects.toThrow()
        expect(clear).not.toHaveBeenCalled()
    })

    it('后端给了 message 时优先用后端那句', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(403, { message: '当前角色没有执行该操作的权限' })))
            .rejects.toMatchObject({ message: '当前角色没有执行该操作的权限', status: 403 })
    })

    it('后端没给 message 时按状态码给人话，而不是 axios 原文', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(400, null)))
            .rejects.toMatchObject({ message: '请求参数有误，请检查后重试' })
        await expect(fail(axiosError(404, null)))
            .rejects.toMatchObject({ message: '请求的资源不存在' })
        await expect(fail(axiosError(500, null)))
            .rejects.toMatchObject({ message: '服务器处理失败，请稍后重试' })
    })

    it('data 是纯文本时不取 message（typeof 判断，不能直接读属性）', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(500, 'Internal Server Error')))
            .rejects.toMatchObject({ message: '服务器处理失败，请稍后重试' })
    })

    it('无响应的网络错误落到「网络异常」文案，不出现 axios 原文', async () => {
        const { fail } = handlers()
        await expect(fail(axiosError(0, null)))
            .rejects.toMatchObject({ message: '网络异常，请稍后重试' })
    })

    it('主动取消的请求不弹错，但仍然是 reject（调用方靠 status 区分）', async () => {
        const { fail } = handlers()
        const cancelled = axiosError(0, null, 'ERR_CANCELED')
        await expect(fail(cancelled)).rejects.toBeTruthy()
        // 取消不该触发登出
        expect(clear).not.toHaveBeenCalled()
    })
})
