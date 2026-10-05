<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">观测记录</h2>
        <p class="page__desc">按时间、地点、生态系统、物种等条件组合筛选野外与实验室观测事件</p>
      </div>
      <div class="page__actions">
        <button v-if="store.canEdit" class="btn" @click="$router.push('/observations/new')">新增观测记录</button>
        <button class="btn btn--ghost" @click="doExportExcel">导出 Excel</button>
        <button class="btn btn--ghost" @click="exportPdf">导出 PDF</button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <input v-model="query.keyword" class="input" placeholder="地点或备注关键字" />
        <select v-model="query.ecosystemId" class="select">
          <option value="">全部生态系统</option>
          <option v-for="e in ecosystems" :key="e.id" :value="e.id">{{ e.name }}</option>
        </select>
        <select v-model="query.speciesId" class="select">
          <option value="">全部物种</option>
          <option v-for="s in species" :key="s.id" :value="s.id">{{ s.chineseName }}</option>
        </select>
        <input v-model="query.from" class="input" type="datetime-local" title="开始时间" />
        <input v-model="query.to" class="input" type="datetime-local" title="结束时间" />
        <button class="btn" @click="doSearch">查询</button>
        <button class="btn btn--ghost" @click="reset">重置</button>
      </div>
    </div>

    <div class="card">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>观测时间</th><th>生态系统</th><th>观测地点</th>
              <th>环境参数</th><th v-if="store.canEdit">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>
                <a href="#" @click.prevent="toggle(row)">{{ formatTime(row.observeTime) }} ▾</a>
              </td>
              <td>{{ row.ecosystem?.name || '—' }}</td>
              <td>
                {{ row.locationName || '—' }}
                <div class="muted" v-if="row.longitude != null">{{ row.longitude }}, {{ row.latitude }}</div>
              </td>
              <td class="muted">
                <span v-if="row.waterTemp != null">水温 {{ row.waterTemp }}℃</span>
                <span v-if="row.salinity != null"> · 盐度 {{ row.salinity }}‰</span>
                <span v-if="row.depth != null"> · 水深 {{ row.depth }}m</span>
                <span v-if="!row.waterTemp && !row.salinity && !row.depth">—</span>
              </td>
              <td v-if="store.canEdit">
                <div class="table__actions">
                  <button class="btn btn--sm" @click="$router.push(`/observations/${row.id}/edit`)">编辑</button>
                  <button class="btn btn--sm btn--danger" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="table__empty" :colspan="store.canEdit ? 5 : 4">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination :page="query.page" :size="query.size" :total="total" @change="onPage" />
    </div>

    <div v-if="expanded" class="card">
      <h3 class="card__title">关联物种（{{ expanded.species.length }} 种）</h3>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>中文名</th><th>学名</th><th>估算数量</th><th>观察行为</th><th>备注</th></tr>
          </thead>
          <tbody>
            <tr v-for="s in expanded.species" :key="s.linkId">
              <td>{{ s.chineseName }}</td>
              <td class="muted">{{ s.scientificName || '—' }}</td>
              <td>{{ s.count ?? 0 }}</td>
              <td>{{ s.behavior || '—' }}</td>
              <td class="muted">{{ s.note || '—' }}</td>
            </tr>
            <tr v-if="!expanded.species.length">
              <td class="table__empty" colspan="5">该记录尚未关联物种</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="btn-group"><button class="btn btn--ghost" @click="expanded = null">收起</button></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { observationApi, ecosystemApi, speciesApi } from '../api'
import { useUserStore } from '../store/user'
import { formatTime, exportExcel, exportPdf, toApiPage } from '../utils'

const store = useUserStore()
const rows = ref([])
const ecosystems = ref([])
const species = ref([])
const total = ref(0)
const expanded = ref(null)

const query = reactive({
  page: 1, size: 10, keyword: '', ecosystemId: '', speciesId: '', from: '', to: ''
})

onMounted(async () => {
  // 物种列表接口返回分页对象，这里只取 records，否则物种下拉会渲染成 { records, total }
  const [eco, sp] = await Promise.all([ecosystemApi.list(), speciesApi.list({ size: 100 })])
  ecosystems.value = eco
  species.value = sp.records || []
  await load()
})

async function load() {
  const data = await observationApi.list({ ...query, page: toApiPage(query.page) })
  rows.value = data.records
  total.value = data.total
}

// 点观测时间展开关联物种 —— 模块三的核心交互点
async function toggle(row) {
  expanded.value = expanded.value && expanded.value.id === row.id ? null : await observationApi.detail(row.id)
}

function onPage(p) {
  query.page = p
  load()
}

function doSearch() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', ecosystemId: '', speciesId: '', from: '', to: '' })
  load()
}

async function remove(row) {
  if (!confirm(`确定删除 ${formatTime(row.observeTime)} 的观测记录？关联的物种信息会一并删除。`)) return
  await observationApi.remove(row.id)
  await load()
}

function exportExcelData() {
  // 列定义用 exportExcel 约定的 { header, key }；观测人员一列接口不返回，不再导出
  const columns = [
    { header: '观测时间', key: 'observeTime', width: 18 },
    { header: '生态系统', key: 'ecosystemName', width: 14 },
    { header: '观测地点', key: 'locationName', width: 22 },
    { header: '经度', key: 'longitude', width: 12 },
    { header: '纬度', key: 'latitude', width: 12 },
    { header: '水温(℃)', key: 'waterTemp', width: 10 },
    { header: '盐度(‰)', key: 'salinity', width: 10 },
    { header: '水深(m)', key: 'depth', width: 10 },
    { header: '天气', key: 'weather', width: 10 },
    { header: '备注', key: 'notes', width: 30 }
  ]
  return {
    columns,
    rows: rows.value.map((r) => ({
      ...r,
      observeTime: formatTime(r.observeTime),
      // 接口返回的是 ecosystem 对象，导出要的是名字，这里摊平一层
      ecosystemName: r.ecosystem?.name || ''
    }))
  }
}

function doExportExcel() {
  const { columns, rows: data } = exportExcelData()
  exportExcel('观测记录', '观测记录', columns, data)
}
</script>
