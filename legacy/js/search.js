/* ============================================================
   「纸页之间」搜索结果页逻辑
   - 读取 URL 参数 ?q=xxx
   - 调用 searchArticles(q) 渲染命中结果
   - 显示关键词、命中条数、命中段落关键词高亮
   ============================================================ */

(function () {
  "use strict";

  const listEl    = document.getElementById("article-list");
  const keywordEl = document.getElementById("search-keyword");
  const countEl   = document.getElementById("search-count");
  const emptyEl   = document.getElementById("search-empty");

  if (!listEl) return;

  const params = new URLSearchParams(window.location.search);
  const q = (params.get("q") || "").trim();

  if (keywordEl) keywordEl.textContent = q ? '「' + q + '」' : "空";
  document.title = (q ? q + " · 搜索" : "搜索") + " · 纸页之间";

  function formatDate(iso) {
    const d = new Date(iso);
    return d.getFullYear() + " 年 " + (d.getMonth() + 1) + " 月 " + d.getDate() + " 日";
  }

  /** 渲染一条搜索结果，summary 含关键词时高亮 */
  function renderItem(article, q) {
    const a = document.createElement("a");
    a.className = "article-item";
    a.href = "article.html?id=" + encodeURIComponent(article.id);

    const meta = document.createElement("div");
    meta.className = "item-meta";
    meta.innerHTML =
      '<span class="item-date">' + formatDate(article.date) + "</span>" +
      '<span class="tag ' + article.type + '">' + article.category + "</span>" +
      '<span class="item-stats">阅读 ' + (article.views || 0) +
      " · 评论 " + (window.paperAdmin.comments.count(article.id) || 0) + "</span>";

    const title = document.createElement("h3");
    title.className = "item-title";
    title.innerHTML = highlight(article.title, q);

    const summary = document.createElement("p");
    summary.className = "item-summary";
    summary.innerHTML = highlight(article.summary, q);

    a.append(meta, title, summary);
    return a;
  }

  /** 把关键词在文本中包一层 <mark>（无注入风险：转义后再插入 mark） */
  function highlight(text, q) {
    const safe = String(text).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
    if (!q) return safe;
    const re = new RegExp("(" + escapeRegExp(q) + ")", "gi");
    return safe.replace(re, '<mark>$1</mark>');
  }
  function escapeRegExp(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"); }

  async function load() {
    try {
      if (!q) {
        if (emptyEl) emptyEl.style.display = "block";
        if (countEl) countEl.textContent = "0 篇";
        return;
      }
      showLoading(listEl, "正在翻找…");
      const list = await searchArticles(q);
      listEl.innerHTML = "";
      if (!list.length) {
        if (emptyEl) emptyEl.style.display = "block";
      } else {
        if (emptyEl) emptyEl.style.display = "none";
        list.forEach((a) => listEl.appendChild(renderItem(a, q)));
      }
      if (countEl) countEl.textContent = list.length + " 篇";
    } catch (err) {
      listEl.innerHTML = '<p class="load-error">搜索失败，请稍后重试。</p>';
      console.error("[纸页之间] 搜索失败：", err);
    }
  }

  load();
})();