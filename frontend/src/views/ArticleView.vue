<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import ArticleItem from '../components/ArticleItem.vue'
import { formatDate, parseContent, isViewed, markViewed, readingTime } from '../utils/format'

const route = useRoute()
const article = ref(null)
const blocks = ref([])
const toc = computed(() => blocks.value.filter((b) => b.type === 'h2'))
const minutes = computed(() => readingTime(blocks.value))
const all = ref([])          // 用于上一篇/下一篇
const related = ref([])
const comments = ref([])
const author = ref('')
const email = ref('')        // 选填：有人回复你的评论时通知
const website = ref('')      // honeypot 隐藏字段，机器人会填
const content = ref('')
const replyTo = ref(null)    // 正在回复的评论
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const submitMsg = ref('')
const liked = ref(false)
const likeCount = ref(0)
const liking = ref(false)

const idx = computed(() => all.value.findIndex((a) => a.slug === article.value?.slug))
const prev = computed(() => idx.value >= 0 ? all.value[idx.value + 1] : null) // 更早
const next = computed(() => idx.value >= 0 ? all.value[idx.value - 1] : null) // 更新

// 按 parentId 组一层楼：顶层评论 + 其下回复（replies）
const commentGroups = computed(() => {
  const roots = []
  const map = new Map()
  for (const c of comments.value) {
    const n = { ...c, replies: [] }
    map.set(n.id, n)
    if (n.parentId == null) roots.push(n)
  }
  for (const c of comments.value) {
    const n = map.get(c.id)
    const p = c.parentId != null ? map.get(c.parentId) : null
    if (p) p.replies.push(n)
    else if (c.parentId != null) roots.push(n) // 防孤儿条目
  }
  return roots
})

