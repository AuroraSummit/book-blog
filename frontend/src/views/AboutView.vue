<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const paragraphs = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const data = await api.about()
    paragraphs.value = data.bio || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <h2 class="about-page-title">关于这里</h2>
    <p v-if="loading" class="list-empty">正在翻页…</p>
    <p v-else-if="error" class="load-error">{{ error }}</p>
    <div v-else class="about-bio">
      <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
    </div>
  </div>
</template>
