<template>
    <div>
        <div class="page__head">
            <div>
                <h1 class="page__title">物种信息管理</h1>
                <p class="page__desc">模块二 · 物种信息的登记、检索与维护</p>
            </div>
            <div class="page__actions">
                <router-link v-if="store.canEdit" class="btn" to="/species/new">+ 新增物种</router-link>
                <button class="btn btn--ghost" :disabled="!rows.length" @click="onExportExcel">导出 Excel</button>
                <button class="btn btn--ghost" @click="exportPdf">导出 PDF</button>
            </div>
        </div>

        <div class="card">
            <div class="filter-bar">
                <div class="form-item">
                    <label class="form-item__label">名称关键字</label>
                    <input v-model.trim="query.keyword" class="input" placeholder="中文名或学名" @keyup.enter="search"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label">分类单元（门）</label>
                    <select v-model="query.phylum" class="select">
                        <option value="">全部</option>
                        <option v-for="p in phylums" :key="p" :value="p">{{ p }}</option>
                    </select>
                </div>
                <div class="form-item">
                    <label class="form-item__label">保护等级</label>
                    <input v-model.trim="query.protectionLevel" class="input" placeholder="如：国家二级"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label">濒危度</label>
                    <input v-model.trim="query.endangerStatus" class="input" placeholder="如：易危"/>
                </div>
                <div class="form-item">
                    <label class="form-item__label">分布区域</label>
                    <input v-model.trim="query.distribution" class="input" placeholder="如：湛江"/>
                </div>
                <div class="btn-group">
                    <button class="btn" @click="search">查询</button>
                    <button class="btn btn--ghost" @click="onReset">重置</button>
                </div>
            </div>

            <div class="table-wrap">
                <table class="table">
                    <thead>
                    <tr>
                        <th style="width: 64px;">图片</th>
                        <th>中文名</th>
                        <th>学名</th>
                        <th>门 / 纲</th>
                        <th>保护等级</th>
                        <th>濒危度</th>
                        <th>公开</th>
                        <th style="width: 170px;">操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="row in rows" :key="row.id">
                        <td>
                            <img v-if="row.imageUrl" :src="row.imageUrl" class="table__thumb" :alt="row.chineseName"/>
                            <span v-else class="tag tag--muted">无</span>
                        </td>
                        <td><router-link :to="`/species/${row.id}`">{{ row.chineseName }}</router-link></td>
                        <td style="font-style: italic; color: var(--text-sub);">{{ row.scientificName || '—' }}</td>
                        <td>{{ [row.phylum, row.className].filter(Boolean).join(' / ') || '—' }}</td>
                        <td>
                            <span v-if="row.protectionLevel" class="tag">{{ row.protectionLevel }}</span>
                            <span v-else>—</span>
                        </td>
                        <td>
                            <span v-if="row.endangerStatus" class="tag" :class="endangerClass(row.endangerStatus)">
                                {{ row.endangerStatus }}
                            </span>
                            <span v-else>—</span>
                        </td>
                        <td>
                            <span class="tag" :class="row.isPublic ? 'tag--success' : 'tag--muted'">
                                {{ row.isPublic ? '公开' : '内部' }}
                            </span>
                        </td>
                        <td>
                            <div class="table__actions">
                                <router-link class="btn btn--sm btn--ghost" :to="`/species/${row.id}`">详情</router-link>
                                <router-link v-if="store.canEdit" class="btn btn--sm btn--ghost"
                                             :to="`/species/${row.id}/edit`">编辑</router-link>
                                <button v-if="store.isAdmin" class="btn btn--sm btn--danger"
                                        @click="onDelete(row)">删除</button>
                            </div>
                        </td>
                    </tr>
                    <tr v-if="!rows.length && !loading">
                        <td colspan="8" class="table__empty">没有符合条件的物种，试试调整筛选条件</td>
                    </tr>
                    </tbody>
                </table>
            </div>

            <Pagination :page="page" :size="size" :total="total" @change="onPage"/>
        </div>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { speciesApi } from '../api'
import { exportExcel, exportPdf, toApiPage } from '../utils'
import { useUserStore } from '../store/user'
import Pagination from '../components/Pagination.vue'

const store = useUserStore()

const rows = ref([])
const phylums = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const emptyQuery = () => ({ keyword: '', phylum: '', protectionLevel: '', endangerStatus: '', distribution: '' })
const query = reactive(emptyQuery())

const EXPORT_COLUMNS = [
    { header: '中文名', key: 'chineseName', width: 16 },
    { header: '学名', key: 'scientificName', width: 22 },
    { header: '门', key: 'phylum', width: 12 },
    { header: '纲', key: 'className', width: 12 },
    { header: '目', key: 'orderName', width: 12 },
    { header: '科', key: 'familyName', width: 14 },
    { header: '属', key: 'genusName', width: 14 },
    { header: '种', key: 'speciesName', width: 14 },
    { header: '保护等级', key: 'protectionLevel', width: 12 },
    { header: '濒危度', key: 'endangerStatus', width: 12 },
    { header: '分布区域', key: 'distribution', width: 20 },
    { header: '经度', key: 'longitude', width: 12 },
    { header: '纬度', key: 'latitude', width: 12 },
    { header: '是否公开', key: 'isPublic', width: 10 }
]

onMounted(async () => {
    phylums.value = await speciesApi.phylums()
    load()
})

function search() {
    page.value = 1
    load()
}

function onReset() {
    Object.assign(query, emptyQuery())
    search()
}

function onPage(target) {
    page.value = target
    load()
}

async function load() {
    loading.value = true
    try {
        const result = await speciesApi.list({ ...query, page: toApiPage(page.value), size })
        rows.value = result.records
        total.value = result.total
    } catch (e) {
        alert(e.message)
    } finally {
        loading.value = false
    }
}

function onDelete(row) {
    if (!confirm(`确定删除物种「${row.chineseName}」吗？相关的观测记录关联也会一并删除，此操作不可恢复。`)) {
        return
    }
    speciesApi.remove(row.id)
        .then(() => {
            alert('删除成功')
            load()
        })
        .catch((e) => alert(e.message))
}

function onExportExcel() {
    exportExcel('物种信息清单', '物种信息', EXPORT_COLUMNS, rows.value)
        .catch((e) => alert('导出失败：' + e.message))
}

function endangerClass(status) {
    if (/极危|灭绝/.test(status)) {
        return 'tag--danger'
    }
    if (/濒危|易危/.test(status)) {
        return 'tag--warning'
    }
    return 'tag--success'
}
</script>
