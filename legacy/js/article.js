/* ============================================================
   「纸页之间」文章详情页逻辑
   - 从 URL 参数读取文章 id（article.html?id=xxx）
   - 渲染全文：支持 "## " 小标题、"> " 引用块等轻量标记
   - 页尾提供 上一篇 / 下一篇 安静导航
   ============================================================ */

(function () {
  "use strict";

  const articleEl = document.getElementById("article");

  /** 日期格式化为「2026 年 8 月 18 日」 */
  function formatDate(iso) {
    const d = new Date(iso);
    return d.getFullYear() + " 年 " + (d.getMonth() + 1) + " 月 " + d.getDate() + " 日";
  }

  /**
   * 将文章正文数组渲染为 HTML。
   * 轻量标记约定：
   *   "## " 开头 → <h2> 小节标题
   *   "> "  开头 → <blockquote> 引用块
   *   其余       → <p> 段落
   * （文本一律使用 textContent 注入，避免 XSS）
   */
  function renderContent(lines) {
    const frag = document.createDocumentFragment();

    lines.forEach(function (line) {
      if (!line || !line.trim()) return;
      if (line.indexOf("## ") === 0) {
        const h2 = document.createElement("h2");
        h2.textContent = line.slice(3).trim();
        frag.appendChild(h2);
      } else if (line.indexOf("> ") === 0) {
        const bq = document.createElement("blockquote");
        const p = document.createElement("p");
        p.textContent = line.slice(2).trim();
        bq.appendChild(p);
        frag.appendChild(bq);
      } else {
        const p = document.createElement("p");
        p.textContent = line.trim();
        frag.appendChild(p);
      }
    });

    return frag;
  }

  /** 文章头部：分类 + 日期 + 阅读数/更新时间 + 标题 */
  function renderHead(article) {
    const head = document.createElement("header");
    head.className = "article-head";

    const meta = document.createElement("div");
    meta.className = "article-meta";
    meta.innerHTML =
      '<span class="tag ' + article.type + '">' + article.category + "</span>" +
      '<span>' + formatDate(article.date) + "</span>" +
      '<span>阅读 ' + (article.views || 0) + "</span>" +
      '<span>更新于 ' + (article.updatedAt || article.date) + "</span>";

    const h1 = document.createElement("h1");
    h1.textContent = article.title;

    head.append(meta, h1);
    return head;
  }

  /** 上下篇导航（不跳出当前分类，按日期顺序取相邻篇目） */
  function renderNav(list, currentId) {
    const idx = list.findIndex((a) => a.id === currentId);
    const prev = list[idx + 1] || null;   // 上一篇（更早）
    const next = list[idx - 1] || null;   // 下一篇（更新）

    const nav = document.createElement("nav");
    nav.className = "article-nav";
    nav.setAttribute("aria-label", "文章导航");

    if (prev) {
      const a = document.createElement("a");
      a.href = "article.html?id=" + encodeURIComponent(prev.id);
      a.innerHTML = '<span class="nav-label">上一篇</span>' + prev.title;
      nav.appendChild(a);
    } else {
      nav.appendChild(document.createElement("span")); // 保持两端对齐
    }

    if (next) {
      const a = document.createElement("a");
      a.className = "nav-next";
      a.href = "article.html?id=" + encodeURIComponent(next.id);
      a.innerHTML = '<span class="nav-label">下一篇</span>' + next.title;
      nav.appendChild(a);
    }

    return nav;
  }

  /** 结尾小装饰 + 页脚「返回列表」 */
  function renderEnd() {
    const end = document.createElement("div");
    end.className = "article-end";
    end.textContent = "❦"; // 静物符号，无图片
    return end;
  }

  /** 评论区：列表 + 发表表单
      ★ 接口预留位 ★
        GET  /api/articles/:id/comments   → 列表
        POST /api/articles/:id/comments   → 发表 { author, content }
  */
  function renderComments(articleId) {
    const wrap = document.createElement("section");
    wrap.className = "comments";

    const title = document.createElement("h2");
    title.className = "comments-title";
    title.textContent = "评论";

    const list = document.createElement("div");
    list.className = "comments-list";

    // 表单
    const form = document.createElement("form");
    form.className = "comment-form";
    form.noValidate = true;

    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.placeholder = "昵称（可不填，默认匿名读者）";
    nameInput.maxLength = 20;
    nameInput.setAttribute("aria-label", "昵称");

    const contentInput = document.createElement("textarea");
    contentInput.rows = 3;
    contentInput.placeholder = "写下你的想法…";
    contentInput.maxLength = 500;
    contentInput.setAttribute("aria-label", "评论内容");
    contentInput.required = true;

    const submitBtn = document.createElement("button");
    submitBtn.type = "submit";
    submitBtn.className = "btn";
    submitBtn.textContent = "发表评论";

    form.appendChild(nameInput);
    form.appendChild(contentInput);
    form.appendChild(submitBtn);

    function refresh() {
      const items = window.paperAdmin.comments.list(articleId);
      title.textContent = "评论（" + items.length + "）";
      list.innerHTML = "";
      if (!items.length) {
        const empty = document.createElement("p");
        empty.className = "comments-empty";
        empty.textContent = "还没有评论，来写下第一条吧。";
        list.appendChild(empty);
        return;
      }
      items.forEach(function (c) {
        const item = document.createElement("div");
        item.className = "comment-item";

        const head = document.createElement("div");
        head.className = "comment-head";
        const author = document.createElement("span");
        author.className = "comment-author";
        author.textContent = c.author;
        const date = document.createElement("span");
        date.className = "comment-date";
        date.textContent = formatDate(c.date);
        head.appendChild(author);
        head.appendChild(date);

        const content = document.createElement("p");
        content.className = "comment-content";
        content.textContent = c.content;

        item.appendChild(head);
        item.appendChild(content);
        list.appendChild(item);
      });
    }

    form.addEventListener("submit", function (e) {
      e.preventDefault();
      const content = contentInput.value.trim();
      if (!content) return;
      const author = nameInput.value.trim() || "匿名读者";
      window.paperAdmin.comments.add(articleId, author, content);
      nameInput.value = "";
      contentInput.value = "";
      refresh();
    });

    refresh();
    wrap.appendChild(title);
    wrap.appendChild(list);
    wrap.appendChild(form);
    return wrap;
  }

  /* ---------- 初始化：读取 URL 参数 → 拉取文章 → 渲染 ---------- */
  (async function init() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");

    if (!id) {
      articleEl.innerHTML = '<p class="not-found">没有找到这篇文章。</p>';
      return;
    }

    try {
      showLoading(articleEl, "正在翻页…");
      const list = await fetchArticles();
      const article = await fetchArticleById(id);

      if (!article) {
        articleEl.innerHTML = '<p class="not-found">没有找到这篇文章，它可能已被删除。</p>';
        return;
      }

      // 更新浏览器标题
      document.title = article.title + " · 纸页之间";

      articleEl.appendChild(renderHead(article));

      const body = document.createElement("div");
      body.className = "article-body";
      body.appendChild(renderContent(article.content));
      articleEl.appendChild(body);

      articleEl.appendChild(renderEnd());
      articleEl.appendChild(renderNav(list, id));
      articleEl.appendChild(renderComments(id));

      // 记录一次阅读（历史观看数 +1）
      // ★ 接口预留位 ★  POST /api/articles/:id/views
      if (window.paperAdmin && window.paperAdmin.recordView) {
        window.paperAdmin.recordView(id);
      }

      window.scrollTo(0, 0);
    } catch (err) {
      articleEl.innerHTML = '<p class="load-error">文章加载失败，请稍后重试。</p>';
      console.error("[纸页之间] 文章加载失败：", err);
    }
  })();
})();
