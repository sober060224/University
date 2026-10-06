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

request.interceptors.response.use(
    response => {
        const body = response.data
        // 后端统一响应体 { success, message, data }，这里直接把 data 拆出来
        if (body && typeof body === 'object' && 'success' in body) {
            if (!body.success) {
                return Promise.reject(new Error(body.message || '操作失败'))
            }
            return body.data
        }
        return body
    },
    error => {
        // 未登录（或会话过期）时后端返回 401，清空本地状态并跳回登录页。
        // 必须带上 redirect：否则用户在某个页面被踢下线后，登录成功只会回到看板，
        // 得自己重新找回去。LoginView 已经会读这个参数。
        if (error.response && error.response.status === 401) {
            useUserStore().clear()
            if (window.location.pathname !== '/login') {
                const back = encodeURIComponent(window.location.pathname + window.location.search)
                window.location.href = `/login?redirect=${back}`
            }
        }
        const message = (error.response && error.response.data && error.response.data.message)
            || error.message
            || '网络异常，请稍后重试'
        return Promise.reject(new Error(message))
    }
)

export default request
