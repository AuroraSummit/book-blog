<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import ArticleItem from '../components/ArticleItem.vue'
import PaginationBar from '../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 5
const filters = [
  { value: 'all', label: '全部' },
  { value: 'reading', label: '读书笔记' },
  { value: 'movie', label: '观影感受' }
]

const activeFilter = ref('all')
const list = ref([])
const total = ref(0)
const totalPages = ref(1)
const page = ref(1)
const loading = ref(false)
const error = ref('')
const shuffling = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.articles({
      type: activeFilter.value === 'all' ? undefined : activeFilter.value,
      page: page.value,
      size: PAGE_SIZE
    })
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

// 筛选切换 → 回到第 1 页并同步 URL
function setFilter(value) {
  if (activeFilter.value === value) return
  activeFilter.value = value
  page.value = 1
  syncUrl()
  load()
}

function goTo(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  syncUrl()
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function syncUrl() {
  const query = {}
  if (activeFilter.value !== 'all') query.type = activeFilter.value
  if (page.value > 1) query.page = String(page.value)
  router.replace({ query })
}

// 随便翻翻：随机跳一篇文章
async function shuffle() {
  if (shuffling.value) return
  shuffling.value = true
  try {
    const r = await api.randomArticle()
    if (r.slug) router.push(`/article/${r.slug}`)
  } catch (e) {
    error.value = e.message
  } finally {
    shuffling.value = false
  }
}

// 从 URL 恢复筛选与页码（刷新后保持）
onMounted(() => {
  const q = route.query
  if (q.type === 'reading' || q.type === 'movie') activeFilter.value = q.type
  const p = parseInt(q.page, 10)
  if (!isNaN(p) && p > 0) page.value = p
  load()
})

// 浏览器前进/后退时跟随 URL
watch(() => route.query, () => {
  const q = route.query
  if (q.type === 'reading' || q.type === 'movie') activeFilter.value = q.type
  else if (activeFilter.value !== 'all') activeFilter.value = 'all'
  const p = parseInt(q.page, 10)
  page.value = !isNaN(p) && p > 0 ? p : 1
  load()
})
</script>

<template>
  <div>
    <div class="list-head">
      <h2 class="list-title">最近的文章</h2>
      <div style="display:flex;align-items:center;gap:18px;">
        <div class="filters" role="group" aria-label="按分类筛选文章">
          <button
            v-for="f in filters"
            :key="f.value"
            :class="{ active: activeFilter === f.value }"
            type="button"
            @click="setFilter(f.value)"
          >{{ f.label }}</button>
        </div>
        <button class="shuffle-btn" type="button" :disabled="shuffling" @click="shuffle">随便翻翻 ↺</button>
      </div>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在翻页…</p>
    <template v-else>
      <section v-if="list.length" aria-live="polite">
        <ArticleItem v-for="a in list" :key="a.slug" :article="a" />
      </section>
      <p v-else class="list-empty">这个分类下还没有文章。</p>
      <PaginationBar :page="page" :total-pages="totalPages" @change="goTo" />
      <p class="list-foot">共 {{ total }} 篇</p>
    </template>
  </div>
</template>
