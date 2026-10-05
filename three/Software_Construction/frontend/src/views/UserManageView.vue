<script setup>
// 模块一：用户与权限管理
// 管理员负责注册审核、角色分配、密码重置；个人资料见独立的「个人资料」页面。
import { ref, reactive, computed, onMounted } from 'vue'
import { userApi } from '../api/index'
import { useUserStore } from '../store/user'
import Pagination from '../components/Pagination.vue'
import { ROLE_NAMES } from '../store/user'
import { formatTime, toApiPage } from '../utils/index'

const store = useUserStore()
const notice = ref('')
const error = ref('')

const STATUS_NAMES = { PENDING: '待审核', ACTIVE: '正常', REJECTED: '已拒绝' }
const ROLE_OPTIONS = ['PUBLIC', 'STUDENT', 'RESEARCHER', 'ADMIN']

const filters = reactive({ status: '', role: '', keyword: '' })
const page = ref(1)
const size = 10
const list = ref({ records: [], total: 0 })
const overview = ref({})

// ---------- 管理员功能 ----------
const pending = ref([])
const editing = ref(null)
const resetTarget = ref(null)
const resetValue = ref('')

const isAdmin = computed(() => store.isAdmin)

async function load() {
  error.value = ''
  try {
    if (isAdmin.value) {
      list.value = await userApi.list({ ...filters, page: toApiPage(page.value), size })
      overview.value = await userApi.overview()
      // 待审核列表固定取第一页，page 是 0 基
      const res = await userApi.list({ status: 'PENDING', page: 0, size: 50 })
      pending.value = res.records
    }
  } catch (e) {
    error.value = e.message
  }
}

function search() {
  page.value = 1
  load()
}

function changePage(next) {
  page.value = next
  load()
}

async function approve(user, approved) {
  error.value = ''
  try {
    await userApi.approve(user.id, { approved })
    notice.value = approved ? `已通过 ${user.username} 的注册申请` : `已拒绝 ${user.username} 的注册申请`
    load()
  } catch (e) {
    error.value = e.message
  }
}

function startEdit(user) {
  editing.value = { id: user.id, username: user.username, role: user.role }
}

async function saveRole() {
  error.value = ''
  try {
    await userApi.assignRole(editing.value.id, { role: editing.value.role })
    notice.value = `已将 ${editing.value.username} 的角色改为${ROLE_NAMES[editing.value.role]}`
    editing.value = null
    load()
  } catch (e) {
    error.value = e.message
  }
}

function startReset(user) {
  resetTarget.value = user
  resetValue.value = ''
}

