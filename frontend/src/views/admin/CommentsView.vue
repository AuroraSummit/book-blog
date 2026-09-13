<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../../api'
import PaginationBar from '../../components/PaginationBar.vue'

const statusFilters = [
  { value: 'all', label: '全部' },
  { value: 'approved', label: '已通过' },
  { value: 'pending', label: '待审核' },
  { value: 'rejected', label: '已驳回' }
]
const statusLabel = { approved: '已通过', pending: '待审核', rejected: '已驳回' }

const activeStatus = ref('all')
const keyword = ref('')
const page = ref(1)
const size = 20
const totalPages = ref(1)
const list = ref([])
const counts = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })
const loading = ref(true)
const error = ref('')
const replyFor = ref(null)
const replyText = ref('')
const replying = ref(false)

const pendingTip = computed(() =>
  counts.value.pending > 0 ? `待审核 ${counts.value.pending}` : ''
)

function load() {
  loading.value = true
  error.value = ''
  api.adminComments({
    status: activeStatus.value === 'all' ? undefined : activeStatus.value,
    keyword: keyword.value.trim() || undefined,
    page: page.value,
    size
  })
    .then((data) => {
      list.value = data.list
      totalPages.value = data.totalPages
    })
    .catch((e) => { error.value = e.message })
    .finally(() => { loading.value = false })
}

function loadCounts() {
  api.adminCommentCounts().then((c) => { counts.value = c }).catch(() => {})
}

function setStatusFilter(v) { activeStatus.value = v; page.value = 1; load() }
function search() { page.value = 1; load() }
function goPage(p) { page.value = p; load() }

function toggleReply(c) {
  replyFor.value = replyFor.value && replyFor.value.id === c.id ? null : c
  replyText.value = ''
}

async function submitReply() {
  const t = replyText.value.trim()
  if (!t || replying.value || !replyFor.value) return
  replying.value = true
  try {
    await api.adminReply({ parentId: replyFor.value.id, content: t })
    toggleReply(replyFor.value)
    load(); loadCounts()
  } catch (e) {
    alert(e.message)
  } finally {
    replying.value = false
  }
}

async function setStatus(c, status) {
  try {
    await api.setCommentStatus(c.id, status)
    load(); loadCounts()
  } catch (e) {
    alert(e.message)
  }
}

async function remove(c) {
  if (!window.confirm(`确定删除 ${c.author} 的这条评论？此操作不可撤销。`)) return
  try {
    await api.deleteComment(c.id)
    load(); loadCounts()
  } catch (e) {
    alert(e.message)
  }
}

onMounted(() => { load(); loadCounts() })
</script>

<template>
  <div>
    <div class="list-head">
      <h2 class="view-title" style="border-bottom:none;margin-bottom:0;">
        评论管理
        <span v-if="pendingTip" class="pending-badge" role="status">{{ pendingTip }}</span>
      </h2>
      <div class="comment-toolbar">
        <div class="filters">
          <button
            v-for="f in statusFilters"
            :key="f.value"
            :class="{ active: activeStatus === f.value }"
            type="button"
            @click="setStatusFilter(f.value)"
          >{{ f.label }}</button>
        </div>
        <div class="keyword-box">
          <input v-model="keyword" type="search" placeholder="搜作者或内容…" aria-label="搜索评论" @keyup.enter="search">
          <button class="btn" type="button" @click="search">搜索</button>
        </div>
      </div>
    </div>

    <p v-if="error" class="load-error">{{ error }}</p>
    <p v-else-if="loading" class="list-empty">正在加载评论…</p>
    <div v-else>
      <div v-if="list.length">
        <div class="c-row" v-for="c in list" :key="c.id">
          <div class="c-main">
            <div class="c-head">
              <span class="c-author">
                {{ c.author }}<span v-if="c.isAuthor" class="badge-author">博主</span>
              </span>
              <span v-if="c.parentId" class="c-replyto">回复 @{{ c.parentAuthor || '未知' }}</span>
              <span class="c-date">{{ c.date }}</span>
              <span class="c-status" :class="'st-' + c.status">{{ statusLabel[c.status] }}</span>
            </div>
            <p class="c-content">{{ c.content }}</p>
            <div class="c-meta">
              <router-link :to="`/article/${encodeURIComponent(c.articleSlug)}`" target="_blank">
                《{{ c.articleTitle }}》
              </router-link>
              <span v-if="c.email" class="c-email">
                <a :href="`mailto:${c.email}`">{{ c.email }}</a>
              </span>
            </div>
          </div>
          <div class="c-actions">
            <button v-if="c.status !== 'approved'" class="ok" type="button" @click="setStatus(c, 'approved')">通过</button>
            <button v-if="c.status !== 'rejected'" class="warn" type="button" @click="setStatus(c, 'rejected')">驳回</button>
            <button type="button" @click="toggleReply(c)">{{ replyFor?.id === c.id ? '取消' : '回复' }}</button>
            <button class="danger" type="button" @click="remove(c)">删除</button>
          </div>

          <div v-if="replyFor?.id === c.id" class="c-replybox">
            <textarea v-model="replyText" rows="2" maxlength="500" placeholder="博主回复内容…" aria-label="回复内容"></textarea>
            <button class="btn" type="button" :disabled="replying" @click="submitReply">发布回复</button>
          </div>
        </div>

        <PaginationBar :page="page" :total-pages="totalPages" @change="goPage" />
      </div>
      <p v-else class="empty-state">没有符合条件的评论。</p>
    </div>
  </div>
</template>

<style scoped>
.comment-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.pending-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 8px;
  border-radius: 10px;
  background: #d9534f;
  color: #fff;
  font-size: 12px;
  vertical-align: 2px;
}
.keyword-box {
  display: flex;
  gap: 6px;
}
.keyword-box input {
  padding: 6px 10px;
  border: 1px solid #d8ccb6;
  border-radius: 6px;
  background: #fff;
}
.c-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px dashed #ddd2bd;
}
.c-main {
  flex: 1;
  min-width: 260px;
}
.c-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.c-author {
  font-weight: 600;
}
.badge-author {
  display: inline-block;
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 4px;
  background: #8a6a3d;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
}
.c-replyto {
  color: #8a6a3d;
  font-size: 12px;
}
.c-date {
  color: #9a9083;
  font-size: 12px;
}
.c-status {
  font-size: 12px;
  padding: 0 6px;
  border-radius: 4px;
}
.st-approved { background: #e8f0e8; color: #3a7d44; }
.st-pending  { background: #fdf3dc; color: #a8781a; }
.st-rejected { background: #f6e4e2; color: #b0483f; }
.c-content {
  margin: 6px 0 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.c-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #8a6a3d;
}
.c-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.c-actions button {
  border: 1px solid #d8ccb6;
  background: #fff;
  color: #555;
  padding: 3px 10px;
  border-radius: 5px;
  cursor: pointer;
}
.c-actions button.ok { border-color: #3a7d44; color: #3a7d44; }
.c-actions button.warn { border-color: #a8781a; color: #a8781a; }
.c-actions button.danger { border-color: #b0483f; color: #b0483f; }
.c-replybox {
  flex-basis: 100%;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.c-replybox textarea {
  flex: 1;
  min-width: 200px;
  padding: 8px 10px;
  border: 1px solid #d8ccb6;
  border-radius: 6px;
  resize: vertical;
}
</style>
