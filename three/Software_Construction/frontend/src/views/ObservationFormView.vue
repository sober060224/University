<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">{{ isEdit ? '编辑观测记录' : '新增观测记录' }}</h2>
        <p class="page__desc">一次观测可关联一个或多个物种，并为每个物种单独记录估算数量与观察行为</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" @click="$router.back()">取消</button>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">观测信息</h3>
      <div class="form">
        <div class="form-item">
          <label class="form-item__label">生态系统 *</label>
          <select v-model="form.ecosystemId" class="select">
            <option :value="null" disabled>请选择</option>
            <option v-for="e in ecosystems" :key="e.id" :value="e.id">{{ e.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-item__label">观测时间 *</label>
          <input v-model="form.observeTime" class="input" type="datetime-local" />
        </div>
        <div class="form-item">
          <label class="form-item__label">观测地点</label>
          <input v-model="form.locationName" class="input" placeholder="如：湛江海湾浅水区" />
        </div>
        <div class="form-item">
          <label class="form-item__label">经度</label>
          <input v-model.number="form.longitude" class="input" type="number" step="0.000001" />
        </div>
        <div class="form-item">
          <label class="form-item__label">纬度</label>
          <input v-model.number="form.latitude" class="input" type="number" step="0.000001" />
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">环境参数</h3>
      <div class="form">
        <div class="form-item">
          <label class="form-item__label">水温（℃）</label>
          <input v-model.number="form.waterTemp" class="input" type="number" step="0.01" />
        </div>
        <div class="form-item">
          <label class="form-item__label">盐度（‰）</label>
          <input v-model.number="form.salinity" class="input" type="number" step="0.01" />
        </div>
        <div class="form-item">
          <label class="form-item__label">水深（m）</label>
          <input v-model.number="form.depth" class="input" type="number" step="0.1" />
        </div>
        <div class="form-item">
          <label class="form-item__label">天气</label>
          <select v-model="form.weather" class="select">
            <option value="">未记录</option>
            <option v-for="w in weathers" :key="w" :value="w">{{ w }}</option>
          </select>
        </div>
        <div class="form-item form-item--full">
          <label class="form-item__label">备注</label>
          <textarea v-model="form.notes" class="textarea" rows="3"></textarea>
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">关联物种（{{ links.length }} 种）*</h3>
      <div class="btn-group">
        <select v-model="pickId" class="select" style="max-width: 280px">
          <option :value="null" disabled>选择要关联的物种</option>
          <option v-for="s in available" :key="s.id" :value="s.id">{{ s.chineseName }}（{{ s.scientificName || '无学名' }}）</option>
        </select>
        <button class="btn" :disabled="!pickId" @click="addLink">添加</button>
        <span class="page__desc">已关联的物种不会重复出现在下拉列表中</span>
      </div>

      <div v-if="links.length" class="table-wrap" style="margin-top: 12px">
        <table class="table">
          <thead>
            <tr><th>物种</th><th>估算数量</th><th>观察行为</th><th>备注</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="(link, i) in links" :key="link.speciesId">
              <td>{{ nameOf(link.speciesId) }}</td>
              <td><input v-model.number="link.count" class="input" type="number" min="0" style="width: 96px" /></td>
              <td><input v-model="link.behavior" class="input" placeholder="如：觅食、附着" /></td>
              <td><input v-model="link.note" class="input" placeholder="选填" /></td>
              <td><button class="btn btn--sm btn--danger" @click="links.splice(i, 1)">移除</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="table__empty" style="margin-top: 12px">尚未关联物种，一次观测至少需要关联一个物种</div>

      <div class="btn-group" style="margin-top: 12px">
        <button class="btn" :disabled="analyzing" @click="analyze">
          {{ analyzing ? '大模型分析中…' : '智能标签与异常检测' }}
        </button>
        <span class="page__desc">根据地点、时间、生态系统自动生成标签，并检查物种分布是否冲突</span>
      </div>

      <div v-if="analysis" class="alert alert--info" style="margin-top: 12px">
        <p v-if="analysis.tags && analysis.tags.length">
          <strong>自动标签：</strong>
          <span v-for="t in analysis.tags" :key="t" class="tag tag--success">{{ t }}</span>
        </p>
        <p v-if="analysis.summary">{{ analysis.summary }}</p>
        <ul v-if="analysis.warnings && analysis.warnings.length">
          <li v-for="(wmsg, i) in analysis.warnings" :key="i">⚠ {{ wmsg }}</li>
        </ul>
      </div>
    </div>

    <div class="btn-group">
      <button class="btn btn--ghost" @click="$router.back()">取消</button>
      <button class="btn" :disabled="saving" @click="submit">{{ saving ? '保存中…' : '保存' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { observationApi, ecosystemApi, speciesApi, aiApi } from '../api'
import { toInputTime, nowInputTime } from '../utils'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id

const weathers = ['晴', '多云', '阴', '小雨', '阵雨', '大风', '雾']

const ecosystems = ref([])
const species = ref([])
const pickId = ref(null)
const links = ref([])
const saving = ref(false)
const analyzing = ref(false)
const analysis = ref(null)

const form = reactive({
  ecosystemId: null,
  observeTime: nowInputTime(),
  locationName: '', longitude: null, latitude: null,
  waterTemp: null, salinity: null, depth: null, weather: '', notes: ''
})

// 已关联的物种从下拉里去掉，避免 observation_species 唯一键冲突
const used = computed(() => new Set(links.value.map((l) => l.speciesId)))
const available = computed(() => species.value.filter((s) => !used.value.has(s.id)))

function nameOf(id) {
  const s = species.value.find((x) => x.id === id)
  return s ? s.chineseName : id
}

onMounted(async () => {
  // 物种列表接口返回分页对象，这里只取 records，避免把 { records, total } 当数组用
  //
  // 两个下拉必须各自成立，且都不能阻断后面的回填：
  // 原来用 Promise.all，任一请求失败整个 onMounted 就抛出，
  // 于是编辑一条已有记录时详情根本不会被读取，form 还停在 blank() 的默认值上
  // （观测时间是「此刻」、关联物种为空），页面看起来和新增一模一样。
  // 用户在这个假表单上点保存，等于凭空多出一条重复记录。
  const [eco, sp] = await Promise.allSettled([
    ecosystemApi.list(),
    speciesApi.list({ size: 100 })
  ])
  if (eco.status === 'fulfilled') ecosystems.value = eco.value || []
  if (sp.status === 'fulfilled') species.value = sp.value?.records || []
  const failed = [eco, sp].filter((r) => r.status === 'rejected').map((r) => r.reason.message)
  if (failed.length) {
    alert(`下拉选项加载失败：${failed.join('；')}`)
  }

  if (isEdit) {
    try {
      const data = await observationApi.detail(route.params.id)
      const o = data.observation
      Object.keys(form).forEach((k) => {
        if (o[k] !== null && o[k] !== undefined) form[k] = o[k]
      })
      form.observeTime = toInputTime(o.observeTime)
      links.value = data.species.map((s) => ({
        speciesId: s.speciesId, count: s.count, behavior: s.behavior, note: s.note
      }))
    } catch (e) {
      // 读不到原记录就绝不能停在这一页：接着编辑下去，保存的是一张空表单。
      // 403 是后端「只能改自己提交的记录」的拒绝，不是登录失效（401 已被拦截器
      // 统一处理），所以不能只弹窗——用户会反复试。
      const msg = /403|无权|权限/.test(e?.message || '')
        ? '你没有修改这条记录的权限（只能编辑自己提交的观测）'
        : `读取观测记录失败：${e?.message || '未知错误'}`
      alert(msg)
      router.push('/observations')
    }
  }
})

function addLink() {
  // 没选物种就点「添加物种」会插进一条 speciesId 为 null 的空行：
  // 提交时被嵌套校验挡下，弹出的提示是「请选择物种」，但用户面对的是一张多出来的空行，
  // 不知道是哪一条、也删不掉刚加的那条；智能标签功能还会把 nameOf() 兜底返回的数字 id
  // 当成物种名发给大模型。
  if (!pickId.value) {
    alert('请先在下拉框中选择一个物种')
    return
  }
  if (used.value.has(pickId.value)) {
    alert('这个物种已经在关联列表里了')
    return
  }
  links.value.push({ speciesId: pickId.value, count: 1, behavior: '', note: '' })
  pickId.value = null
}

// 模块五：观测记录智能标签与异常检测（选做）
async function analyze() {
  analyzing.value = true
  try {
    // 请求体与后端 Analyze 记录一一对应（平铺字段），不要把草稿再包一层
    const res = await aiApi.analyze({
      observeTime: form.observeTime,
      locationName: form.locationName,
      longitude: form.longitude,
      latitude: form.latitude,
      ecosystemName: (ecosystems.value.find((e) => e.id === form.ecosystemId) || {}).name,
      // nameOf 查不到时会兜底返回数字 id，直接发给大模型就是拿「17」当物种名，
      // 它只会对着一个数字编标签。这里只送真正解析出名字的物种。
      speciesNames: links.value
              .map((l) => nameOf(l.speciesId))
              .filter((name) => typeof name === 'string' && name.trim())
    })
    // 接口返回 { tags, warnings, summary, raw }
    analysis.value = res
  } catch (e) {
    alert(e.message)
  } finally {
    analyzing.value = false
  }
}

async function submit() {
  // 按钮的 :disabled 要等下一帧才生效（Vue 异步更新 DOM），双击的第一帧能连发两次
  // 请求；新增场景就是两条完全一样的记录
  if (saving.value) return
  if (!form.ecosystemId) {
    alert('请选择生态系统')
    return
  }
  if (!form.observeTime) {
    alert('请填写观测时间')
    return
  }
  if (!links.value.length) {
    alert('请至少关联一个物种')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, species: links.value }
    if (isEdit) {
      await observationApi.update(route.params.id, payload)
    } else {
      await observationApi.create(payload)
    }
    router.push('/observations')
  } catch (e) {
    // 后端已经把「重复关联物种」「只能修改自己提交的记录」等业务原因写进 message，
    // 这里必须弹出来，否则保存失败只有按钮恢复可用，用户看不到任何原因。
    alert(e.message)
  } finally {
    saving.value = false
  }
}
</script>
