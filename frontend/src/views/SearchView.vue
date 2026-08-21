<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import ArticleItem from '../components/ArticleItem.vue'

const route = useRoute()
const list = ref([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')

function load() {
  keyword.value = (route.query.q || '').trim()
  document.title = keyword.value ? `${keyword.value} · 搜索 · 纸页之间` : '搜索 · 纸页之间'
  if (!keyword.value) {
    list.value = []
    return
  }
  loading.value = true
  error.value = ''
  api.searchArticles(keyword.value)
    .then((data) => { list.value = data })
    .catch((e) => { error.value = e.message })
    .finally(() => { loading.value = false })
}

onMounted(load)
watch(() => route.query.q, load)
</script>

<template>
  <div>
    <div class="search-head">
      <p class="search-keyword">搜索：<span>「{{ keyword || '空' }}」</span></p>
      <p class="search-count">{{ keyword ? list.length + ' 篇' : '0 篇' }}</p>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在翻找…</p>
    <template v-else>
      <section v-if="list.length" aria-live="polite">
        <ArticleItem v-for="a in list" :key="a.slug" :article="a" :q="keyword" />
      </section>
      <p v-else-if="keyword" class="list-empty">没有找到相关的文章。</p>
      <p v-else class="list-empty">请输入关键词搜索。</p>
    </template>
  </div>
</template>
