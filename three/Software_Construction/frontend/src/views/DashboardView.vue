<template>
    <div>
        <div class="page__head">
            <div>
                <h1 class="page__title">综合数据看板</h1>
                <p class="page__desc">集中展示物种、观测、生态系统与用户的关键指标与分布</p>
            </div>
            <div class="page__actions">
                <button class="btn btn--ghost" :disabled="loading" @click="load">刷新</button>
                <button class="btn btn--ghost" @click="exportPdf">导出 PDF</button>
            </div>
        </div>

        <div v-if="loading && !data" class="loading">加载中…</div>

        <template v-else-if="data">
            <!-- 系统概览 -->
            <div class="stat-grid">
                <div class="stat-card">
                    <div class="stat-card__label">物种总数</div>
                    <div class="stat-card__value">{{ data.overview.speciesTotal }}</div>
                    <div class="stat-card__hint">模块二 已登记物种</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card__label">观测记录总数</div>
                    <div class="stat-card__value">{{ data.overview.observationTotal }}</div>
                    <div class="stat-card__hint">本月 {{ data.overview.observationThisMonth }} 次</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card__label">近 30 天观测</div>
                    <div class="stat-card__value">{{ data.overview.observationLast30Days }}</div>
                    <div class="stat-card__hint">近期活跃度</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card__label">生态系统</div>
                    <div class="stat-card__value">{{ data.overview.ecosystemTotal }}</div>
                    <div class="stat-card__hint">珊瑚礁 / 红树林 / 海草床等</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card__label">科研人员</div>
                    <div class="stat-card__value">{{ data.overview.researcherTotal }}</div>
                    <div class="stat-card__hint">系统用户 {{ data.overview.userTotal }} 人</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card__label">待审核用户</div>
                    <div class="stat-card__value">{{ data.overview.pendingUserTotal }}</div>
                    <div class="stat-card__hint">注册申请数量</div>
                </div>
            </div>

            <!-- 分布图 -->
            <div class="chart-grid">
                <div class="card">
                    <h2 class="card__title">各分类单元物种占比</h2>
                    <ChartBox :option="pieOption('phylum')"/>
                </div>
                <div class="card">
                    <h2 class="card__title">保护等级分布</h2>
                    <ChartBox :option="pieOption('protectionLevel')"/>
                </div>
                <div class="card">
                    <h2 class="card__title">各生态系统观测次数与发现物种数</h2>
                    <ChartBox :option="ecosystemOption"/>
                </div>
                <div class="card">
                    <h2 class="card__title">逐月观测次数</h2>
                    <ChartBox :option="monthlyOption"/>
                </div>
                <div class="card">
                    <h2 class="card__title">观测人员活跃度</h2>
                    <ChartBox :option="observerOption"/>
                </div>
                <div class="card">
                    <h2 class="card__title">濒危度分布</h2>
                    <ChartBox :option="pieOption('endangerStatus')"/>
                </div>
            </div>
        </template>
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { statsApi } from '../api'
import { exportPdf } from '../utils'
import ChartBox from '../components/ChartBox.vue'

const data = ref(null)
const loading = ref(false)

const PALETTE = ['#0a6e96', '#0e9c9c', '#3aa6a0', '#6fb1c4', '#f0a04b', '#e2703a', '#8d6e63', '#78909c']

onMounted(load)

async function load() {
    loading.value = true
    try {
        data.value = await statsApi.dashboard()
    } catch (e) {
        alert(e.message)
    } finally {
        loading.value = false
    }
}

function pieOption(key) {
    const rows = data.value[key] || []
    return {
        tooltip: { trigger: 'item', formatter: '{b}: {c} 种 ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        color: PALETTE,
        series: [{
            type: 'pie',
            radius: ['38%', '62%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
            label: { formatter: '{b}\n{c} 种' },
            data: rows
        }]
    }
}

const ecosystemOption = computed(() => ({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0 },
    grid: { left: 50, right: 50, top: 30, bottom: 60 },
    xAxis: { type: 'category', data: (data.value?.ecosystemStats || []).map((i) => i.name), axisLabel: { interval: 0, rotate: 18 } },
    yAxis: [{ type: 'value', name: '观测次数' }, { type: 'value', name: '物种数' }],
    series: [
        { name: '观测次数', type: 'bar', data: (data.value?.ecosystemStats || []).map((i) => i.observationCount), itemStyle: { color: '#0a6e96' }, barMaxWidth: 34 },
        { name: '发现物种数', type: 'line', yAxisIndex: 1, data: (data.value?.ecosystemStats || []).map((i) => i.speciesCount), itemStyle: { color: '#f0a04b' }, smooth: true }
    ]
}))

const monthlyOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 46, right: 20, top: 30, bottom: 46 },
    xAxis: { type: 'category', data: (data.value?.monthlyStats || []).map((i) => i.name) },
    yAxis: { type: 'value' },
    series: [{
        name: '观测次数',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(10,110,150,0.18)' },
        itemStyle: { color: '#0a6e96' },
        data: (data.value?.monthlyStats || []).map((i) => i.value)
    }]
}))

const observerOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 78, right: 24, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: (data.value?.observerStats || []).map((i) => i.name).reverse() },
    series: [{
        name: '观测次数',
        type: 'bar',
        itemStyle: { color: '#0e9c9c', borderRadius: [0, 4, 4, 0] },
        barMaxWidth: 20,
        data: (data.value?.observerStats || []).map((i) => i.value).reverse()
    }]
}))
</script>
