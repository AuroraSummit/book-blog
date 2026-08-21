<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function submit() {
  if (!username.value.trim() || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await api.login({ username: username.value.trim(), password: password.value })
    auth.setAuth(data.token, data.username)
    router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/admin')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-body">
    <div class="login-wrap">
      <div class="login-box">
        <h1>纸页之间</h1>
        <p class="sub">管理后台 · 仅博主可入</p>

        <form novalidate @submit.prevent="submit">
          <div class="form-item">
            <label for="username">用户名</label>
            <input id="username" v-model="username" type="text" autocomplete="username" required>
          </div>
          <div class="form-item">
            <label for="password">密码</label>
            <input id="password" v-model="password" type="password" autocomplete="current-password" required>
          </div>
          <button class="btn btn-primary" type="submit" :disabled="loading">
            {{ loading ? '登录中…' : '进入后台' }}
          </button>
          <p class="login-error">{{ errorMsg }}</p>
        </form>
      </div>
    </div>
  </div>
</template>
