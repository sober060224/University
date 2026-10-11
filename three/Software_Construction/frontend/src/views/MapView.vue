<script setup>
// 模块四：数据可视化与报表 —— 物种分布地图 / 观测地点地图
// 两张图的数据分别来自模块二与模块三，与愿景文档的模块依赖关系一致。
// 底图使用 OpenStreetMap 公共瓦片 + Leaflet 开源库，不依赖任何商业地图 API。
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import L from 'leaflet'
import { speciesApi, observationApi } from '../api/index'
import { formatTime, PROTECTION_LEVELS, protectionColor } from '../utils/index'
import { useUserStore } from '../store/user'

const store = useUserStore()
const mapEl = ref(null)
const loading = ref(false)
const mode = ref('species')
// 地图实例不放进响应式：Leaflet 内部有大量可变状态，被 Vue 代理会出问题
let map = null
let layer = null

// 广东海洋大学（湛江麻章）为中心，覆盖粤西海域
const CENTER = [21.15, 110.42]
const ZOOM = 9

// 观测点按生态系统轮换上色
const ECO_COLORS = ['#0a6e96', '#0e9c9c', '#f0a500', '#e4572e', '#7b5ea7', '#3c8dbc']

const legend = ref([])

// 观测地点地图属于「统计报表与分布地图」，矩阵里公众是 ×，后端也会回 403，
// 所以这个 tab 对公众直接隐藏；物种分布地图本身是公开信息，仍然保留。
const showObservationTab = computed(() => store.canViewStats)

function escapeHtml(text) {
  return String(text ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]))
}

function renderSpecies(points) {
  // 图例只列当前数据里真实出现的等级。原来无条件列出四个等级，
  // 而这四个等级的名字跟库里的取值一个都对不上，等于在图上摆了一份假的图例。
  const present = PROTECTION_LEVELS.filter(level => points.some(p => p.protectionLevel === level.name))
  const others = [...new Set(
    points.map(p => p.protectionLevel).filter(name => name && !PROTECTION_LEVELS.some(l => l.name === name))
  )]
  legend.value = [
    ...present.map(level => ({ name: level.name, color: level.color })),
    ...others.map(name => ({ name, color: protectionColor(name) }))
  ]
  points.forEach(p => {
    const color = protectionColor(p.protectionLevel)
    L.circleMarker([p.latitude, p.longitude], {
      radius: 8, color, weight: 2, fillColor: color, fillOpacity: 0.55
    }).bindPopup(`
        <div class="map-popup">
          <strong>${escapeHtml(p.name)}</strong>
          <em>${escapeHtml(p.scientificName)}</em>
          <span>保护等级：${escapeHtml(p.protectionLevel || '未评定')}</span>
          <span>濒危度：${escapeHtml(p.endangerStatus || '未评定')}</span>
          <span>坐标：${p.longitude}, ${p.latitude}</span>
        </div>`).addTo(layer)
  })
}

function renderObservations(points) {
  const names = []
  points.forEach(p => {
    // 观测地图接口里的 ecosystem 是生态系统名字符串，不是对象
    if (p.ecosystem && !names.includes(p.ecosystem)) names.push(p.ecosystem)
  })
  // 未指定生态系统不在 names 里，indexOf 返回 -1，取模会得到 -1 而非合法下标，
  // 那样颜色是 undefined。所以这里统一归一化成非负下标。
  const colorOf = (eco) => ECO_COLORS[((names.indexOf(eco) % ECO_COLORS.length) + ECO_COLORS.length) % ECO_COLORS.length]
  legend.value = names.map(name => ({ name, color: colorOf(name) }))
  points.forEach(p => {
    const eco = p.ecosystem || '未指定'
    const color = colorOf(eco)
    L.circleMarker([p.latitude, p.longitude], {
      radius: 9, color, weight: 2, fillColor: color, fillOpacity: 0.6
    }).bindPopup(`
        <div class="map-popup">
          <strong>${escapeHtml(p.locationName || '未命名观测点')}</strong>
          <em>${escapeHtml(eco)}</em>
          <span>观测时间：${escapeHtml(formatTime(p.observeTime))}</span>
          <span>发现物种：${p.speciesCount} 种</span>
          <span>坐标：${p.longitude}, ${p.latitude}</span>
        </div>`).addTo(layer)
  })
}

async function load() {
  loading.value = true
  try {
    // 切换图层时先清空旧数据，避免两种标记混在一张图里
    if (layer) layer.remove()
    layer = L.layerGroup().addTo(map)

    if (mode.value === 'observations' && showObservationTab.value) {
      renderObservations(await observationApi.map())
    } else {
      renderSpecies(await speciesApi.map())
    }
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  map = L.map(mapEl.value, { center: CENTER, zoom: ZOOM, scrollWheelZoom: true })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map)
  load()
})

watch(mode, load)
onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">空间分布地图</h2>
        <p class="page__desc">模块四：物种分布点取自模块二，观测地点取自模块三；底图为 OpenStreetMap 开源瓦片</p>
      </div>
      <div class="page__actions">
        <div class="btn-group">
          <button class="btn" :class="{ 'map-tab--active': mode === 'species' }" @click="mode = 'species'">物种分布地图</button>
          <button v-if="showObservationTab" class="btn" :class="{ 'map-tab--active': mode === 'observations' }" @click="mode = 'observations'">观测地点地图</button>
        </div>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card">
      <div class="card__title">图例</div>
      <div class="legend">
        <span v-for="item in legend" :key="item.name" class="legend__item">
          <i class="legend__dot" :style="{ background: item.color }"></i>{{ item.name }}
        </span>
        <span v-if="!legend.length && !loading" class="legend__item">当前范围内暂无可定位的数据点</span>
      </div>
    </div>

    <div v-if="loading" class="loading">地图数据加载中…</div>
    <div ref="mapEl" class="map"></div>
  </div>
</template>

<style scoped>
.map-tab--active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
</style>

<style>
/* 弹窗内容由 innerHTML 注入，样式必须全局，不能用 scoped */
.map-popup {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 13px;
  line-height: 1.5;
  min-width: 170px;
}

.map-popup strong {
  font-size: 14px;
  color: #1c2b36;
}

.map-popup em {
  font-style: italic;
  color: #5b6b78;
  font-size: 12px;
}

.map-popup span {
  color: #5b6b78;
}
</style>