function jumpTo(index) {
  const el = document.getElementById('sec-' + index)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function startReply(c) { replyTo.value = c }
function cancelReply() { replyTo.value = null }

async function loadComments() {
  try {
    comments.value = await api.comments(route.params.slug)
  } catch {
    comments.value = []
  }
}

async function loadLikes() {
  try {
    const s = await api.likedStatus(route.params.slug)
    liked.value = s.liked
    likeCount.value = s.likeCount
  } catch {
    likeCount.value = article.value?.likeCount || 0
  }
}

async function like() {
  if (liked.value || liking.value) return
  liking.value = true
  try {
    const s = await api.like(route.params.slug)
    liked.value = s.liked
    likeCount.value = s.likeCount
  } catch (e) {
    submitMsg.value = e.message
  } finally {
    liking.value = false
  }
}

async function submitComment() {
  const text = content.value.trim()
  if (!text || submitting.value) return
  submitting.value = true
  submitMsg.value = ''
  try {
    await api.postComment(route.params.slug, {
      author: author.value.trim(),
      content: text,
      email: email.value.trim() || '',
      website: website.value,
      parentId: replyTo.value ? replyTo.value.id : null
    })
    content.value = ''
    if (replyTo.value) {
      submitMsg.value = '回复已发布。'
      replyTo.value = null
    } else {
      submitMsg.value = '评论已发布。'
    }
    await loadComments()
  } catch (e) {
    submitMsg.value = e.message
  } finally {
    submitting.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.article(route.params.slug)
    article.value = data
    blocks.value = parseContent(data.content)
    document.title = `${data.title} · 纸页之间`

    // 阅读计数：同一浏览器会话只记一次（避免刷新重复 +1）
    if (!isViewed(data.slug)) {
      api.recordView(data.slug).catch(() => {})
      markViewed(data.slug)
    }

    // 预取全部已发布文章用于上下篇导航 + 相关推荐
    const page = await api.articles({ page: 1, size: 100 })
    all.value = page.list
    api.relatedArticles(data.slug, 3)
      .then((r) => { related.value = r })
      .catch(() => { related.value = [] })
    await Promise.all([loadComments(), loadLikes()])
  } catch (e) {
    error.value = e.message === '文章不存在' ? '没有找到这篇文章，它可能已被删除。' : e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <p class="back-link"><router-link to="/">← 返回</router-link></p>

    <p v-if="loading" class="list-empty">正在翻页…</p>
    <p v-else-if="error" class="not-found">{{ error }}</p>
    <article v-else-if="article">
      <header class="article-head">
        <div class="article-meta">
          <span class="tag" :class="article.type">{{ article.category }}</span>
          <span>{{ formatDate(article.date) }}</span>
          <span>阅读 {{ article.views }}</span>
          <span>约 {{ minutes }} 分钟读完</span>
          <span>更新于 {{ formatDate(article.updatedAt) }}</span>
        </div>
        <h1>{{ article.title }}</h1>
        <div v-if="article.tags && article.tags.length" class="article-tags">
          <router-link v-for="t in article.tags" :key="t" :to="`/tag/${encodeURIComponent(t)}`">{{ t }}</router-link>
        </div>
      </header>

      <!-- 目录（正文有 ## 小节时显示） -->
      <nav v-if="toc.length" class="toc" aria-label="文章目录">
        <span class="toc-title">目录</span>
        <a v-for="(b, i) in toc" :key="i" href="#" @click.prevent="jumpTo(blocks.indexOf(b))">{{ b.text }}</a>
      </nav>

      <!-- 正文：解析结果逐块渲染，文本一律走 {{ }}，杜绝 XSS -->
      <div class="article-body">
        <template v-for="(b, i) in blocks" :key="i">
          <h2 v-if="b.type === 'h2'" :id="'sec-' + i">{{ b.text }}</h2>
          <blockquote v-else-if="b.type === 'quote'"><p>{{ b.text }}</p></blockquote>
          <p v-else>{{ b.text }}</p>
        </template>
      </div>

      <!-- 点赞 -->
      <div class="like-bar">
        <button class="like-btn" :class="{ liked }" type="button" :disabled="liked || liking" @click="like">
          {{ liked ? '♥ 已喜欢' : '♡ 喜欢' }} <span class="like-num">{{ likeCount }}</span>
        </button>
        <span class="like-hint">{{ liked ? '谢谢你喜欢这篇' : '觉得不错就点一下' }}</span>
      </div>

      <div class="article-end">❦</div>

      <!-- 相关阅读 -->
      <section v-if="related.length" class="related">
        <h2 class="related-title">相关阅读</h2>
        <ArticleItem v-for="r in related" :key="r.slug" :article="r" />
      </section>

      <nav class="article-nav" aria-label="文章导航">
        <router-link v-if="prev" :to="`/article/${prev.slug}`">
          <span class="nav-label">上一篇</span>{{ prev.title }}
        </router-link>
        <span v-else class="nav-void"></span>
        <router-link v-if="next" class="nav-next" :to="`/article/${next.slug}`">
          <span class="nav-label">下一篇</span>{{ next.title }}
        </router-link>
      </nav>

      <!-- 评论区：匿名即时发布 + 一层回复楼 -->
      <section class="comments">
        <h2 class="comments-title">评论（{{ comments.length }}）</h2>
        <div v-if="commentGroups.length" class="comments-list">
          <div v-for="g in commentGroups" :key="g.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-author">
                {{ g.author }}<span v-if="g.isAuthor" class="badge-author" role="img" aria-label="博主">博主</span>
              </span>
              <span class="comment-date">{{ formatDate(g.date) }}</span>
              <button class="comment-reply-btn" type="button" @click="startReply(g)">回复</button>
            </div>
            <p class="comment-content">{{ g.content }}</p>

            <div v-if="g.replies.length" class="comment-replies">
              <div v-for="r in g.replies" :key="r.id" class="comment-item reply">
                <div class="comment-head">
                  <span class="comment-author">
                    {{ r.author }}<span v-if="r.isAuthor" class="badge-author" role="img" aria-label="博主">博主</span>
                  </span>
                  <span class="comment-date">{{ formatDate(r.date) }}</span>
                  <button class="comment-reply-btn" type="button" @click="startReply(r)">回复</button>
                </div>
                <p class="comment-content">{{ r.content }}</p>
              </div>
            </div>
          </div>
        </div>
        <p v-else class="comments-empty">还没有评论，来写下第一条吧。</p>

        <form class="comment-form" novalidate @submit.prevent="submitComment">
          <!-- honeypot：真人看不见也填不到，机器人会填 → 后端拦截 -->
          <input v-model="website" class="hp-field" type="text" name="website" tabindex="-1" autocomplete="off" aria-hidden="true">

          <div v-if="replyTo" class="reply-banner">
            <span>正在回复 <strong>{{ replyTo.author }}</strong></span>
            <button type="button" @click="cancelReply">取消</button>
          </div>

          <div class="comment-form-row">
            <input v-model="author" type="text" maxlength="20" placeholder="昵称（可不填，默认匿名读者）" aria-label="昵称">
            <input v-model="email" type="email" maxlength="100" placeholder="邮箱（选填，有人回复时通知你）" aria-label="邮箱（选填）">
          </div>
          <textarea v-model="content" rows="3" maxlength="500" placeholder="写下你的想法…" aria-label="评论内容" required></textarea>
          <button class="btn" type="submit" :disabled="submitting">{{ replyTo ? '发布回复' : '发表评论' }}</button>
          <p class="comment-hint" role="status">{{ submitMsg }}</p>
        </form>
      </section>
    </article>
  </div>
</template>

<style scoped>
.hp-field {
  position: absolute;
  left: -9999px;
  top: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}
.comment-form-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-form-row input {
  flex: 1;
  min-width: 0;
}
.reply-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  margin-bottom: 8px;
  border: 1px dashed #c8b89c;
  border-radius: 6px;
  background: #faf6ef;
  font-size: 13px;
}
.reply-banner button {
  margin-left: auto;
  border: none;
  background: none;
  color: #8a6a3d;
  cursor: pointer;
}
.comment-replies {
  margin-left: 18px;
  padding-left: 12px;
  border-left: 2px solid #e5dccb;
}
.comment-reply-btn {
  margin-left: auto;
  border: none;
  background: none;
  color: #8a6a3d;
  font-size: 12px;
  cursor: pointer;
}
.comment-reply-btn:hover {
  text-decoration: underline;
}
.badge-author {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 4px;
  background: #8a6a3d;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  vertical-align: 1px;
}
</style>
