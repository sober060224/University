<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">观测记录</h2>
        <p class="page__desc">按时间、地点、生态系统、物种等条件组合筛选野外与实验室观测事件</p>
      </div>
      <div class="page__actions">
        <button v-if="store.canEdit" class="btn" @click="$router.push('/observations/new')">新增观测记录</button>
        <!-- 导出按 表 3-2 限管理员与科研人员；后端没有导出接口，这两个按钮就是唯一闸门 -->
        <button v-if="store.canEdit" class="btn btn--ghost" @click="doExportExcel">导出 Excel</button>
        <button v-if="store.canEdit" class="btn btn--ghost" @click="exportPdf">导出 PDF</button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <input v-model="query.keyword" class="input" placeholder="观测地点关键字" />
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
                <span v-if="row.waterTemp == null && row.salinity == null && row.depth == null">—</span>
              </td>
              <td v-if="store.canEdit">
                <div class="table__actions">
                  <button class="btn btn--sm" @click="$router.push(`/observations/${row.id}/edit`)">编辑</button>
                  <button class="btn btn--sm btn--danger" @click="remove(row)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="table__empty" :colspan="store.canEdit ? 5 : 4">{{ loadError || '暂无数据' }}</td>
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
import { fetchAllPages, formatTime, exportExcel, exportPdf, toApiPage } from '../utils'

const store = useUserStore()
const rows = ref([])
const ecosystems = ref([])
const species = ref([])
const total = ref(0)
const expanded = ref(null)
const loadError = ref('')

const query = reactive({
  page: 1, size: 10, keyword: '', ecosystemId: '', speciesId: '', from: '', to: ''
})

onMounted(async () => {
  // 下拉数据失败不应阻断列表：allSettled 保证下面的 load() 一定执行，
  // 否则任一请求出错都会让表格停在「暂无数据」，把请求失败伪装成没有数据。
  const [eco, sp] = await Promise.allSettled([ecosystemApi.list(), speciesApi.list({ size: 100 })])
  // 物种列表接口返回分页对象，这里只取 records，否则物种下拉会渲染成 { records, total }
  if (eco.status === 'fulfilled') ecosystems.value = eco.value
  if (sp.status === 'fulfilled') species.value = sp.value?.records || []
  await load()
})

// 每次加载的序号：连续两次 search / 翻页时旧回包晚到会把旧筛选结果写回列表。
let loadToken = 0

async function load() {
  const token = ++loadToken
  // 请求失败时不能停在「暂无数据」：那会把一次报错伪装成库里没有记录
  try {
    const data = await observationApi.list({ ...query, page: toApiPage(query.page) })
    if (token !== loadToken) return
    rows.value = data?.records || []
    total.value = data?.total || 0
    loadError.value = ''
  } catch (e) {
    if (token !== loadToken) return
    rows.value = []
    total.value = 0
    loadError.value = `加载失败：${e.message}`
  }
}

// 点观测时间展开关联物种 —— 模块三的核心交互点
// 接口返回的是 { observation, species }，观测记录本身在 observation 里，没有顶层 id
async function toggle(row) {
  if (expanded.value?.observation?.id === row.id) {
    expanded.value = null
    return
  }
  try {
    expanded.value = await observationApi.detail(row.id)
  } catch (e) {
    alert(`读取该次观测关联的物种失败：${e.message}`)
  }
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
  // 必须接住失败：科研人员只能删自己提交的记录，而删除按钮在每一行都显示，
  // 点到别人那条时后端会回「只能修改自己提交的观测记录」。
  // 原来这里没有 catch，异常直接逃出点击函数，界面上一声不响、
  // 下面那句收起明细和刷新列表也都不会执行，屏幕上仍然留着那条已经删不掉记录。
  try {
    await observationApi.remove(row.id)
  } catch (e) {
    alert(e.message)
    return
  }
  // 删掉的正是当前展开的那条时，必须一起收起，否则下方仍显示已删除记录的关联物种
  if (expanded.value?.observation?.id === row.id) expanded.value = null
  // 删掉的是当前页最后一条且不在第一页时，原地重拉会得到空页（页码指向不存在的页），
  // 用户看到「暂无数据」但其实上一页还有
  if (rows.value.length === 1 && query.page > 1) query.page -= 1
  await load()
}

function exportColumns() {
  // 列定义用 exportExcel 约定的 { header, key }；观测人员一列接口不返回，不再导出
  return [
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
}

function flatten(records) {
  return records.map((r) => ({
    ...r,
    observeTime: formatTime(r.observeTime),
    // 接口返回的是 ecosystem 对象，导出要的是名字，这里摊平一层
    ecosystemName: r.ecosystem?.name || ''
  }))
}

async function doExportExcel() {
  // 和物种列表一样不能只导当前这一页，按当前筛选条件重新分页取全量
  try {
    const { page, size, ...filters } = query
    const records = await fetchAllPages(observationApi.list, filters)
    await exportExcel('观测记录', '观测记录', exportColumns(), flatten(records))
  } catch (e) {
    alert('导出失败：' + e.message)
  }
}
</script>
