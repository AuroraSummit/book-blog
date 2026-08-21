/** 纸色 / 夜间主题切换（localStorage 记忆，默认纸色）。 */
const KEY = 'pp:theme'

export function applyTheme() {
  const t = localStorage.getItem(KEY) || 'light'
  document.documentElement.dataset.theme = t
  return t
}

export function toggleTheme() {
  const next = (localStorage.getItem(KEY) || 'light') === 'light' ? 'night' : 'light'
  localStorage.setItem(KEY, next)
  document.documentElement.dataset.theme = next
  return next
}
