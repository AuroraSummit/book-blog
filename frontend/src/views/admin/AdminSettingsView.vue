<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'

const form = ref({
  name: '',
  subtitle: '',
  announcement: '',
  aboutBio: ''
})
const loading = ref(true)
const saving = ref(false)
const msg = ref('')

function splitLines(text) {
  return String(text || '').split('\n').map((s) => s.trim()).filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const data = await api.adminSettings()
    form.value = {
      name: data.name || '',
      subtitle: data.subtitle || '',
      announcement: data.announcement || '',
      aboutBio: (data.aboutBio || []).join('\n')
    }
  } catch (e) {
    msg.value = e.message
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.value.name.trim()) {
    msg.value = '站点名称不能为空'
    return
  }
  saving.value = true
  msg.value = ''
  try {
    await api.updateSettings({
      name: form.value.name.trim(),
      subtitle: form.value.subtitle.trim(),
      announcement: form.value.announcement.trim(),
      aboutBio: splitLines(form.value.aboutBio)
    })
    msg.value = '已保存，前台刷新可见。'
  } catch (e) {
    msg.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="view-title">站点设置</h2>

    <p v-if="loading" class="list-empty">正在加载…</p>
    <form v-else novalidate @submit.prevent="save">
      <div class="form-item">
        <label for="s-name">站点名称（页眉标题）</label>
        <input id="s-name" v-model="form.name" type="text" required>
      </div>
      <div class="form-item">
        <label for="s-subtitle">副标题（页眉一行小字）</label>
        <input id="s-subtitle" v-model="form.subtitle" type="text">
      </div>
      <div class="form-item">
        <label for="s-announce">公告（页眉下方提示条，留空则不显示）</label>
        <input id="s-announce" v-model="form.announcement" type="text" placeholder="例如：博客新开张，欢迎留言">
      </div>
      <div class="form-item">
        <label for="s-about">关于页文字（每行一段）</label>
        <textarea id="s-about" v-model="form.aboutBio" rows="10"></textarea>
      </div>
      <div>
        <button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存设置' }}</button>
        <p class="form-hint">{{ msg }}</p>
      </div>
    </form>
  </div>
</template>
