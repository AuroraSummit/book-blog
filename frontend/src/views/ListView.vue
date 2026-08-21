<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import ArticleItem from '../components/ArticleItem.vue'
import PaginationBar from '../components/PaginationBar.vue'

// 通用列表页：按分类（/books /movies，meta.filter）或按标签（/tag/:tag）浏览
const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 5
const isTag = computed(() => !!route.params.tag)
const filter = route.meta.filter // 'reading' | 'movie' | undefined
const heading = computed(() => (isTag.value ? `标签 · ${route.params.tag}` : route.meta.heading || '文章'))

const list = ref([])
const total = ref(0)
const totalPages = ref(1)
const page = ref(1)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: PAGE_SIZE }
    if (isTag.value) params.tag = route.params.tag
    else if (filter) params.type = filter
    const data = await api.articles(params)
    list.value = data.list
    total.value = data.total
    totalPages.value = Math.max(1, data.totalPages)
    if (page.value > totalPages.value) page.value = totalPages.value
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function goTo(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  const query = p > 1 ? { page: String(p) } : {}
  router.replace({ query })
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function init() {
  const p = parseInt(route.query.page, 10)
  page.value = !isNaN(p) && p > 0 ? p : 1
  load()
}

onMounted(init)
watch(() => route.query, init)
watch(() => route.params.tag, () => {
  page.value = 1
  router.replace({ query: {} })
  load()
})
</script>

<template>
  <div>
    <div class="list-head">
      <h2 class="list-title">{{ heading }}</h2>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在翻页…</p>
    <template v-else>
      <section v-if="list.length" aria-live="polite">
        <ArticleItem v-for="a in list" :key="a.slug" :article="a" />
      </section>
      <p v-else class="list-empty">还没有相关文章。</p>
      <PaginationBar :page="page" :total-pages="totalPages" @change="goTo" />
      <p class="list-foot">共 {{ total }} 篇</p>
    </template>
  </div>
</template>
