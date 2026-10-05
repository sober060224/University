<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">{{ item.chineseName }}</h2>
        <p class="page__desc">{{ item.scientificName || '暂无学名' }}</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" @click="$router.back()">返回列表</button>
        <button v-if="store.canEdit" class="btn" @click="translate">多语言描述</button>
        <button v-if="store.canEdit" class="btn" @click="$router.push(`/species/${item.id}/edit`)">编辑</button>
      </div>
    </div>

    <div v-if="!item.id" class="card">加载中…</div>

    <template v-else>
      <div class="card">
        <h3 class="card__title">基本信息</h3>
        <div class="detail-photo">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.chineseName" />
          <div v-else class="table__empty">暂无图片</div>
        </div>
        <div class="desc-list">
          <div class="desc-item"><span class="desc-item__label">中文名</span><span class="desc-item__value">{{ item.chineseName }}</span></div>
          <div class="desc-item"><span class="desc-item__label">学名</span><span class="desc-item__value">{{ item.scientificName || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">门</span><span class="desc-item__value">{{ item.phylum || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">纲</span><span class="desc-item__value">{{ item.className || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">目</span><span class="desc-item__value">{{ item.orderName || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">科</span><span class="desc-item__value">{{ item.familyName || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">属</span><span class="desc-item__value">{{ item.genusName || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">种</span><span class="desc-item__value">{{ item.speciesName || '—' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">保护等级</span><span class="desc-item__value"><span v-if="item.protectionLevel" class="tag tag--warning">{{ item.protectionLevel }}</span><span v-else>—</span></span></div>
          <div class="desc-item"><span class="desc-item__label">濒危度</span><span class="desc-item__value"><span v-if="item.endangerStatus" class="tag" :class="endangerClass">{{ item.endangerStatus }}</span><span v-else>—</span></span></div>
          <div class="desc-item"><span class="desc-item__label">是否公开</span><span class="desc-item__value">{{ item.isPublic ? '公开（公众可见）' : '内部数据' }}</span></div>
          <div class="desc-item"><span class="desc-item__label">分布坐标</span><span class="desc-item__value">{{ coordText }}</span></div>
          <div class="desc-item"><span class="desc-item__label">观测记录</span><span class="desc-item__value">{{ observationCount }} 次</span></div>
          <div class="desc-item"><span class="desc-item__label">更新时间</span><span class="desc-item__value">{{ formatTime(item.updateTime) }}</span></div>
        </div>
      </div>

      <div class="card">
        <h3 class="card__title">形态特征</h3>
        <p>{{ item.morphology || '暂无记录' }}</p>
      </div>

      <div class="card">
        <h3 class="card__title">生活习性</h3>
        <p>{{ item.habits || '暂无记录' }}</p>
      </div>

      <div class="card">
        <h3 class="card__title">分布区域</h3>
        <p>{{ item.distribution || '暂无记录' }}</p>
      </div>

      <div v-if="item.reference" class="card">
        <h3 class="card__title">参考文献</h3>
        <p>{{ item.reference }}</p>
      </div>

      <div v-if="translated" class="card">
        <h3 class="card__title">多语言描述（{{ translateLang }}）</h3>
        <pre class="chat__bubble">{{ translated }}</pre>
      </div>

      <div v-if="item.videoUrl" class="card">
        <h3 class="card__title">影像资料</h3>
        <a :href="item.videoUrl" target="_blank" rel="noopener">{{ item.videoUrl }}</a>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { speciesApi, observationApi, aiApi } from '../api'
import { useUserStore } from '../store/user'
import { formatTime } from '../utils'

const route = useRoute()
const store = useUserStore()
const item = ref({})
const translated = ref('')
const translateLang = ref('English')
const observationCount = ref(0)

const coordText = computed(() => {
  if (item.value.longitude == null || item.value.latitude == null) return '—'
  return `${item.value.longitude}, ${item.value.latitude}`
})

const endangerClass = computed(() => {
  const s = item.value.endangerStatus || ''
  if (s.includes('极危') || s.includes('灭绝')) return 'tag--danger'
  if (s.includes('濒危') || s.includes('易危')) return 'tag--warning'
  return 'tag--success'
})

onMounted(async () => {
  item.value = await speciesApi.detail(route.params.id)
  // 模块二/三交叉统计：这个物种被观测过多少次
  const obs = await observationApi.list({ speciesId: item.value.id, size: 1 })
  observationCount.value = obs.total
})

// 模块五：物种描述多语言支持
async function translate() {
  const text = [item.value.chineseName, item.value.morphology, item.value.habits]
    .filter(Boolean)
    .join('。')
  if (!text) {
    alert('该物种还没有可用于翻译的描述内容')
    return
  }
  const res = await aiApi.translate({ text, targetLanguage: translateLang.value })
  translated.value = res.text
}
</script>
