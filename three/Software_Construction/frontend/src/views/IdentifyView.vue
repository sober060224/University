<script setup>
// 模块五 能力一：图像智能识别与物种鉴定
// 调用大模型多模态接口返回最可能物种 + 置信度，并回查本库已有物种记录供一键关联；
// 置信度较低时展示候选列表，走人工确认流程。
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { aiApi, uploadApi } from '../api/index'
import Pagination from '../components/Pagination.vue'
import { formatTime, toApiPage } from '../utils/index'

const imageUrl = ref('')
const preview = ref('')
const result = ref(null)
const identifying = ref(false)
const error = ref('')

const records = ref({ records: [], total: 0 })
const page = ref(1)
const size = 10

const TYPE_NAMES = {
  IDENTIFY: '图像识别', COMPLETE: '文本补全', TRANSLATE: '多语言翻译',
  TAG: '标签与异常检测', QA: '智能问答'
}

// 连续换图时，先发的上传（大图）可能后回包，盖掉后发请求的 imageUrl，
// 界面显示 A 图、识别却用 B 图。用序号丢弃过期回包。
let uploadToken = 0

async function onFileChange(event) {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    error.value = '图片不能超过 5MB'
    return
  }
  // accept="image/*" 只是选择框的提示，用户可以切到「所有文件」或拖入任意文件；
  // 不上传前先挡，否则一个改名的非图片会白花一次多模态调用再在后端报错
  if (!/^image\/(jpeg|png|gif|webp)$/.test(file.type)) {
    error.value = '只支持 JPG / PNG / GIF / WebP 图片'
    return
  }
  error.value = ''
  // 上一次预览的 objectURL 必须显式释放，否则每次换图都会泄漏一份 Blob
  if (preview.value) {
    URL.revokeObjectURL(preview.value)
  }
  // 旧图同时作废：否则上传失败时模板仍渲染已释放的 blob URL（破图），
  // 而 imageUrl 还留着上一张的地址，用户点「开始识别」用的是与界面不一致的图。
  preview.value = ''
  imageUrl.value = ''
  result.value = null
  const token = ++uploadToken
  try {
    const res = await uploadApi.image(file)
    if (token !== uploadToken) return
    imageUrl.value = res.url
    preview.value = URL.createObjectURL(file)
  } catch (e) {
    if (token === uploadToken) error.value = e?.message || '上传失败，请稍后重试'
  }
}

async function identify() {
  if (!imageUrl.value) {
    error.value = '请先上传一张海洋生物图片'
    return
  }
  identifying.value = true
  error.value = ''
  try {
    result.value = await aiApi.identify({ imageUrl: imageUrl.value })
    // 刷新调用记录要等完，否则识别按钮已经恢复、记录表还是旧的
    await loadRecords()
  } catch (e) {
    error.value = e.message
  } finally {
    identifying.value = false
  }
}

function confidencePercent(value) {
  // 后端 confidence 是 0~1 的小数
  return Math.round((value || 0) * 100)
}

function confidenceClass(value) {
  const p = confidencePercent(value)
  if (p >= 75) return 'tag--success'
  if (p >= 50) return 'tag--warning'
  return 'tag--danger'
}

// 与 confidenceClass 用同一套 75 / 50 阈值，报告 5.16 节里的高、中、低分档
function confidenceLabel(value) {
  const p = confidencePercent(value)
  if (p >= 75) return '高'
  if (p >= 50) return '中'
  return '低'
}

// 分页/识别后刷新时旧回包会晚到，把旧记录写成当前页；且不能复用页面级的 error，
// 否则识别成功旁边会挂着「加载记录失败」。序号丢弃过期回包。
let recordsToken = 0

async function loadRecords() {
  const token = ++recordsToken
  try {
    const res = await aiApi.records({ page: toApiPage(page.value), size })
    if (token !== recordsToken) return
    // 模板里直接读 records.records / records.total，接口一旦没按分页结构返回，
    // 渲染期就会抛 undefined.length 而整页白屏，所以这里只接受带 records 的结构。
    records.value = res?.records ? res : { records: [], total: 0 }
  } catch (e) {
    if (token !== recordsToken) return
    records.value = { records: [], total: 0 }
    error.value = e.message
  }
}

function changePage(next) {
  page.value = next
  loadRecords()
}

onMounted(loadRecords)

