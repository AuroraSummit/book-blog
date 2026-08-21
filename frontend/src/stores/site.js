import { defineStore } from 'pinia'
import { api } from '../api'

/** 站点公开信息（站名/副标题/公告），App 启动时加载一次。 */
export const useSiteStore = defineStore('site', {
  state: () => ({
    name: '纸页之间',
    subtitle: '读书笔记 · 观影感受 · 安静记录',
    announcement: '',
    loaded: false
  }),
  actions: {
    async fetch() {
      try {
        const data = await api.siteInfo()
        this.name = data.name || this.name
        this.subtitle = data.subtitle || this.subtitle
        this.announcement = data.announcement || ''
      } catch {
        // 后端不可用时保持默认值
      } finally {
        this.loaded = true
      }
    }
  }
})
