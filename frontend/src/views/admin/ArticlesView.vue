<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const filters = [
  { value: 'all', label: '全部' },
  { value: 'published', label: '已发布' },
  { value: 'draft', label: '草稿' }
]
const activeFilter = ref('all')
const list = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await api.adminArticles(activeFilter.value === 'all' ? undefined : activeFilter.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function setFilter(v) {
  activeFilter.value = v
  load()
}

async function publish(a) {
  try {
    await api.setArticleStatus(a.slug, 'published')
    load()
  } catch (e) {
    alert(e.message)
  }
}

async function remove(a) {
  if (!window.confirm(`确定删除《${a.title}》？此操作不可撤销。`)) return
  try {
    await api.deleteArticle(a.slug)
    load()
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="list-head">
      <h2 class="view-title" style="border-bottom:none;margin-bottom:0;">文章管理</h2>
      <div style="display:flex;align-items:center;gap:16px;">
        <div class="filters">
          <button
            v-for="f in filters"
            :key="f.value"
            :class="{ active: activeFilter === f.value }"
            type="button"
            @click="setFilter(f.value)"
          >{{ f.label }}</button>
        </div>
        <router-link class="btn" to="/admin/write">＋ 写文章</router-link>
      </div>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在翻页…</p>
    <div v-else>
      <div v-if="list.length">
        <div class="article-row" v-for="a in list" :key="a.slug">
          <span class="row-title">{{ a.title }}</span>
          <span class="row-meta">{{ a.date }} · {{ a.category }}<template v-if="a.status === 'draft'"> · 草稿</template> · 阅读 {{ a.views }}</span>
          <span class="row-actions">
            <button v-if="a.status === 'draft'" class="ok" type="button" @click="publish(a)">发布</button>
            <router-link :to="{ name: 'admin-write', query: { slug: a.slug } }">编辑</router-link>
            <button class="danger" type="button" @click="remove(a)">删除</button>
          </span>
        </div>
      </div>
      <p v-else class="empty-state">{{ activeFilter === 'draft' ? '还没有草稿。' : '还没有文章。' }}</p>
    </div>
  </div>
</template>
