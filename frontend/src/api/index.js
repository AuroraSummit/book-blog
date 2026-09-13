import axios from 'axios'
import router from '../router'
import { useAuthStore } from '../stores/auth'

/** axios 实例：统一 /api 前缀、携带 token、解包 Result 包装。 */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一 { code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
      if (router.currentRoute.value.name !== 'admin-login') {
        router.push({ name: 'admin-login' })
      }
    }
    const msg = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(msg))
  }
)

export default http

/* ==================== 前台接口 ==================== */
export const api = {
  // 文章
  articles: (params) => http.get('/articles', { params }),
  searchArticles: (q) => http.get('/articles/search', { params: { q } }),
  article: (slug) => http.get(`/articles/${encodeURIComponent(slug)}`),
  randomArticle: () => http.get('/articles/random'),
  relatedArticles: (slug, limit = 3) => http.get(`/articles/${encodeURIComponent(slug)}/related`, { params: { limit } }),
  recordView: (slug) => http.post(`/articles/${encodeURIComponent(slug)}/views`),
  like: (slug) => http.post(`/articles/${encodeURIComponent(slug)}/like`),
  likedStatus: (slug) => http.get(`/articles/${encodeURIComponent(slug)}/liked`),
  tags: () => http.get('/tags'),
  about: () => http.get('/about'),
  siteInfo: () => http.get('/site'),

  // 评论（匿名即时发布 + 一层回复楼，website 为 honeypot 隐藏字段）
  comments: (slug) => http.get(`/articles/${encodeURIComponent(slug)}/comments`),
  postComment: (slug, data) => http.post(`/articles/${encodeURIComponent(slug)}/comments`, data),

  /* ==================== 后台接口 ==================== */
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),

  adminArticles: (status) => http.get('/admin/articles', { params: { status } }),
  adminArticle: (slug) => http.get(`/admin/articles/${encodeURIComponent(slug)}`),
  createArticle: (data) => http.post('/admin/articles', data),
  updateArticle: (slug, data) => http.put(`/admin/articles/${encodeURIComponent(slug)}`, data),
  deleteArticle: (slug) => http.delete(`/admin/articles/${encodeURIComponent(slug)}`),
  setArticleStatus: (slug, status) => http.patch(`/admin/articles/${encodeURIComponent(slug)}/status`, null, { params: { status } }),

  adminTags: () => http.get('/admin/tags'),
  renameTag: (name, newName) => http.put(`/admin/tags/${encodeURIComponent(name)}`, { newName }),
  deleteTag: (name) => http.delete(`/admin/tags/${encodeURIComponent(name)}`),

  adminSettings: () => http.get('/admin/settings'),
  updateSettings: (data) => http.put('/admin/settings', data),

  adminStats: () => http.get('/admin/stats'),

  // 后台评论管理
  adminComments: (params) => http.get('/admin/comments', { params }),
  adminCommentCounts: () => http.get('/admin/comments/counts'),
  adminReply: (data) => http.post('/admin/comments', data),
  deleteComment: (id) => http.delete(`/admin/comments/${id}`),
  setCommentStatus: (id, status) => http.patch(`/admin/comments/${id}/status`, null, { params: { status } })
}