async function doReset() {
  error.value = ''
  try {
    await userApi.resetPassword(resetTarget.value.id, { newPassword: resetValue.value })
    notice.value = `已重置 ${resetTarget.value.username} 的密码`
    resetTarget.value = null
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  await store.load()
  load()
})
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">用户与权限管理</h2>
        <p class="page__desc">模块一：注册申请审核、角色分配（管理员 / 科研人员 / 学生 / 公众）</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="notice" class="alert alert--success">{{ notice }}</div>
    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <template v-if="isAdmin">
      <!-- 待审核 -->
      <div v-if="pending.length" class="card">
        <div class="card__title">待审核的注册申请（{{ pending.length }}）</div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr><th>用户名</th><th>姓名</th><th>申请身份</th><th>学号</th><th>联系方式</th><th>申请时间</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="u in pending" :key="u.id">
                <td>{{ u.username }}</td>
                <td>{{ u.realName }}</td>
                <td><span class="tag tag--muted">{{ ROLE_NAMES[u.role] || u.role }}</span></td>
                <td>{{ u.studentNo || '—' }}</td>
                <td>{{ u.phone || u.email || '—' }}</td>
                <td>{{ formatTime(u.createTime) }}</td>
                <td class="table__actions">
                  <button class="btn btn--sm" @click="approve(u, true)">通过</button>
                  <button class="btn btn--sm btn--danger" @click="approve(u, false)">拒绝</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 概览 -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-card__label">用户总数</div>
          <div class="stat-card__value">{{ overview.total ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">正常账号</div>
          <div class="stat-card__value">{{ overview.active ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">待审核</div>
          <div class="stat-card__value">{{ overview.pending ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">科研人员</div>
          <div class="stat-card__value">{{ overview.researcher ?? 0 }}</div>
        </div>
      </div>

      <!-- 用户列表 -->
      <div class="card">
        <div class="card__title">全部用户</div>
        <div class="filter-bar">
          <input v-model="filters.keyword" class="input" placeholder="用户名或姓名">
          <select v-model="filters.status" class="select">
            <option value="">全部状态</option>
            <option value="PENDING">待审核</option>
            <option value="ACTIVE">正常</option>
            <option value="REJECTED">已拒绝</option>
          </select>
          <select v-model="filters.role" class="select">
            <option value="">全部角色</option>
            <option v-for="r in ROLE_OPTIONS" :key="r" :value="r">{{ ROLE_NAMES[r] }}</option>
          </select>
          <button class="btn" @click="search">查询</button>
          <button class="btn btn--ghost" @click="filters.keyword = ''; filters.status = ''; filters.role = ''; search()">重置</button>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr><th>ID</th><th>用户名</th><th>姓名</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="u in list.records" :key="u.id">
                <td>{{ u.id }}</td>
                <td>{{ u.username }}</td>
                <td>{{ u.realName }}</td>
                <td><span class="tag">{{ ROLE_NAMES[u.role] || u.role }}</span></td>
                <td>
                  <span class="tag" :class="u.status === 'ACTIVE' ? 'tag--success' : u.status === 'PENDING' ? 'tag--warning' : 'tag--danger'">
                    {{ STATUS_NAMES[u.status] || u.status }}
                  </span>
                </td>
                <td>{{ formatTime(u.createTime) }}</td>
                <td class="table__actions">
                  <button class="btn btn--ghost btn--sm" @click="startEdit(u)">分配角色</button>
                  <button class="btn btn--ghost btn--sm" @click="startReset(u)">重置密码</button>
                </td>
              </tr>
              <tr v-if="!list.records.length">
                <td colspan="7" class="table__empty">没有符合条件的用户</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :page="page" :size="size" :total="list.total" @change="changePage" />
      </div>
    </template>

    <div v-else class="alert alert--info">
      用户与权限管理页面的审核、角色分配功能仅对系统管理员开放。个人资料与密码请前往「个人资料」页面维护。
    </div>

    <!-- 分配角色 -->
    <div v-if="editing" class="modal" @click.self="editing = null">
      <div class="modal__box">
        <h3 class="modal__title">分配角色 —— {{ editing.username }}</h3>
        <div class="form-item">
          <label class="form-item__label">角色</label>
          <select v-model="editing.role" class="select">
            <option v-for="r in ROLE_OPTIONS" :key="r" :value="r">{{ ROLE_NAMES[r] }}</option>
          </select>
        </div>
        <p class="form-item__hint">科研人员与管理员只能由管理员授予；系统会保证至少保留一名管理员。</p>
        <div class="btn-group" style="margin-top: 14px">
          <button class="btn" @click="saveRole">保存</button>
          <button class="btn btn--ghost" @click="editing = null">取消</button>
        </div>
      </div>
    </div>

    <!-- 重置密码 -->
    <div v-if="resetTarget" class="modal" @click.self="resetTarget = null">
      <div class="modal__box">
        <h3 class="modal__title">重置密码 —— {{ resetTarget.username }}</h3>
        <div class="form-item">
          <label class="form-item__label">新密码</label>
          <input v-model="resetValue" class="input" type="password" placeholder="至少 6 位">
        </div>
        <div class="btn-group" style="margin-top: 14px">
          <button class="btn" @click="doReset">确认重置</button>
          <button class="btn btn--ghost" @click="resetTarget = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-item__hint {
  color: var(--text-sub);
  font-size: 12px;
  margin-top: 8px;
}
</style>
