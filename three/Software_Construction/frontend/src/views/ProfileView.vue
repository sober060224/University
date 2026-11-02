<script setup>
// 模块一：个人资料
// 所有已登录用户均可访问：左侧维护资料，右侧修改密码。
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api/index'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()
const notice = ref('')
const error = ref('')
// 保存/改密进行中：按钮 :disabled 只在下一帧生效，双击仍可能发出两次 PUT/POST，
// 所以动作本身也要有在飞锁（语义同其它页面的 saving/submitting）
const savingProfile = ref(false)
const savingPwd = ref(false)
// 改密成功后延时跳登录页的定时器句柄
let redirectTimer = null

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
  if (savingProfile.value) return
  // 与后端 UserForms 的 @Size 对齐：前端先挡，避免白跑一次请求
  if (profile.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profile.email)) {
    error.value = '邮箱格式不正确'
    return
  }
  if (profile.phone && !/^[0-9+\-\s]{6,20}$/.test(profile.phone)) {
    error.value = '手机号格式不正确'
    return
  }
  error.value = ''
  savingProfile.value = true
  try {
    // updateProfile 直接回最新用户信息，用它同步 store，避免界面回显旧值
    store.setProfile(await userApi.updateProfile(profile))
    fillProfile()
    notice.value = '个人资料已保存'
  } catch (e) {
    error.value = e?.message || '保存失败，请稍后重试'
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (savingPwd.value) return
  if (!pwd.oldPassword) {
    error.value = '请输入原密码'
    return
  }
  // 与后端 ChangePassword @Size(min=6) 对齐；原先只在占位符里写了「至少 6 位」
  if (pwd.newPassword.length < 6) {
    error.value = '新密码至少 6 位'
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    error.value = '两次输入的新密码不一致'
    return
  }
  error.value = ''
  savingPwd.value = true
  try {
    await userApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    // 后端改密后会话已作废，这里同步清掉本地状态并回登录页
    notice.value = '密码修改成功，请重新登录'
    store.clear()
    // 定时器必须留句柄并在离开页面时清掉：否则用户在这 800ms 内点了别的菜单，
    // 还是会被强行拽回登录页
    redirectTimer = setTimeout(() => router.push('/login'), 800)
  } catch (e) {
    error.value = e?.message || '修改失败，请稍后重试'
  } finally {
    savingPwd.value = false
  }
}

onMounted(async () => {
  await store.load()
  fillProfile()
})

// 会话此刻已在后端作废，即使被清掉跳转，任何一次请求都会由 401 拦截器送回登录页，
// 所以清理定时器不会把用户留在一个已经失效的页面上。
onBeforeUnmount(() => {
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
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
          <button class="btn" :disabled="savingProfile" @click="saveProfile">{{ savingProfile ? '保存中…' : '保存资料' }}</button>
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
          <button class="btn" :disabled="savingPwd" @click="changePassword">{{ savingPwd ? '提交中…' : '提交修改' }}</button>
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