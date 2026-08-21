<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const tags = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    tags.value = await api.adminTags()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function rename(t) {
  const newName = window.prompt(`将标签「${t.name}」重命名为（若已存在则合并）：`, t.name)
  if (!newName || newName.trim() === t.name) return
  try {
    await api.renameTag(t.name, newName.trim())
    load()
  } catch (e) {
    alert(e.message)
  }
}

async function remove(t) {
  if (!window.confirm(`确定删除标签「${t.name}」？会从所有文章（共 ${t.count} 篇）中移除。`)) return
  try {
    await api.deleteTag(t.name)
    load()
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="view-title">标签管理</h2>
    <p class="form-hint" style="margin-bottom:20px;">标签来自文章；可重命名（自动合并同名标签）或删除（从所有文章移除）。</p>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在加载…</p>
    <div v-else>
      <div v-if="tags.length" class="article-row" v-for="t in tags" :key="t.name">
        <span class="row-title">{{ t.name }}</span>
        <span class="row-meta">{{ t.count }} 篇</span>
        <span class="row-actions">
          <button type="button" @click="rename(t)">重命名</button>
          <button class="danger" type="button" @click="remove(t)">删除</button>
        </span>
      </div>
      <p v-else class="empty-state">还没有标签。</p>
    </div>
  </div>
</template>
