<script setup>
defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true }
})
const emit = defineEmits(['change'])
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="分页">
    <button class="p-btn" type="button" :disabled="page <= 1" @click="emit('change', page - 1)">← 上一页</button>
    <span class="p-info">第 {{ page }} 页 · 共 {{ totalPages }} 页</span>
    <button class="p-btn" type="button" :disabled="page >= totalPages" @click="emit('change', page + 1)">下一页 →</button>
  </nav>
</template>
