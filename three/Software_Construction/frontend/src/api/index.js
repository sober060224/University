import request from './request'

// 与后端 controller 一一对应的接口清单，报告第 8 章的 API 文档即按此表编写

/** 模块一：用户与权限 */
export const authApi = {
    csrf: () => request.get('/auth/csrf'),
    login: (data) => request.post('/auth/login', data),
    register: (data) => request.post('/auth/register', data),
    logout: () => request.post('/auth/logout'),
    me: () => request.get('/auth/me')
}

export const userApi = {
    list: (params) => request.get('/users', { params }),
    overview: () => request.get('/users/overview'),
    get: (id) => request.get(`/users/${id}`),
    approve: (id, data) => request.post(`/users/${id}/approve`, data),
    assignRole: (id, data) => request.put(`/users/${id}/role`, data),
    resetPassword: (id, data) => request.put(`/users/${id}/password`, data),
    updateProfile: (data) => request.put('/users/profile', data),
    changePassword: (data) => request.post('/users/password', data)
}

/** 模块二：物种信息 */
export const speciesApi = {
    list: (params) => request.get('/species', { params }),
    phylums: () => request.get('/species/phylums'),
    map: () => request.get('/species/map'),
    detail: (id) => request.get(`/species/${id}`),
    create: (data) => request.post('/species', data),
    update: (id, data) => request.put(`/species/${id}`, data),
    remove: (id) => request.delete(`/species/${id}`)
}

/** 模块三：生态系统与观测记录 */
export const ecosystemApi = {
    list: () => request.get('/ecosystems'),
    stats: () => request.get('/ecosystems/stats'),
    get: (id) => request.get(`/ecosystems/${id}`),
    create: (data) => request.post('/ecosystems', data),
    update: (id, data) => request.put(`/ecosystems/${id}`, data),
    remove: (id) => request.delete(`/ecosystems/${id}`)
}

export const observationApi = {
    list: (params) => request.get('/observations', { params }),
    map: () => request.get('/observations/map'),
    detail: (id) => request.get(`/observations/${id}`),
    create: (data) => request.post('/observations', data),
    update: (id, data) => request.put(`/observations/${id}`, data),
    remove: (id) => request.delete(`/observations/${id}`)
}

/** 模块四：数据可视化与报表 */
export const statsApi = {
    dashboard: () => request.get('/stats/dashboard')
}

export const logApi = {
    list: (params) => request.get('/logs', { params })
}

/** 模块五：智能服务 */
export const aiApi = {
    identify: (data) => request.post('/ai/identify', data),
    complete: (data) => request.post('/ai/complete', data),
    translate: (data) => request.post('/ai/translate', data),
    analyze: (data) => request.post('/ai/analyze', data),
    ask: (data) => request.post('/ai/ask', data),
    history: () => request.get('/ai/history'),
    clearHistory: () => request.delete('/ai/history'),
    records: (params) => request.get('/ai/records', { params })
}

/** 公共：图片上传 */
export const uploadApi = {
    image: (file) => {
        const form = new FormData()
        form.append('file', file)
        return request.post('/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    }
}
