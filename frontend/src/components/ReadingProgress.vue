<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

// 阅读进度条 + 回到顶部（原 ui.js 逻辑的 Vue 版）
const progress = ref(0)
const showTop = ref(false)
let ticking = false

function update() {
  const doc = document.documentElement
  const max = doc.scrollHeight - window.innerHeight
  const ratio = max > 0 ? Math.min(Math.max(window.scrollY / max, 0), 1) : 0
  progress.value = ratio
  showTop.value = ratio > 0.88
  ticking = false
}

function onScroll() {
  if (!ticking) {
    ticking = true
    requestAnimationFrame(update)
  }
}

function backToTop() {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  update()
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <div
    class="reading-progress"
    role="progressbar"
    aria-label="阅读进度"
    aria-valuemin="0"
    aria-valuemax="100"
    :aria-valuenow="Math.round(progress * 100)"
    :style="{ transform: `scaleX(${progress})` }"
  ></div>
  <button
    class="back-to-top"
    :class="{ show: showTop }"
    type="button"
    aria-label="回到顶部"
    @click="backToTop"
  >
    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M8 13V3" />
      <path d="M3.5 7.5 8 3l4.5 4.5" />
    </svg>
  </button>
</template>
