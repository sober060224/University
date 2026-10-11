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
      <!-- 统计接口失败时不能只显示 0：必须把「没取到」这件事说出来 -->
      <div v-if="statsError" class="alert alert--warn">
        观测统计暂时取不到（{{ statsError }}），下面两列显示为「—」，不代表这些生态系统没有观测记录。
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>编号</th><th>名称</th><th>代码</th>
              <th v-if="showCounts">观测次数</th><th v-if="showCounts">发现物种数</th>
              <th>说明</th><th v-if="store.canEdit">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td><code>{{ row.code || '—' }}</code></td>
              <td v-if="showCounts">{{ row.observationCount ?? '—' }}</td>
              <td v-if="showCounts">{{ row.speciesCount ?? '—' }}</td>
              <td class="muted">{{ row.description || '—' }}</td>
              <td v-if="store.canEdit">
                <div class="table__actions">
                  <button class="btn btn--sm" @click="open(row)">编辑</button>
                  <button v-if="store.isAdmin" class="btn btn--sm btn--danger" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="table__empty" :colspan="colspan">{{ loadError || '暂无数据' }}</td>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ecosystemApi } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
// 名称 / 代码 / 说明来自公开的 /api/ecosystems，观测次数与发现物种数来自
// /api/ecosystems/stats，两者按 id 合并成一张表。
//
// 原来这张表整个由 stats 一个接口驱动，有两个后果：
// 其一，stats 的返回里根本没有 code 与 description，于是「代码」「说明」两列
// 对任何角色都恒定显示「—」（open() 里那句注释正是在绕这个坑）；
// 其二，权限矩阵把统计类接口对公众标为 ×，后端也已经拦下，
// 公众进这一页就只会看到「加载失败：当前角色没有执行该操作的权限」，
// 连本来就公开的生态系统名称也看不成了。
const rows = ref([])
const loadError = ref('')
const statsError = ref('')
const editing = ref(false)
// 观测次数 / 物种数属于统计，公众不请求也不渲染这两列
const showCounts = computed(() => store.canViewStats)
// 统计列对公众隐藏，空行的 colspan 必须跟着变，否则「暂无数据」会溢出表格外
const colspan = computed(() => 4 + (showCounts.value ? 2 : 0) + (store.canEdit ? 1 : 0))
const form = reactive({ id: null, name: '', code: '', description: '' })

onMounted(load)

async function load() {
  try {
    statsError.value = ''
    const [list, stats] = await Promise.all([
      ecosystemApi.list(),
      // 统计拉不动也不能让整页失败：名称列表是公开数据，照常显示。
      // 但失败必须记下来——见下面 observationCount 的赋值，不能把它伪装成 0。
      showCounts.value
          ? ecosystemApi.stats().catch((e) => {
              statsError.value = e.message
              return []
            })
          : Promise.resolve([])
    ])
    const countById = new Map((stats || []).map((s) => [s.id, s]))
    const statsFailed = showCounts.value && statsError.value !== ''
    rows.value = (list || []).map((item) => ({
      ...item,
      // 统计接口失败时显示 null（模板里渲染成 —），而不是 0：
      // 0 会把「一次请求失败」伪装成「这个生态系统没有观测记录」，
      // 而同文件下面的 catch 专门写了「失败不能显示成没有数据」，这里原先漏了一处。
      observationCount: statsFailed ? null : (countById.get(item.id)?.observationCount ?? 0),
      speciesCount: statsFailed ? null : (countById.get(item.id)?.speciesCount ?? 0)
    }))
    loadError.value = ''
  } catch (e) {
    // 失败时不能停在「暂无数据」，那会把一次报错伪装成库里没有生态系统
    rows.value = []
    loadError.value = `加载失败：${e.message}`
  }
}

async function open(row) {
  if (row) {
    // 现在这一行本身就带着 code 与 description，不必再回查一次详情。
    // 另外必须把后端没有的字段归一成空串，否则 null 会被当成用户输入写回表单。
    Object.assign(form, {
      id: row.id,
      name: row.name || '',
      code: row.code || '',
      description: row.description || ''
    })
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
  try {
    if (form.id) {
      await ecosystemApi.update(form.id, form)
    } else {
      await ecosystemApi.create(form)
    }
  } catch (e) {
    // 保存失败时保持弹窗打开，让用户改完再提交；否则弹窗关了、错误也看不到，只能反复点
    alert(e.message)
    return
  }
  editing.value = false
  await load()
}

async function remove(row) {
  if (!confirm(`确定删除「${row.name}」？该生态系统下的观测记录会一并删除。`)) return
  try {
    await ecosystemApi.remove(row.id)
  } catch (e) {
    alert(e.message)
    return
  }
  await load()
}
</script>