// 预览用的 objectURL 只在「换下一张图」时释放过，离开本页时最后那一份一直挂在
// Blob 引用上不会被回收；反复进出识别页就会稳定泄漏。
onBeforeUnmount(() => {
  // 作废还在飞的请求，避免卸载后回包又写 state / 改动已销毁的 DOM
  uploadToken++
  recordsToken++
  if (preview.value) {
    URL.revokeObjectURL(preview.value)
    preview.value = ''
  }
})
</script>

<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">图像智能识别与物种鉴定</h2>
        <p class="page__desc">模块五能力一：上传图片调用大模型多模态接口，返回最可能物种与置信度，并推荐关联的已有物种记录</p>
      </div>
    </div>

    <div v-if="error" class="alert alert--error">{{ error }}</div>

    <div class="card">
      <div class="card__title">第一步：上传图片</div>
      <div class="form-item">
        <label class="form-item__label">选择图片</label>
        <input class="input" type="file" accept="image/png,image/jpeg,image/gif,image/webp" @change="onFileChange">
      </div>
      <p class="form-item__hint">支持 jpg / png / gif / webp，单张不超过 5MB。图片仅用于本次识别，不对外公开。</p>
      <div v-if="preview" class="identify-preview">
        <img :src="preview" alt="待识别的海洋生物图片">
      </div>
      <div class="btn-group" style="margin-top: 12px">
        <button class="btn" :disabled="identifying" @click="identify">
          {{ identifying ? '识别中…（大模型响应需要几秒）' : '开始识别' }}
        </button>
      </div>
    </div>

    <div v-if="result" class="card">
      <div class="card__title">第二步：识别结果</div>
      <div class="stat-grid stat-grid--2">
        <div class="stat-card">
          <div class="stat-card__label">最可能物种</div>
          <div class="stat-card__value">{{ result.chineseName || '未能识别' }}</div>
          <div class="stat-card__hint">{{ result.scientificName || '模型未给出学名' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">置信度</div>
          <div class="stat-card__value">
            {{ confidencePercent(result.confidence) }}%
            <span class="tag" :class="confidenceClass(result.confidence)">
              {{ confidenceLabel(result.confidence) }}
            </span>
          </div>
          <div class="confidence-bar"><i :style="{ width: confidencePercent(result.confidence) + '%' }"></i></div>
        </div>
      </div>

      <p v-if="confidencePercent(result.confidence) < 50" class="alert alert--warn">
        置信度偏低，建议对照下方候选列表人工确认后再录入物种信息。
      </p>

      <div v-if="result.description" class="desc-item">
        <span class="desc-item__label">形态描述</span>
        <span class="desc-item__value">{{ result.description }}</span>
      </div>

      <h3 class="card__title" style="margin-top: 16px">推荐关联的已有物种记录</h3>
      <div v-if="!result.candidates?.length" class="alert alert--info">
        本库中暂未匹配到该物种，可先在「物种信息管理」中新增，再回到观测记录里关联。
      </div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>中文名</th><th>学名</th><th>保护等级</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in result.candidates" :key="item.id">
              <td>{{ item.chineseName }}</td>
              <td class="table__sci">{{ item.scientificName }}</td>
              <td><span class="tag">{{ item.protectionLevel || '未评定' }}</span></td>
              <td class="table__actions">
                <RouterLink class="btn btn--ghost btn--sm" :to="`/species/${item.id}`">查看详情</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="card__title">调用记录（可追溯与质量分析）</div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>时间</th><th>能力</th><th>调用结果摘要</th><th>耗时</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in records.records" :key="r.id">
              <td>{{ formatTime(r.createTime) }}</td>
              <td><span class="tag tag--muted">{{ TYPE_NAMES[r.type] || r.type }}</span></td>
              <td class="table__ellipsis">{{ r.result }}</td>
              <td>{{ r.costMs }} ms</td>
            </tr>
            <tr v-if="!records.records.length">
              <td colspan="4" class="table__empty">暂无调用记录</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination :page="page" :size="size" :total="records.total" @change="changePage" />
    </div>
  </div>
</template>

<style scoped>
.identify-preview {
  margin-top: 12px;
  text-align: center;
}

.identify-preview img {
  max-width: 320px;
  max-height: 260px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.confidence-bar {
  margin-top: 8px;
  height: 8px;
  background: var(--border);
  border-radius: 4px;
  overflow: hidden;
}

.confidence-bar i {
  display: block;
  height: 100%;
  background: var(--primary);
}

.stat-grid--2 {
  grid-template-columns: repeat(2, 1fr);
}

.form-item__hint {
  color: var(--text-sub);
  font-size: 12px;
}
</style>
