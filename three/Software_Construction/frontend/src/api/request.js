import axios from 'axios'
import { useUserStore } from '../store/user'

// 所有请求走同源 /api：开发时由 Vite 代理到 8088，打包后由后端自己托管静态页面。
// withCredentials 保证 JSESSIONID 随请求带上；CSRF 令牌由 axios 自动
// 从 XSRF-TOKEN Cookie 读取并放进 X-XSRF-TOKEN 请求头，无需手写。
const request = axios.create({
    baseURL: '/api',
    withCredentials: true,
    // 大模型识别与问答可能耗时较久，超时放宽到 60 秒
    timeout: 60000
})

// 多个请求同时 401 时，每个都会走到这里：clear() 和 location.href 会被反复执行，
// 后面的 401 还会在页面已经跳转时再触发一次导航。用一个标志位只跳一次。
let redirectingToLogin = false

request.interceptors.response.use(
    response => {
        const body = response.data
        // 后端统一响应体 { success, message, data }，这里直接把 data 拆出来
        if (body && typeof body === 'object' && 'success' in body) {
            if (!body.success) {
                return Promise.reject(withStatus(new Error(body.message || '操作失败'), response.status))
            }
            return body.data
        }
        return body
    },
    error => {
        const status = error.response && error.response.status
        // 未登录（或会话过期）时后端返回 401，清空本地状态并跳回登录页。
        // 必须带上 redirect：否则用户在某个页面被踢下线后，登录成功只会回到看板，
        // 得自己重新找回去。LoginView 已经会读这个参数。
        if (status === 401) {
            useUserStore().clear()
            if (!redirectingToLogin && window.location.pathname !== '/login') {
                redirectingToLogin = true
                const back = encodeURIComponent(window.location.pathname + window.location.search)
                window.location.href = `/login?redirect=${back}`
            }
        }
        // data 是纯文本时 data.message 为 undefined，不能直接取；
        // 而 axios 的「Request failed with status code 500」对用户毫无意义，
        // 所以按状态码给一句人话。取消请求不弹错：那是调用方主动放弃的。
        if (error.code === 'ERR_CANCELED') {
            return Promise.reject(withStatus(error, status))
        }
        const data = error.response && error.response.data
        // 没有任何响应（断网、后端没起）时 error.message 是 axios 的英文原文，
        // 对用户无意义；这时直接给中文兜底。有响应时后端 message 优先。
        const message = !error.response
            ? '网络异常，请稍后重试'
            : (data && typeof data === 'object' && data.message)
            || FRIENDLY[status]
            || '网络异常，请稍后重试'
        return Promise.reject(withStatus(new Error(message), status))
    }
)

// 调用方（如 store.load）需要能区分「未登录」与「网络故障/后端 5xx」
function withStatus(err, status) {
    err.status = status
    return err
}

const FRIENDLY = {
    400: '请求参数有误，请检查后重试',
    403: '当前角色没有执行该操作的权限',
    404: '请求的资源不存在',
    500: '服务器处理失败，请稍后重试'
}

export default request
