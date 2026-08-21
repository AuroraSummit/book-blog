<script setup>
import { formatDate, highlight } from '../utils/format'

// article: 文章条目；q: 搜索关键词（可选，命中部分高亮）
// highlight 内部先转义再包 <mark>，v-html 安全
defineProps({
  article: { type: Object, required: true },
  q: { type: String, default: '' }
})
</script>

<template>
  <div class="article-item">
    <router-link class="article-item-main" :to="`/article/${article.slug}`" :aria-label="`阅读全文：${article.title}`">
      <div class="item-meta">
        <span class="item-date">{{ formatDate(article.date) }}</span>
        <span class="tag" :class="article.type">{{ article.category }}</span>
        <span class="item-stats">阅读 {{ article.views || 0 }} · 评论 {{ article.commentCount || 0 }} · 喜欢 {{ article.likeCount || 0 }}</span>
      </div>
      <h3 class="item-title" v-html="highlight(article.title, q)"></h3>
      <p class="item-summary" v-html="highlight(article.summary, q)"></p>
    </router-link>
    <div v-if="article.tags && article.tags.length" class="item-tags">
      <router-link v-for="t in article.tags" :key="t" :to="`/tag/${encodeURIComponent(t)}`">{{ t }}</router-link>
    </div>
  </div>
</template>
