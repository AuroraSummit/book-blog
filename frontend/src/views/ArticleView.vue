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
const content = ref('')
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

function jumpTo(index) {
  const el = document.getElementById('sec-' + index)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

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
      content: text
    })
    content.value = ''
    submitMsg.value = '评论已发布。'
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

      <!-- 评论区：即时发布展示 -->
      <section class="comments">
        <h2 class="comments-title">评论（{{ comments.length }}）</h2>
        <div v-if="comments.length" class="comments-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-author">{{ c.author }}</span>
              <span class="comment-date">{{ formatDate(c.date) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
          </div>
        </div>
        <p v-else class="comments-empty">还没有评论，来写下第一条吧。</p>

        <form class="comment-form" novalidate @submit.prevent="submitComment">
          <input v-model="author" type="text" maxlength="20" placeholder="昵称（可不填，默认匿名读者）" aria-label="昵称">
          <textarea v-model="content" rows="3" maxlength="500" placeholder="写下你的想法…" aria-label="评论内容" required></textarea>
          <button class="btn" type="submit" :disabled="submitting">发表评论</button>
          <p class="comment-hint">{{ submitMsg }}</p>
        </form>
      </section>
    </article>
  </div>
</template>
