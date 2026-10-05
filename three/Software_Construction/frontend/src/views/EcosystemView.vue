<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">生态系统管理</h2>
        <p class="page__desc">维护珊瑚礁、红树林、海草床等海洋生态系统的基本信息</p>
      </div>
      <div class="page__actions">
        <button v-if="store.canEdit" class="btn" @click="open()">新增生态系统</button>
      </div>
    </div>

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>编号</th><th>名称</th><th>代码</th><th>观测次数</th><th>发现物种数</th>
              <th>说明</th><th v-if="store.canEdit">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td><code>{{ row.code || '—' }}</code></td>
              <td>{{ row.observationCount }}</td>
              <td>{{ row.speciesCount }}</td>
              <td class="muted">{{ row.description || '—' }}</td>
              <td v-if="store.canEdit">
                <div class="table__actions">
                  <button class="btn btn--sm" @click="open(row)">编辑</button>
                  <button v-if="store.isAdmin" class="btn btn--sm btn--danger" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="table__empty" :colspan="store.canEdit ? 7 : 6">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="editing" class="card">
      <h3 class="card__title">{{ form.id ? '编辑生态系统' : '新增生态系统' }}</h3>
      <div class="form">
        <div class="form-item">
          <label class="form-item__label">名称 *</label>
          <input v-model="form.name" class="input" placeholder="如：红树林" />
        </div>
        <div class="form-item">
          <label class="form-item__label">代码</label>
          <input v-model="form.code" class="input" placeholder="如：MF" />
        </div>
        <div class="form-item form-item--full">
          <label class="form-item__label">说明</label>
          <textarea v-model="form.description" class="textarea" rows="3"></textarea>
        </div>
      </div>
      <div class="btn-group">
        <button class="btn btn--ghost" @click="editing = false">取消</button>
        <button class="btn" @click="save">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ecosystemApi } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
// /api/ecosystems/stats 一次返回名称 + 观测次数 + 发现物种数，列表页直接用这份数据，
// 不用再发一次 list 请求然后在前端拼。
const rows = ref([])
const editing = ref(false)
const form = reactive({ id: null, name: '', code: '', description: '' })

onMounted(load)

async function load() {
  rows.value = await ecosystemApi.stats()
}

async function open(row) {
  if (row) {
    // 列表数据来自 /ecosystems/stats，只有 id/name/观测次数/物种数，没有 code 与 description。
    // 直接 Object.assign 会把表单里的空串当成用户输入，保存时就把「说明」清空了，所以按 id 取一次完整记录。
    try {
      const { id, name, code, description } = await ecosystemApi.get(row.id)
      Object.assign(form, { id, name, code, description })
    } catch (e) {
      alert(e.message)
      return
    }
  } else {
    Object.assign(form, { id: null, name: '', code: '', description: '' })
  }
  editing.value = true
}

async function save() {
  if (!form.name.trim()) {
    alert('名称不能为空')
    return
  }
  if (form.id) {
    await ecosystemApi.update(form.id, form)
  } else {
    await ecosystemApi.create(form)
  }
  editing.value = false
  await load()
}

async function remove(row) {
  if (!confirm(`确定删除「${row.name}」？该生态系统下的观测记录会一并删除。`)) return
  await ecosystemApi.remove(row.id)
  await load()
}
</script>
