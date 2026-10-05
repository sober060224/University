<script setup>
// 模块一：个人资料
// 所有已登录用户均可访问：左侧维护资料，右侧修改密码。
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api/index'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()
const notice = ref('')
const error = ref('')

const profile = reactive({ realName: '', gender: '', phone: '', email: '', avatar: '', studentNo: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function fillProfile() {
  if (!store.profile) return
  Object.assign(profile, {
    realName: store.profile.realName || '',
    gender: store.profile.gender || '',
    phone: store.profile.phone || '',
    email: store.profile.email || '',
    avatar: store.profile.avatar || '',
    studentNo: store.profile.studentNo || ''
  })
}

async function saveProfile() {
  error.value = ''
  try {
    // updateProfile 直接回最新用户信息，用它同步 store，避免界面回显旧值
    store.setProfile(await userApi.updateProfile(profile))
    fillProfile()
    notice.value = '个人资料已保存'
  } catch (e) {
    error.value = e.message
  }
}

async function changePassword() {
  error.value = ''
  if (pwd.newPassword !== pwd.confirm) {
    error.value = '两次输入的新密码不一致'
    return
  }
  try {
    await userApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    // 后端改密后会话已作废，这里同步清掉本地状态并回登录页
    notice.value = '密码修改成功，请重新登录'
    store.clear()
    setTimeout(() => router.push('/login'), 800)
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  await store.load()
  fillProfile()
})
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">个人资料</h2>
        <p class="page__desc">模块一：维护本人资料，并修改登录密码</p>
      </div>
    </div>

    <div v-if="notice" class="alert alert--success">{{ notice }}</div>
    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <div class="profile-grid">
      <div class="card">
        <div class="card__title">我的资料</div>
        <div class="form">
          <div class="form-item">
            <label class="form-item__label">姓名</label>
            <input v-model="profile.realName" class="input">
          </div>
          <div class="form-item">
            <label class="form-item__label">性别</label>
            <select v-model="profile.gender" class="select">
              <option value="">未填写</option>
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="form-item">
            <label class="form-item__label">学号</label>
            <input v-model="profile.studentNo" class="input">
          </div>
          <div class="form-item">
            <label class="form-item__label">手机号</label>
            <input v-model="profile.phone" class="input">
          </div>
          <div class="form-item">
            <label class="form-item__label">邮箱</label>
            <input v-model="profile.email" class="input" type="email">
          </div>
          <div class="form-item form-item--full">
            <label class="form-item__label">头像地址</label>
            <input v-model="profile.avatar" class="input" placeholder="填写图片 URL">
          </div>
        </div>
        <div class="btn-group" style="margin-top: 12px">
          <button class="btn" @click="saveProfile">保存资料</button>
        </div>
      </div>

      <div class="card">
        <div class="card__title">修改密码</div>
        <div class="form">
          <div class="form-item form-item--full">
            <label class="form-item__label">原密码</label>
            <input v-model="pwd.oldPassword" class="input" type="password">
          </div>
          <div class="form-item form-item--full">
            <label class="form-item__label">新密码</label>
            <input v-model="pwd.newPassword" class="input" type="password" placeholder="至少 6 位">
          </div>
          <div class="form-item form-item--full">
            <label class="form-item__label">确认新密码</label>
            <input v-model="pwd.confirm" class="input" type="password">
          </div>
        </div>
        <div class="btn-group" style="margin-top: 12px">
          <button class="btn" @click="changePassword">提交修改</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>