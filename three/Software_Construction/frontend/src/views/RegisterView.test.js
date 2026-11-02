import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

// 提供最小 stub：真实 router-link 需要 router 实例才能挂载
const globalStubs = {
    'router-link': { template: '<a><slot /></a>' }
}

const register = vi.fn()
const csrf = vi.fn().mockResolvedValue({})
vi.mock('../api', () => ({
    authApi: {
        register: (...a) => register(...a),
        csrf: (...a) => csrf(...a),
        me: vi.fn().mockResolvedValue(null),
        login: vi.fn(),
        logout: vi.fn()
    }
}))

const { default: RegisterView } = await import('./RegisterView.vue')

const flush = () => new Promise((r) => setTimeout(r, 0))

/** 填好三个必填项（username / password / realName 是前三个 input） */
async function fillValid(wrapper) {
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('newuser')
    await inputs[1].setValue('123456')
    await inputs[2].setValue('张三')
    return inputs
}

const submitBtn = (wrapper) => wrapper.findAll('button')[0]

describe('RegisterView（R5 High #13：Enter / 程序化触发绕过按钮 disabled 重复提交）', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        register.mockReset()
        register.mockResolvedValue({ id: 1 })
        csrf.mockClear()
    })

    it('在飞时连续两次提交只发一次请求（early-return 生效）', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await fillValid(wrapper)

        // 不靠按钮 disabled，直接连调两次 onSubmit —— 模拟 Enter 键与反应更新前的双击。
        // 只靠 :disabled 时第二次仍会进来，这就是 #13 报的问题。
        const vm = wrapper.vm
        await Promise.all([vm.onSubmit(), vm.onSubmit()])
        await flush()

        expect(register).toHaveBeenCalledTimes(1)
    })

    it('按钮在提交期间被禁用（界面层也有防护）', async () => {
        let release
        register.mockReturnValue(new Promise((r) => { release = r }))
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await fillValid(wrapper)

        submitBtn(wrapper).trigger('click')
        await flush()
        expect(submitBtn(wrapper).attributes('disabled')).toBeDefined()

        release({ id: 1 })
        await flush()
        // 成功后 done=true，按钮本身被 v-if 移除（不再是「重新可用」）
        expect(wrapper.text()).toContain('请等待管理员审核')
        expect(wrapper.findAll('button')).toHaveLength(0)
    })

    it('用户名短于 3 位时前端直接拦下，不浪费一次往返', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        const inputs = wrapper.findAll('input')
        await inputs[0].setValue('ab')
        await inputs[1].setValue('123456')
        await inputs[2].setValue('张三')
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(register).not.toHaveBeenCalled()
        expect(wrapper.text()).toContain('用户名长度需为 3–50 个字符')
    })

    it('密码短于 6 位时同样的前端拦截', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        const inputs = wrapper.findAll('input')
        await inputs[0].setValue('newuser')
        await inputs[1].setValue('123')
        await inputs[2].setValue('张三')
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(register).not.toHaveBeenCalled()
        expect(wrapper.text()).toContain('密码长度需为 6–100 个字符')
    })

    it('必填项缺失时不提交', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(register).not.toHaveBeenCalled()
        expect(wrapper.text()).toContain('均为必填项')
    })

    it('邮箱格式错误时不提交', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await fillValid(wrapper)
        const emailInput = wrapper.findAll('input').find((i) => i.attributes('type') === 'email')
        await emailInput.setValue('不是邮箱')
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(register).not.toHaveBeenCalled()
        expect(wrapper.text()).toContain('邮箱格式不正确')
    })

    it('合法数据提交一次并显示已提交状态', async () => {
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await fillValid(wrapper)
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(register).toHaveBeenCalledTimes(1)
        expect(register.mock.calls[0][0]).toMatchObject({ username: 'newuser', realName: '张三' })
        expect(wrapper.text()).toContain('请等待管理员审核')
    })

    it('提交失败时显示后端 message，且能立刻重试', async () => {
        register.mockRejectedValue(new Error('用户名「newuser」已被占用'))
        const wrapper = mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        await fillValid(wrapper)
        await submitBtn(wrapper).trigger('click')
        await flush()

        expect(wrapper.text()).toContain('用户名「newuser」已被占用')
        // 失败后 loading 必须复位，否则用户以为卡死
        expect(submitBtn(wrapper).attributes('disabled')).toBeUndefined()

        register.mockResolvedValue({ id: 1 })
        await submitBtn(wrapper).trigger('click')
        await flush()
        expect(register).toHaveBeenCalledTimes(2)
    })

    it('挂载时预取 CSRF（否则首次 POST 会被 CSRF 过滤器挡）', async () => {
        mount(RegisterView, { global: { stubs: globalStubs } })
        await flush()
        expect(csrf).toHaveBeenCalled()
    })
})
