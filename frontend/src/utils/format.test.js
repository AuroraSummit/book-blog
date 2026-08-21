import { describe, it, expect } from 'vitest'
import { formatDate, highlight, parseContent, readingTime } from './format'

describe('formatDate', () => {
  it('格式化 ISO 日期', () => {
    expect(formatDate('2026-08-18')).toBe('2026 年 8 月 18 日')
  })
  it('非法日期原样返回', () => {
    expect(formatDate('不是日期')).toBe('不是日期')
  })
  it('空值返回空串', () => {
    expect(formatDate(null)).toBe('')
  })
})

describe('highlight', () => {
  it('先转义再高亮，杜绝 XSS', () => {
    const out = highlight('<script>alert(1)</script>', 'script')
    // 任何 <script> 标签都不能原样出现在输出中
    expect(out).not.toContain('<script>')
    expect(out).not.toContain('</script>')
    // 标签字符已被转义
    expect(out).toContain('&lt;')
    expect(out).toContain('&gt;')
    // 关键词被高亮包裹
    expect(out).toContain('<mark>script</mark>')
  })
  it('无关键词时仅转义', () => {
    expect(highlight('<b>hi</b>', '')).toBe('&lt;b&gt;hi&lt;/b&gt;')
  })
  it('转义特殊正则字符', () => {
    expect(highlight('a+b', '+')).toContain('<mark>+</mark>')
  })
})

describe('parseContent', () => {
  it('解析小标题 / 引用 / 段落', () => {
    const blocks = parseContent(['## 一点没想明白的地方', '> 满地都是六便士', '普通段落', ''])
    expect(blocks).toEqual([
      { type: 'h2', text: '一点没想明白的地方' },
      { type: 'quote', text: '满地都是六便士' },
      { type: 'p', text: '普通段落' }
    ])
  })
  it('忽略空行', () => {
    expect(parseContent(['  ', '', '有内容'])).toHaveLength(1)
  })
})

describe('readingTime', () => {
  it('按 400 字/分钟估算，最少 1 分钟', () => {
    expect(readingTime([])).toBe(1)
    const blocks = parseContent(['二三四五六七八九十'.repeat(40)]) // 恰好 400 字
    expect(readingTime(blocks)).toBe(1)
  })
  it('长文超过 1 分钟', () => {
    const blocks = parseContent(['二三四五六七八九十'.repeat(100)]) // 1000 字
    expect(readingTime(blocks)).toBe(3)
  })
})
