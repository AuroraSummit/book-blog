import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '纸页之间 · 读书与观影手记' } },
  { path: '/books', name: 'books', component: () => import('../views/ListView.vue'), meta: { title: '读书笔记 · 纸页之间', filter: 'reading', heading: '读书笔记' } },
  { path: '/movies', name: 'movies', component: () => import('../views/ListView.vue'), meta: { title: '影评 · 纸页之间', filter: 'movie', heading: '影评' } },
  { path: '/tags', name: 'tags', component: () => import('../views/TagsView.vue'), meta: { title: '标签 · 纸页之间' } },
  { path: '/tag/:tag', name: 'tag', component: () => import('../views/ListView.vue'), meta: { title: '标签 · 纸页之间' } },
  { path: '/search', name: 'search', component: () => import('../views/SearchView.vue'), meta: { title: '搜索 · 纸页之间' } },
  { path: '/article/:slug', name: 'article', component: () => import('../views/ArticleView.vue'), meta: { title: '文章 · 纸页之间' } },
  { path: '/about', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: '关于 · 纸页之间' } },

  // ---------- 后台 ----------
  { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/AdminLogin.vue'), meta: { title: '登录 · 纸页之间后台' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, title: '后台 · 纸页之间' },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { title: '仪表盘 · 纸页之间后台' } },
      { path: 'articles', name: 'admin-articles', component: () => import('../views/admin/ArticlesView.vue'), meta: { title: '文章管理 · 纸页之间后台' } },
      { path: 'write', name: 'admin-write', component: () => import('../views/admin/WriteView.vue'), meta: { title: '写文章 · 纸页之间后台' } },
      { path: 'tags', name: 'admin-tags', component: () => import('../views/admin/AdminTagsView.vue'), meta: { title: '标签管理 · 纸页之间后台' } },
      { path: 'settings', name: 'admin-settings', component: () => import('../views/admin/AdminSettingsView.vue'), meta: { title: '站点设置 · 纸页之间后台' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 登录守卫 + 页面标题
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'admin-login' && auth.token) {
    return { name: 'admin-dashboard' }
  }
  return true
})

router.afterEach((to) => {
  const base = to.meta.title || '纸页之间'
  document.title = to.name === 'tag' ? `标签：${to.params.tag} · 纸页之间` : base
})

export default router
