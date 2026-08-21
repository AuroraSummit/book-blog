<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api'

const route = useRoute()
const router = useRouter()

const editSlug = ref(null)
const form = ref({
  title: '',
  type: 'reading',
  date: new Date().toISOString().slice(0, 10),
  tags: '',
  summary: '',
  content: '',
  status: 'published'
})
const loading = ref(false)
const saving = ref(false)
const msg = ref('')

async function load() {
  const slug = typeof route.query.slug === 'string' ? route.query.slug : ''
  editSlug.value = slug || null
  if (!slug) return
  loading.value = true
  try {
    const detail = await api.adminArticle(slug)
    form.value = {
      title: detail.title,
      type: detail.type,
      date: String(detail.date),
      tags: (detail.tags || []).join(', '),
      summary: detail.summary,
      content: (detail.content || []).join('\n'),
      status: detail.status
    }
  } catch (e) {
    msg.value = e.message
  } finally {
    loading.value = false
  }
}

function submit(status) {
  const f = form.value
  if (!f.title.trim() || !f.date || !f.summary.trim() || !f.content.trim()) {
    msg.value = '请填写标题、日期、摘要和正文'
    return
  }
  const payload = {
    title: f.title.trim(),
    type: f.type,
    category: f.type === 'movie' ? '观影感受' : '读书笔记',
    date: f.date,
    tags: f.tags.split(/[,，]/).map((s) => s.trim()).filter(Boolean),
    summary: f.summary.trim(),
    content: f.content.split('\n').map((s) => s.trim()).filter(Boolean),
    status
  }
  saving.value = true
  msg.value = ''
  const req = editSlug.value
    ? api.updateArticle(editSlug.value, payload)
    : api.createArticle(payload)
  req
    .then(() => {
      router.push({ name: 'admin-articles' })
    })
    .catch((e) => {
      msg.value = e.message
      saving.value = false
    })
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="view-title">{{ editSlug ? '编辑文章' : '写文章' }}</h2>

    <p v-if="loading" class="list-empty">正在加载…</p>
    <form v-else novalidate @submit.prevent>
      <div class="form-item">
        <label for="f-title">标题</label>
        <input id="f-title" v-model="form.title" type="text" required placeholder="例如：《月亮与六便士》读后…">
      </div>

      <div class="form-row">
        <div class="form-item">
          <label for="f-type">分类</label>
          <select id="f-type" v-model="form.type">
            <option value="reading">读书笔记</option>
            <option value="movie">观影感受</option>
          </select>
        </div>
        <div class="form-item">
          <label for="f-date">日期</label>
          <input id="f-date" v-model="form.date" type="date" required>
        </div>
      </div>

      <div class="form-item">
        <label for="f-tags">标签（用逗号分隔）</label>
        <input id="f-tags" v-model="form.tags" type="text" placeholder="例如：毛姆, 理想主义, 外国文学">
      </div>

      <div class="form-item">
        <label for="f-summary">摘要（首页卡片显示）</label>
        <textarea id="f-summary" v-model="form.summary" rows="2" required></textarea>
      </div>

      <div class="form-item">
        <label for="f-content">正文（每行一段；「## 」开头为小标题，「> 」开头为引用）</label>
        <textarea id="f-content" v-model="form.content" rows="14" required></textarea>
      </div>

      <div>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="submit('published')">
          {{ editSlug ? '保存修改' : '发布文章' }}
        </button>
        <button class="btn btn-ghost" type="button" :disabled="saving" @click="submit('draft')">存为草稿</button>
        <router-link class="btn btn-ghost" to="/admin/articles">取消</router-link>
        <p class="form-hint">{{ msg }}</p>
      </div>
    </form>
  </div>
</template>
