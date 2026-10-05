<script setup>
// 模块一：用户活动日志记录（仅系统管理员可访问，与 SecurityConfig 的 /api/logs/** 规则对应）
import { ref, reactive, onMounted } from 'vue'
import { logApi } from '../api/index'
import Pagination from '../components/Pagination.vue'
import { formatTime, toApiPage } from '../utils/index'

const filters = reactive({ module: '', username: '', operation: '' })
const page = ref(1)
const size = 15
const list = ref({ records: [], total: 0 })
const error = ref('')

// 过滤项直接照抄 operation_logs.module 的取值，避免手输错字查不到
const MODULES = ['模块一', '模块二', '模块三', '模块四', '模块五']
// 过滤项必须与后端 LogService.record(...) 实际写入的 operation 完全一致，否则必然查空
const OPERATIONS = [
  '用户登录', '新增物种', '编辑物种', '删除物种',
  '新增观测记录', '编辑观测记录', '删除观测记录', '删除生态系统',
  '审核通过', '审核驳回', '调整角色', '重置密码', '上传图片',
  '导出', '查询'
]

function tagClass(module) {
  return {
    模块一: 'tag--muted',
    模块二: 'tag--success',
    模块三: 'tag--warning',
    模块四: 'tag--danger',
    模块五: 'tag'
  }[module] || 'tag--muted'
}

async function load() {
  error.value = ''
  try {
    list.value = await logApi.list({ ...filters, page: toApiPage(page.value), size })
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

onMounted(load)
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">用户活动日志</h2>
        <p class="page__desc">模块一：记录登录登出、增删改、审核、角色分配等关键操作，用于追溯与审计</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <div class="card">
      <div class="filter-bar">
        <select v-model="filters.module" class="select">
          <option value="">全部模块</option>
          <option v-for="m in MODULES" :key="m" :value="m">{{ m }}</option>
        </select>
        <input v-model="filters.username" class="input" placeholder="操作人">
        <select v-model="filters.operation" class="select">
          <option value="">全部操作</option>
          <option v-for="o in OPERATIONS" :key="o" :value="o">{{ o }}</option>
        </select>
        <button class="btn" @click="search">查询</button>
        <button class="btn btn--ghost" @click="filters.module = ''; filters.username = ''; filters.operation = ''; search()">重置</button>
      </div>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th><th>时间</th><th>操作人</th><th>所属模块</th>
              <th>操作</th><th>对象</th><th>详情</th><th>来源 IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in list.records" :key="log.id">
              <td>{{ log.id }}</td>
              <td>{{ formatTime(log.createTime) }}</td>
              <td>{{ log.username || '匿名' }}</td>
              <td><span class="tag" :class="tagClass(log.module)">{{ log.module || '—' }}</span></td>
              <td>{{ log.operation }}</td>
              <td>{{ log.targetType ? `${log.targetType} #${log.targetId ?? '-'}` : '—' }}</td>
              <td class="table__ellipsis" :title="log.detail">{{ log.detail || '—' }}</td>
              <td>{{ log.ip || '—' }}</td>
            </tr>
            <tr v-if="!list.records.length">
              <td colspan="8" class="table__empty">没有符合条件的日志</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination :page="page" :size="size" :total="list.total" @change="changePage" />
    </div>
  </div>
</template>
