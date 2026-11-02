<script setup>
// 模块四：数据可视化与报表 —— 统计分析图表
// 数据全部来自 GET /api/stats/dashboard，一次请求拿全，避免瀑布式请求
import { ref, computed, onMounted } from 'vue'
import ChartBox from '../components/ChartBox.vue'
import { exportExcel, exportPdf, fetchAllPages, formatTime } from '../utils/index'
import { statsApi, speciesApi, observationApi } from '../api/index'
import { useUserStore } from '../store/user'

// 表 3-2「导出 Excel / PDF」是 √ √ × ×：学生与公众没有导出权。
// 导出纯在浏览器里生成、后端没有对应接口，所以这两个按钮本身就是唯一的闸门，
// 必须按角色隐藏，不能只靠「学生看不到统计页」这种间接效果。
const store = useUserStore()

const loading = ref(false)
const data = ref(null)
// load 的在飞序号，用于丢弃过期回包（见 load()）
let loadToken = 0
const activeTab = ref('species')

// 与 DashboardView 一致的配色，保证两个页面视觉统一
const PALETTE = ['#0a6e96', '#0e9c9c', '#f0a500', '#e4572e', '#7b5ea7', '#3c8dbc', '#6b8e23', '#8b7355']

function pieOption(title, pairs) {
  // pairs 来自后端 dashboard，缺字段或接口降级时可能是 undefined；
  // ECharts 收到 undefined 会直接抛错并让整个页面白掉，这里先归一成空数组
  const rows = Array.isArray(pairs) ? pairs : []
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14, color: '#1c2b36' } },
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
      data: rows
    }]
  }
}

function barOption(title, pairs, color) {
  const rows = Array.isArray(pairs) ? pairs : []
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14, color: '#1c2b36' } },
    color: [color],
    tooltip: { trigger: 'axis' },
    grid: { left: 45, right: 30, top: 60, bottom: 50 },
    xAxis: { type: 'category', data: rows.map(p => p.name), axisLabel: { rotate: rows.length > 6 ? 30 : 0, fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', barMaxWidth: 36, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: rows.map(p => p.value) }]
  }
}

const ecosystemOption = computed(() => {
  const rows = data.value?.ecosystemStats || []
  return {
    title: { text: '各生态系统的观测次数与发现物种数', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, data: ['观测次数', '发现物种数'] },
    grid: { left: 45, right: 45, top: 60, bottom: 55 },
    xAxis: { type: 'category', data: rows.map(r => r.name), axisLabel: { rotate: 20, fontSize: 11 } },
    yAxis: [
      { type: 'value', name: '观测次数', minInterval: 1 },
      { type: 'value', name: '物种数', minInterval: 1 }
    ],
    series: [
      { name: '观测次数', type: 'bar', barMaxWidth: 32, itemStyle: { color: '#0a6e96', borderRadius: [4, 4, 0, 0] }, data: rows.map(r => r.observationCount) },
      { name: '发现物种数', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 8, itemStyle: { color: '#e4572e' }, data: rows.map(r => r.speciesCount) }
    ]
  }
})

const monthlyOption = computed(() => {
  const rows = data.value?.monthlyStats || []
  return {
    title: { text: '逐月观测次数趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 45, right: 30, top: 60, bottom: 45 },
    xAxis: { type: 'category', data: rows.map(r => r.name), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line', smooth: true, symbolSize: 6,
      lineStyle: { width: 3, color: '#0a6e96' },
      itemStyle: { color: '#0a6e96' },
      areaStyle: { color: 'rgba(10,110,150,0.12)' },
      data: rows.map(r => r.value)
    }]
  }
})

const observerOption = computed(() => ({
  title: { text: '观测人员活跃度（观测次数 Top 10）', left: 'center', textStyle: { fontSize: 14 } },
  color: ['#0e9c9c'],
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 90, right: 40, top: 60, bottom: 30 },
  xAxis: { type: 'value', minInterval: 1 },
  yAxis: { type: 'category', data: (data.value?.observerStats || []).map(r => r.name), axisLabel: { fontSize: 11 } },
  series: [{ type: 'bar', barMaxWidth: 20, itemStyle: { borderRadius: [0, 4, 4, 0] }, data: (data.value?.observerStats || []).map(r => r.value) }]
}))

async function load() {
  // 在飞序号：刷新按钮与 onMounted 都能触发 load，回包乱序时旧响应会盖掉新数据，
  // 且旧请求的 finally 会提前把 loading 置 false（看起来还是旧的）
  const token = ++loadToken
  loading.value = true
  try {
    const res = await statsApi.dashboard()
    if (token !== loadToken) return
    data.value = res
  } catch (e) {
    if (token === loadToken) alert(e?.message || '统计加载失败，请稍后重试')
  } finally {
    if (token === loadToken) loading.value = false
  }
}

