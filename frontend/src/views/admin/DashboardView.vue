<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { api } from '../../api'

const stats = ref(null)
const error = ref('')
let charts = []

const AC = '#8a6a3d'
const GRID = '#e0d7c5'
const LABEL = '#9a9083'

// 按需动态加载 ECharts（生产包不包含图表，仅后台用）
async function renderCharts() {
  const echarts = await import('echarts')
  if (!stats.value) return

  const v = stats.value.visitors || []
  const vc = echarts.init(document.getElementById('chart-visitors'))
  vc.setOption({
    color: [AC],
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: 26, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: false, data: v.map((d) => d.date),
      axisLine: { lineStyle: { color: GRID } }, axisLabel: { color: LABEL, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: GRID } }, axisLabel: { color: LABEL, fontSize: 11 } },
    series: [{ name: '访客', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: v.map((d) => d.count) }]
  })
  charts.push(vc)

  const p = stats.value.published || []
  const pc = echarts.init(document.getElementById('chart-publish'))
  pc.setOption({
    color: [AC],
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: 26, bottom: 30 },
    xAxis: { type: 'category', data: p.map((d) => d.month),
      axisLine: { lineStyle: { color: GRID } }, axisLabel: { color: LABEL, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: GRID } }, axisLabel: { color: LABEL, fontSize: 11 } },
    series: [{ name: '发表', type: 'bar', barWidth: 22,
      itemStyle: { color: AC, borderRadius: [2, 2, 0, 0] }, data: p.map((d) => d.count) }]
  })
  charts.push(pc)

  // 标签分布（饼图，限制为前 7 + 其他，避免扇区过多）
  const tags = stats.value.tagDistribution || []
  if (tags.length) {
    const tc = echarts.init(document.getElementById('chart-tags'))
    tc.setOption({
      color: ['#8a6a3d', '#a08a5c', '#6d7f6d', '#9b7e6a', '#7a6a8a', '#b09a7a', '#5d7f8a', '#9a9857'],
      tooltip: { trigger: 'item', formatter: '{b}：{c} 篇（{d}%）' },
      legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { color: LABEL, fontSize: 11 } },
      series: [{
        type: 'pie', radius: ['40%', '64%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 1 },
        label: { color: LABEL, fontSize: 11, formatter: '{b} {c}' },
        labelLine: { length: 8, length2: 6 },
        data: tags.map((t) => ({ name: t.name, value: t.count }))
      }]
    })
    charts.push(tc)
  }
}

function onResize() {
  charts.forEach((c) => c && c.resize())
}

onMounted(async () => {
  try {
    stats.value = await api.adminStats()
    await renderCharts()
    window.addEventListener('resize', onResize)
  } catch (e) {
    error.value = e.message
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c && c.dispose())
  charts = []
})
</script>

<template>
  <div>
    <h2 class="view-title">仪表盘</h2>
    <p v-if="error" class="load-error">{{ error }}</p>
    <template v-else-if="stats">
      <div class="stat-grid">
        <div class="stat-card"><div class="num">{{ stats.totalArticles }}</div><div class="label">全部文章</div></div>
        <div class="stat-card"><div class="num">{{ stats.totalBooks }}</div><div class="label">读书笔记</div></div>
        <div class="stat-card"><div class="num">{{ stats.totalMovies }}</div><div class="label">观影感受</div></div>
        <div class="stat-card"><div class="num">{{ stats.totalDrafts }}</div><div class="label">草稿</div></div>
        <div class="stat-card"><div class="num">{{ stats.totalComments }}</div><div class="label">全部评论</div></div>
        <div class="stat-card"><div class="num">{{ stats.totalLikes }}</div><div class="label">全部点赞</div></div>
      </div>

      <div class="charts-grid">
        <div class="chart-box">
          <p class="chart-title">访客趋势 · 近 14 天</p>
          <div id="chart-visitors" style="height:240px;"></div>
        </div>
        <div class="chart-box">
          <p class="chart-title">文章发表数量 · 按月</p>
          <div id="chart-publish" style="height:240px;"></div>
        </div>
        <div class="chart-box">
          <p class="chart-title">标签分布</p>
          <div id="chart-tags" style="height:240px;"></div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:26px;margin-bottom:38px;">
        <div>
          <p class="dash-section-title">最受欢迎（阅读量 TOP 5）</p>
          <template v-if="stats.mostViewed.length">
            <div class="article-row" v-for="a in stats.mostViewed" :key="'v' + a.slug">
              <router-link class="row-title" :to="`/article/${a.slug}`" target="_blank" rel="noopener">{{ a.title }}</router-link>
              <span class="row-meta">{{ a.views }} 阅读</span>
            </div>
          </template>
          <p v-else class="empty-state" style="padding:20px 0;">暂无数据</p>
        </div>
        <div>
          <p class="dash-section-title">点赞榜 TOP 5</p>
          <template v-if="stats.mostLiked.length">
            <div class="article-row" v-for="a in stats.mostLiked" :key="'l' + a.slug">
              <router-link class="row-title" :to="`/article/${a.slug}`" target="_blank" rel="noopener">{{ a.title }}</router-link>
              <span class="row-meta">♥ {{ a.likes }}</span>
            </div>
          </template>
          <p v-else class="empty-state" style="padding:20px 0;">还没有人点赞，去前台点一个吧</p>
        </div>
      </div>

      <p class="dash-section-title">最近的文章</p>
      <template v-if="stats.recentArticles && stats.recentArticles.length">
        <div class="article-row" v-for="a in stats.recentArticles" :key="a.slug">
          <span class="row-title">{{ a.title }}</span>
          <span class="row-meta">{{ a.date }} · {{ a.category }}<template v-if="a.status === 'draft'"> · 草稿</template></span>
          <span class="row-actions">
            <router-link :to="`/article/${a.slug}`" target="_blank" rel="noopener">查看</router-link>
          </span>
        </div>
      </template>
    </template>
    <p v-else class="list-empty">加载中…</p>
  </div>
</template>
