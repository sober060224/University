<template>
    <div ref="el" class="chart"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

// 图表容器：只负责 echarts 实例的生命周期，业务视图只管传 option
const props = defineProps({
    option: { type: Object, required: true }
})

const el = ref(null)
let chart = null

function render() {
    if (chart) {
        chart.setOption(props.option, true)
    }
}

function resize() {
    if (chart) {
        chart.resize()
    }
}

onMounted(() => {
    chart = echarts.init(el.value)
    render()
    window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    if (chart) {
        chart.dispose()
        chart = null
    }
})
</script>