// 导出：交给 utils 里的 fetchAllPages 分页拉全量（含 total 换算与 size=100 的夹值约定）

async function exportSpecies() {
  try {
    const records = await fetchAllPages(speciesApi.list, {})
    await exportExcel('物种信息统计', '物种', [
      { header: '中文名', key: 'chineseName', width: 16 },
      { header: '学名', key: 'scientificName', width: 22 },
      { header: '门', key: 'phylum', width: 12 },
      { header: '纲', key: 'className', width: 12 },
      { header: '目', key: 'orderName', width: 12 },
      { header: '科', key: 'familyName', width: 14 },
      { header: '保护等级', key: 'protectionLevel', width: 12 },
      { header: '濒危度', key: 'endangerStatus', width: 10 },
      { header: '分布区域', key: 'distribution', width: 28 },
      { header: '经度', key: 'longitude', width: 11 },
      { header: '纬度', key: 'latitude', width: 11 },
      { header: '是否公开', key: 'isPublic', width: 10 }
    ], records.map(s => ({ ...s, isPublic: s.isPublic ? '是' : '否' })))
  } catch (e) {
    alert(e.message)
  }
}

async function exportObservations() {
  try {
    const records = await fetchAllPages(observationApi.list, {})
    await exportExcel('观测记录统计', '观测记录', [
      { header: '观测时间', key: 'observeTime', width: 20 },
      { header: '生态系统', key: 'ecosystemName', width: 16 },
      { header: '观测地点', key: 'locationName', width: 22 },
      { header: '经度', key: 'longitude', width: 11 },
      { header: '纬度', key: 'latitude', width: 11 },
      { header: '水温(℃)', key: 'waterTemp', width: 10 },
      { header: '盐度(‰)', key: 'salinity', width: 10 },
      { header: '水深(m)', key: 'depth', width: 10 },
      { header: '天气', key: 'weather', width: 10 },
      { header: '备注', key: 'notes', width: 32 }
    ], records.map(o => ({
      ...o,
      observeTime: formatTime(o.observeTime),
      ecosystemName: o.ecosystem?.name || ''
    })))
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">统计分析与报表</h2>
        <p class="page__desc">模块四：物种分布、生态系统构成、观测趋势与人员活跃度的多维统计，支持导出 Excel 与打印 PDF</p>
      </div>
      <div class="page__actions">
        <button class="btn" @click="load">刷新</button>
        <button v-if="store.canEdit" class="btn btn--ghost" @click="exportSpecies">导出物种 Excel</button>
        <button v-if="store.canEdit" class="btn btn--ghost" @click="exportObservations">导出观测 Excel</button>
        <button v-if="store.canEdit" class="btn btn--ghost" @click="exportPdf">打印 / 导出 PDF</button>
      </div>
    </div>

    <div v-if="loading" class="loading">统计中，请稍候…</div>

    <template v-else-if="data">
      <div class="filter-bar">
        <div class="btn-group">
          <button class="btn" :class="{ 'tab-btn--active': activeTab === 'species' }" @click="activeTab = 'species'">物种统计</button>
          <button class="btn" :class="{ 'tab-btn--active': activeTab === 'observation' }" @click="activeTab = 'observation'">观测统计</button>
        </div>
        <span class="pagination__info">统计口径为全量数据，不受列表分页影响</span>
      </div>

      <div v-show="activeTab === 'species'" class="chart-grid">
        <div class="chart"><ChartBox :option="pieOption('各分类单元（门）物种数量占比', data.phylum)" /></div>
        <div class="chart"><ChartBox :option="pieOption('保护等级分布', data.protectionLevel)" /></div>
        <div class="chart"><ChartBox :option="pieOption('濒危度分布', data.endangerStatus)" /></div>
        <div class="chart"><ChartBox :option="barOption('各门物种数量对比', data.phylum, '#0a6e96')" /></div>
      </div>

      <div v-show="activeTab === 'observation'" class="chart-grid">
        <div class="chart chart--wide"><ChartBox :option="ecosystemOption" /></div>
        <div class="chart chart--wide"><ChartBox :option="monthlyOption" /></div>
        <!-- 后端只把观测人活跃度发给管理员与科研人员（里面是真实姓名），学生看不到这个键 -->
        <div v-if="data.observerStats" class="chart chart--wide"><ChartBox :option="observerOption" /></div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.chart--wide {
  grid-column: 1 / -1;
}

.tab-btn--active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
</style>
