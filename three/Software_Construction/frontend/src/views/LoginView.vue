<template>
    <div class="auth">
        <div class="auth__card">
            <h1 class="auth__title">海洋生物多样性信息管理系统</h1>
            <p class="auth__sub">广东海洋大学 · 软件构造与体系结构课程设计</p>

            <div v-if="error" class="alert alert--error">{{ error }}</div>

            <div class="form">
                <div class="form-item form-item--full">
                    <label class="form-item__label"><span class="required">*</span>用户名</label>
                    <input v-model.trim="form.username" class="input" autocomplete="username"
                           placeholder="请输入用户名" @keyup.enter="onSubmit"/>
                </div>
                <div class="form-item form-item--full">
                    <label class="form-item__label"><span class="required">*</span>密码</label>
                    <input v-model="form.password" type="password" class="input" autocomplete="current-password"
                           placeholder="请输入密码" @keyup.enter="onSubmit"/>
                </div>
            </div>

            <div class="btn-group" style="margin-top: 18px; justify-content: center;">
                <button class="btn" style="min-width: 150px;" :disabled="loading" @click="onSubmit">
                    {{ loading ? '登录中…' : '登 录' }}
                </button>
            </div>

            <p class="auth__foot">还没有账号？<router-link to="/register">提交注册申请</router-link></p>

            <div class="auth__demo">
                <strong>演示账号</strong>（种子数据，密码见 database.sql 注释）<br/>
                管理员 admin / admin123　　科研人员 researcher / research123<br/>
                学生 student / student123　　公众 visitor / public123
            </div>
        </div>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

onMounted(async () => {
    // 先取一次 CSRF 令牌，让后端把 XSRF-TOKEN 写进 Cookie，
    // 之后 axios 会自动带上 X-XSRF-TOKEN 头
    try {
        await authApi.csrf()
    } catch (e) {
        // 令牌获取失败不阻塞登录页显示
    }
})

async function onSubmit() {
    if (!form.username || !form.password) {
        error.value = '请输入用户名和密码'
        return
    }
    error.value = ''
    loading.value = true
    try {
        await store.login({ ...form })
        router.push(route.query.redirect || '/dashboard')
    } catch (e) {
        error.value = e.message
    } finally {
        loading.value = false
    }
}
</script>
