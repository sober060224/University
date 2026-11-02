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

// 浅监听就够：所有用 ChartBox 的视图都把 option 写在 computed 里，
// 任何一次数据变化都会是一个新对象。deep 监听会把整棵 option 树（含几百个点
// 的 series data）逐层比对，而 setOption(..., true) 本来就会整体替换，白花开销。
watch(() => props.option, render)

onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    if (chart) {
        chart.dispose()
        chart = null
    }
})
</script>
