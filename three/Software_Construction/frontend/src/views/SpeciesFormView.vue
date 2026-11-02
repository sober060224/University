<template>
  <div>
    <div class="page__head">
      <div>
        <h2 class="page__title">{{ isEdit ? '编辑物种信息' : '新增物种信息' }}</h2>
        <p class="page__desc">带 * 的为必填项，其余可留空后续补充</p>
      </div>
      <div class="page__actions">
        <button class="btn btn--ghost" @click="$router.back()">取消</button>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">名称与分类</h3>
      <div class="form">
        <div class="form-item">
          <label class="form-item__label">中文名 *</label>
          <input v-model="form.chineseName" class="input" placeholder="如：珍珠牡蛎" />
        </div>
        <div class="form-item">
          <label class="form-item__label">学名</label>
          <input v-model="form.scientificName" class="input" placeholder="如：Pinctada maxima" />
        </div>
        <div class="form-item">
          <label class="form-item__label">门</label>
          <input v-model="form.phylum" class="input" />
        </div>
        <div class="form-item">
          <label class="form-item__label">纲</label>
          <input v-model="form.className" class="input" />
        </div>
        <div class="form-item">
          <label class="form-item__label">目</label>
          <input v-model="form.orderName" class="input" />
        </div>
        <div class="form-item">
          <label class="form-item__label">科</label>
          <input v-model="form.familyName" class="input" />
        </div>
        <div class="form-item">
          <label class="form-item__label">属</label>
          <input v-model="form.genusName" class="input" />
        </div>
        <div class="form-item">
          <label class="form-item__label">种</label>
          <input v-model="form.speciesName" class="input" />
        </div>
      </div>

      <!-- 模块五：文本辅助分类与补全 -->
      <div class="btn-group" style="margin-top: 12px">
        <button class="btn" :disabled="completing" @click="autoComplete">
          {{ completing ? '大模型补全中…' : '智能补全分类与描述' }}
        </button>
        <span class="page__desc">输入中文名或学名后调用大模型自动补全门纲目科属种、形态特征与生活习性</span>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">形态与习性</h3>
      <div class="form">
        <div class="form-item form-item--full">
          <label class="form-item__label">形态特征</label>
          <textarea v-model="form.morphology" class="textarea" rows="4"></textarea>
        </div>
        <div class="form-item form-item--full">
          <label class="form-item__label">生活习性</label>
          <textarea v-model="form.habits" class="textarea" rows="4"></textarea>
        </div>
        <div class="form-item form-item--full">
          <label class="form-item__label">分布区域</label>
          <input v-model="form.distribution" class="input" placeholder="如：广东湛江雷州半岛浅海" />
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">保护与坐标</h3>
      <div class="form">
        <div class="form-item">
          <label class="form-item__label">保护等级</label>
          <select v-model="form.protectionLevel" class="select">
            <option value="">未评估</option>
            <option v-for="lv in protectionLevels" :key="lv" :value="lv">{{ lv }}</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-item__label">濒危度</label>
          <select v-model="form.endangerStatus" class="select">
            <option value="">未评估</option>
            <option v-for="lv in endangerLevels" :key="lv" :value="lv">{{ lv }}</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-item__label">经度</label>
          <input v-model.number="form.longitude" class="input" type="number" step="0.000001" />
        </div>
        <div class="form-item">
          <label class="form-item__label">纬度</label>
          <input v-model.number="form.latitude" class="input" type="number" step="0.000001" />
        </div>
        <div class="form-item form-item--full">
          <label class="form-item__label">
            <input v-model="form.isPublic" type="checkbox" /> 对公众公开（不勾选则仅管理员与科研人员可见）
          </label>
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="card__title">影像与文献</h3>
      <div class="form">
        <div class="form-item form-item--full">
          <label class="form-item__label">物种图片</label>
          <input type="file" accept="image/*" class="input" @change="upload" />
          <img v-if="form.imageUrl" :src="form.imageUrl" class="detail-photo" style="width:160px;margin-top:8px" />
        </div>
        <div class="form-item">
          <label class="form-item__label">视频链接</label>
          <input v-model="form.videoUrl" class="input" placeholder="https://" />
        </div>
        <div class="form-item">
          <label class="form-item__label">参考文献</label>
          <textarea v-model="form.reference" class="textarea" rows="3"></textarea>
        </div>
      </div>
    </div>

    <div class="btn-group">
      <button class="btn btn--ghost" @click="$router.back()">取消</button>
      <button class="btn" :disabled="saving" @click="submit">{{ saving ? '保存中…' : '保存' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { speciesApi, aiApi, uploadApi } from '../api'
import { PROTECTION_LEVELS } from '../utils'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id

// 取值必须与 database.sql 的 protection_level / endanger_status 以及 AiService
// 提示词里让模型填的那套词一致。原来这里写的是 一级保护/二级保护/重点保护/一般保护，
// 库里存的却是 国家二级/自治区重点/无：编辑一个已有物种时，select 的值不在 options 里，
// 下拉框显示成空白，用户以为这个字段没填，随手一保存就把原来的等级清掉了。
const protectionLevels = PROTECTION_LEVELS.map((item) => item.name)
const endangerLevels = ['未评估', '无危', '近危', '易危', '濒危', '极危']

const blank = () => ({
  chineseName: '', scientificName: '', phylum: '', className: '', orderName: '',
  familyName: '', genusName: '', speciesName: '', morphology: '', habits: '',
  distribution: '', longitude: null, latitude: null, protectionLevel: '',
  endangerStatus: '', imageUrl: '', videoUrl: '', reference: '', isPublic: true
})
const form = reactive(blank())
const saving = ref(false)
const completing = ref(false)
const uploading = ref(false)

// 智能补全写回字段时用的中文名，弹窗里要给用户看中文而不是字段名
const LABELS = {
  scientificName: '学名', phylum: '门', className: '纲', orderName: '目',
  familyName: '科', genusName: '属', speciesName: '种', morphology: '形态特征',
  habits: '生活习性', distribution: '分布', protectionLevel: '保护级别',
  endangerStatus: '濒危等级'
}

onMounted(async () => {
  if (isEdit) {
    try {
      const data = await speciesApi.detail(route.params.id)
      Object.keys(form).forEach((k) => {
        if (data[k] !== null && data[k] !== undefined) form[k] = data[k]
      })
    } catch (e) {
      // 加载失败必须退出：否则留下的是一个看着正常、实则全空的表单，
      // 而 isPublic 默认 true，用户只补中文名就能把原本未公开的物种发布出去。
      alert(e.message)
      router.push('/species')
    }
  }
})

// 模块五：输入中文名或学名 → 大模型自动补全分类信息
async function autoComplete() {
  const name = form.chineseName || form.scientificName
  if (!name) {
    alert('请先填写中文名或学名')
    return
  }
  completing.value = true
  try {
    const res = await aiApi.complete({ name })
    // 接口返回的字段已经平铺在 data 上（axios 拦截器剥掉了外层信封）
    const r = res || {}
    const filled = []
    const pick = (key, label) => {
      if (r[key] && !form[key]) {
        form[key] = r[key]
        filled.push(label)
      }
    }
    ;['scientificName', 'phylum', 'className', 'orderName', 'familyName', 'genusName', 'speciesName',
      'morphology', 'habits', 'distribution', 'protectionLevel', 'endangerStatus']
      .forEach((k) => pick(k, LABELS[k] || k))
    alert(filled.length ? `已自动补全：${filled.join('、')}` : '大模型没有返回可补全的新字段')
  } catch (e) {
    alert(e.message)
  } finally {
    completing.value = false
  }
}

async function upload(e) {
  const file = e.target.files[0]
  if (!file) return
  // 与 img 标签同口径：后端硬上限 5MB（multipart），前端不先拦就会白传 5MB 再吃一个 400
  if (file.size > 5 * 1024 * 1024) {
    alert('图片不能超过 5MB')
    e.target.value = ''
    return
  }
  // accept="image/*" 可被绕过：改名的非图片会白传一次再到后端被魔数校验报错
  if (!/^image\/(jpeg|png|gif|webp)$/.test(file.type)) {
    alert('只支持 JPG / PNG / GIF / WebP 图片')
    e.target.value = ''
    return
  }
  // 在飞锁：input 在上传期间仍可点，连选两次会并发跑两个 POST
  if (uploading.value) return
  uploading.value = true
  try {
    const res = await uploadApi.image(file)
    form.imageUrl = res.url
  } catch (e) {
    alert(e?.message || '上传失败，请稍后重试')
  } finally {
    uploading.value = false
    // 清空 input，连续上传同一张图片时才会再次触发 change
    e.target.value = ''
  }
}

async function submit() {
  // 按钮的 :disabled 要等下一帧，双击会发出两次 POST（新增就是两条重复物种）
  if (saving.value) return
  if (!form.chineseName.trim()) {
    alert('中文名不能为空')
    return
  }
  saving.value = true
  try {
    if (isEdit) {
      await speciesApi.update(route.params.id, form)
    } else {
      await speciesApi.create(form)
    }
    router.push('/species')
  } catch (e) {
    // 后端校验失败会带上具体字段原因，不弹出来等于保存按钮按了没反应
    alert(e.message)
  } finally {
    saving.value = false
  }
}
</script>
