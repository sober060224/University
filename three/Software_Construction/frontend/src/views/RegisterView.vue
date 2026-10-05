<template>
    <div class="auth">
        <div class="auth__card" style="max-width: 640px;">
            <h1 class="auth__title">注册申请</h1>
            <p class="auth__sub">学生与公众可自助申请，科研人员与管理员账号由管理员在后台分配</p>

            <div v-if="error" class="alert alert--error">{{ error }}</div>
            <div v-if="done" class="alert alert--success">
                注册申请已提交，请等待管理员审核通过后再登录。
            </div>

            <div v-if="!done" class="form">
                <div class="form-item">
                    <label class="form-item__label"><span class="required">*</span>用户名</label>
                    <input v-model.trim="form.username" class="input" placeholder="3-50 位，注册后不可修改"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label"><span class="required">*</span>密码</label>
                    <input v-model="form.password" type="password" class="input" placeholder="至少 6 位"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label"><span class="required">*</span>真实姓名</label>
                    <input v-model.trim="form.realName" class="input" placeholder="用于观测记录与日志署名"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label"><span class="required">*</span>申请身份</label>
                    <select v-model="form.applyRole" class="select">
                        <option value="STUDENT">学生</option>
                        <option value="PUBLIC">公众</option>
                    </select>
                </div>
                <div class="form-item">
                    <label class="form-item__label">学号</label>
                    <input v-model.trim="form.studentNo" class="input" placeholder="选填"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label">手机号</label>
                    <input v-model.trim="form.phone" class="input" placeholder="选填"/>
                </div>
                <div class="form-item form-item--full">
                    <label class="form-item__label">邮箱</label>
                    <input v-model.trim="form.email" type="email" class="input" placeholder="选填"/>
                </div>
            </div>

            <div class="btn-group" style="margin-top: 18px; justify-content: center;">
                <button v-if="!done" class="btn" :disabled="loading" @click="onSubmit">
                    {{ loading ? '提交中…' : '提交申请' }}
                </button>
                <router-link class="btn btn--ghost" to="/login">返回登录</router-link>
            </div>
        </div>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { authApi } from '../api'

const form = reactive({
    username: '', password: '', realName: '', applyRole: 'STUDENT',
    phone: '', email: '', studentNo: ''
})
const error = ref('')
const done = ref(false)
const loading = ref(false)

onMounted(async () => {
    try {
        await authApi.csrf()
    } catch (e) {
        // 忽略
    }
})

async function onSubmit() {
    if (!form.username || !form.password || !form.realName) {
        error.value = '用户名、密码、真实姓名均为必填项'
        return
    }
    error.value = ''
    loading.value = true
    try {
        await authApi.register({ ...form })
        done.value = true
    } catch (e) {
        error.value = e.message
    } finally {
        loading.value = false
    }
}
</script>
