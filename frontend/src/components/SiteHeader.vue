<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteStore } from '../stores/site'
import { toggleTheme, applyTheme } from '../utils/theme'

const router = useRouter()
const site = useSiteStore()
const keyword = ref('')
const theme = ref(applyTheme())

function submit() {
  const q = keyword.value.trim()
  router.push({ name: 'search', query: q ? { q } : {} })
}

function switchTheme() {
  theme.value = toggleTheme()
}
</script>

<template>
  <header class="site-head">
    <h1 class="site-name">
      <router-link to="/">{{ site.name }}</router-link>
    </h1>
    <p class="site-sub">{{ site.subtitle }}</p>

    <!-- 公告栏：后台「站点设置」可编辑 -->
    <p v-if="site.announcement" class="site-announce">{{ site.announcement }}</p>

    <div class="nav-row">
      <nav class="site-nav" aria-label="站点导航">
        <router-link to="/">文章</router-link>
        <router-link to="/books">读书</router-link>
        <router-link to="/movies">影评</router-link>
        <router-link to="/tags">标签</router-link>
        <router-link to="/about">关于</router-link>
      </nav>
      <div class="site-search">
        <form role="search" @submit.prevent="submit">
          <input
            v-model="keyword"
            type="search"
            name="q"
            placeholder="搜文章 / 影评 / 标签"
            aria-label="站内搜索"
          >
        </form>
        <button class="theme-toggle" type="button" :aria-label="theme === 'night' ? '切换到纸色模式' : '切换到夜间模式'" @click="switchTheme">
          {{ theme === 'night' ? '☀' : '☾' }}
        </button>
      </div>
    </div>
  </header>
</template>
