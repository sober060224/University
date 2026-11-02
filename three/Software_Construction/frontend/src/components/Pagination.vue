<template>
    <div v-if="total > 0" class="pagination">
        <span class="pagination__info">共 {{ total }} 条记录，第 {{ page }} / {{ totalPages }} 页</span>
        <button class="btn btn--sm btn--ghost" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
        <button class="btn btn--sm btn--ghost" :disabled="page >= totalPages" @click="go(page + 1)">下一页</button>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    page: { type: Number, required: true },
    size: { type: Number, default: 10 },
    total: { type: Number, required: true }
})

const emit = defineEmits(['change'])

// size 为 0/负数时 Math.ceil 会得到 Infinity/NaN，Math.max(1, NaN) 仍是 NaN，
// 模板会渲染「第 X / NaN 页」且下一页按钮永远可点
const totalPages = computed(() => {
    const step = Number(props.size) > 0 ? props.size : 10
    const pages = Math.ceil((Number(props.total) || 0) / step)
    return Number.isFinite(pages) ? Math.max(1, pages) : 1
})

function go(target) {
    if (target >= 1 && target <= totalPages.value) {
        emit('change', target)
    }
}
</script>
