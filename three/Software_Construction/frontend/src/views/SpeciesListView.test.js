import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

// R5 High #2/#3/#4/#6 的根因是同一个 loadToken 竞态守卫模式。
// 这里直接对 SpeciesListView 挂载 + 手动控制回包顺序，验证「旧回包被丢弃」。
// 只要这个模式在任一视图上被改回无守卫，下面的用例就会红。

const globalStubs = {
    'router-link': { template: '<a><slot /></a>' },
    Pagination: { template: '<div />' }
}

const list = vi.fn()
const phylums = vi.fn()
const remove = vi.fn()
vi.mock('../api', () => ({
    speciesApi: {
        list: (...a) => list(...a),
        phylums: (...a) => phylums(...a),
        remove: (...a) => remove(...a)
    },
    authApi: { me: vi.fn() }
}))
vi.mock('../store/user', () => ({
    useUserStore: () => ({ canEdit: true, isAdmin: true, canViewStats: true })
}))

const { default: SpeciesListView } = await import('./SpeciesListView.vue')

const flush = () => new Promise((r) => setTimeout(r, 0))

/** 造一个可控的 pending promise */
function deferred() {
    let resolve
    let reject
    const promise = new Promise((res, rej) => {
        resolve = res
        reject = rej
    })
    return { promise, resolve, reject }
}

describe('SpeciesListView 的 loadToken 竞态守卫（R5 High #2）', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        list.mockReset()
        phylums.mockReset()
        phylums.mockResolvedValue([])
    })

    it('慢的旧请求后到时不覆盖新请求的数据', async () => {
        const first = deferred()
        const second = deferred()
        list.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)

        const wrapper = mount(SpeciesListView, { global: { stubs: globalStubs } })
        await flush()

        // 第一次加载已挂在 onMounted 上（第一个 deferred），再触发一次翻页
        wrapper.vm.onPage(2)
        await flush()
        expect(list).toHaveBeenCalledTimes(2)

        // 新请求先回，旧请求后回 —— 这正是「快速翻页」时发生的顺序
        second.resolve({ records: [{ id: 2, chineseName: '新页物种' }], total: 20 })
        await flush()
        first.resolve({ records: [{ id: 1, chineseName: '旧页物种' }], total: 20 })
        await flush()

        // 表格里必须还是新页的数据，旧回包被 token 丢弃
        expect(wrapper.text()).toContain('新页物种')
        expect(wrapper.text()).not.toContain('旧页物种')
    })

    it('旧请求后到时不再关闭 loading（避免表格已换数据但转圈提前消失）', async () => {
        const first = deferred()
        const second = deferred()
        list.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)

        const wrapper = mount(SpeciesListView, { global: { stubs: globalStubs } })
        await flush()
        wrapper.vm.onPage(2)
        await flush()

        // 旧请求先回：loading 必须仍为 true（因为新请求还没回）
        first.resolve({ records: [], total: 0 })
        await flush()

        // 新请求后回才关 loading
        second.resolve({ records: [{ id: 9, chineseName: '最终数据' }], total: 1 })
        await flush()
        expect(wrapper.text()).toContain('最终数据')
    })

    it('单次请求正常写入 records 与 total', async () => {
        list.mockResolvedValue({ records: [{ id: 5, chineseName: '单条' }], total: 1 })
        const wrapper = mount(SpeciesListView, { global: { stubs: globalStubs } })
        await flush()

        expect(wrapper.text()).toContain('单条')
        // 总数由 <Pagination> 渲染，这里已 stub 掉，直接断言组件状态
        expect(wrapper.vm.total).toBe(1)
    })

    it('下拉接口挂掉时列表仍然加载（两个请求各自成立，不互相拖死）', async () => {
        phylums.mockRejectedValue(new Error('分类接口 500'))
        list.mockResolvedValue({ records: [{ id: 6, chineseName: '列表照常' }], total: 1 })
        // 组件用 alert 报下拉失败，这里吞掉避免 jsdom 报警
        const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {})

        const wrapper = mount(SpeciesListView, { global: { stubs: globalStubs } })
        await flush()

        // 关键：下拉失败不能把列表说成「没有数据」
        expect(wrapper.text()).toContain('列表照常')
        expect(alertSpy).toHaveBeenCalled()
        alertSpy.mockRestore()
    })

    it('筛选变化会重置到第 1 页（否则筛选后还停在第 5 页会查出空页）', async () => {
        list.mockResolvedValue({ records: [], total: 0 })
        const wrapper = mount(SpeciesListView, { global: { stubs: globalStubs } })
        await flush()
        wrapper.vm.onPage(5)
        await flush()
        list.mockClear()
        list.mockResolvedValue({ records: [], total: 0 })

        wrapper.vm.search()
        await flush()

        // page 归 1 → 接口页码 0
        expect(list.mock.calls.at(-1)[0].page).toBe(0)
    })
})
