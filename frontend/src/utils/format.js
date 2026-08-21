/** 日期格式化为「2026 年 8 月 18 日」。 */
export function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(String(iso).length === 10 ? iso + 'T00:00:00' : iso)
  if (isNaN(d.getTime())) return String(iso)
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`
}

function escapeRegExp(s) {
  return String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[c])
}

/**
 * 搜索结果关键词高亮：先转义再包 <mark>，无注入风险。
 * 返回 HTML 字符串，仅用于标题/摘要渲染。
 */
export function highlight(text, q) {
  const safe = escapeHtml(text ?? '')
  if (!q) return safe
  try {
    const re = new RegExp('(' + escapeRegExp(q) + ')', 'gi')
    return safe.replace(re, '<mark>$1</mark>')
  } catch {
    return safe
  }
}

/**
 * 解析正文行：
 * "## " 开头 → 小节标题；"> " 开头 → 引用；其余 → 段落。
 * 返回 [{ type: 'h2'|'quote'|'p', text }]，渲染端用 textContent 输出，杜绝 XSS。
 */
export function parseContent(lines) {
  return (lines || [])
    .map((line) => String(line ?? '').trim())
    .filter(Boolean)
    .map((line) => {
      if (line.startsWith('## ')) return { type: 'h2', text: line.slice(3).trim() }
      if (line.startsWith('> ')) return { type: 'quote', text: line.slice(2).trim() }
      return { type: 'p', text: line }
    })
}

/** 是否已在本会话阅读过（用于阅读计数去重，避免刷新重复 +1）。 */
export function isViewed(slug) {
  return sessionStorage.getItem('pp:viewed:' + slug) === '1'
}

export function markViewed(slug) {
  sessionStorage.setItem('pp:viewed:' + slug, '1')
}

/** 估算阅读时长：按中文阅读速度约 400 字/分钟。 */
export function readingTime(blocks) {
  const total = (blocks || []).reduce((sum, b) => sum + (b.text ? b.text.length : 0), 0)
  return Math.max(1, Math.ceil(total / 400))
}
