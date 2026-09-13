<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-body">
    <header class="admin-top">
      <span class="brand">纸页之间 · 后台</span>
      <span class="top-links">
        <router-link to="/" target="_blank">回到前台</router-link>
        <a href="#" @click.prevent="logout">退出</a>
      </span>
    </header>

    <div class="admin-wrap">
      <nav class="admin-side" aria-label="后台导航">
        <router-link to="/admin" exact-active-class="router-link-exact-active">仪表盘</router-link>
        <router-link to="/admin/articles">文章管理</router-link>
        <router-link to="/admin/comments">评论管理</router-link>
        <router-link to="/admin/write">写文章</router-link>
        <router-link to="/admin/tags">标签管理</router-link>
        <router-link to="/admin/settings">站点设置</router-link>
      </nav>
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>
