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

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

function go(target) {
    if (target >= 1 && target <= totalPages.value) {
        emit('change', target)
    }
}
</script>
