<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const tags = ref([])
const loading = ref(true)
const error = ref('')

// 标签云字号：按文章数比例放大
function fontSize(t) {
  const max = Math.max(...tags.value.map((x) => x.count), 1)
  return 14 + Math.round((t.count / max) * 10) + 'px'
}

onMounted(async () => {
  try {
    tags.value = await api.tags()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="list-head">
      <h2 class="list-title">标签</h2>
      <p class="list-foot" style="margin:0;">共 {{ tags.length }} 个标签</p>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在翻页…</p>
    <p v-else-if="!tags.length" class="list-empty">还没有标签。</p>
    <div v-else class="tag-cloud">
      <router-link
        v-for="t in tags"
        :key="t.name"
        :to="`/tag/${encodeURIComponent(t.name)}`"
        :style="{ fontSize: fontSize(t) }"
      >{{ t.name }}<span class="tag-count">{{ t.count }}</span></router-link>
    </div>
  </div>
</template>
