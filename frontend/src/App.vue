<script setup>
// 全局：阅读进度条 + 回到顶部 + 页面骨架 + 站点信息/主题加载
import { onMounted } from 'vue'
import ReadingProgress from './components/ReadingProgress.vue'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useSiteStore } from './stores/site'
import { applyTheme } from './utils/theme'

const route = useRoute()
const site = useSiteStore()
// 后台页面使用独立的紧凑布局（不渲染前台页眉页脚）
const isAdmin = computed(() => route.path.startsWith('/admin'))

onMounted(() => {
  applyTheme()
  site.fetch()
})
</script>

<template>
  <ReadingProgress />
  <template v-if="isAdmin">
    <router-view />
  </template>
  <template v-else>
    <div class="page">
      <SiteHeader />
      <main>
        <router-view />
      </main>
    </div>
    <SiteFooter />
  </template>
</template>
