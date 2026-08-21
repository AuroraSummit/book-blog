/* ============================================================
   「纸页之间」通用列表渲染器
   ------------------------------------------------------------
   由 首页 / books.html / movies.html 共用：
     <body data-page="index" data-filter="reading">
   - 当存在 .filters 按钮组时，支持运行时切换（首页用）
   - 当仅 data-filter 预设时，固定类型（读书 / 影评页用）
   - 分页：每页 5 篇，超过一页自动显示 上一页/下一页
     当前页码同步到 URL ?page=N，刷新后保持
   ============================================================ */

(function () {
  "use strict";

  var PAGE_SIZE = 5;

  var listEl   = document.getElementById("article-list");
  var emptyEl  = document.getElementById("list-empty");
  var pageEl   = document.getElementById("page-count");
  var pagEl    = document.getElementById("pagination");
  var filterBtns = document.querySelectorAll(".filters button");

  if (!listEl) return;

  var presetType = document.body.dataset.filter;        // "reading" | "movie" | undefined
  var currentType = presetType || "all";
  var currentPage = 1;

  /* ---------- 从 URL 恢复页码 ---------- */
  (function initPage() {
    var p = parseInt(new URLSearchParams(location.search).get("page"), 10);
    currentPage = (isNaN(p) || p < 1) ? 1 : p;
  })();

  /** 日期格式化为「2026 年 8 月 18 日」 */
  function formatDate(iso) {
    var d = new Date(iso);
    return d.getFullYear() + " 年 " + (d.getMonth() + 1) + " 月 " + d.getDate() + " 日";
  }

  /** 渲染一条文章（书页式分隔条目） */
  function renderItem(article) {
    var a = document.createElement("a");
    a.className = "article-item";
    a.href = "article.html?id=" + encodeURIComponent(article.id);
    a.setAttribute("aria-label", "阅读全文：" + article.title);

    var meta = document.createElement("div");
    meta.className = "item-meta";
    meta.innerHTML =
      '<span class="item-date">' + formatDate(article.date) + "</span>" +
      '<span class="tag ' + article.type + '">' + article.category + "</span>" +
      '<span class="item-stats">阅读 ' + (article.views || 0) +
      " · 评论 " + (window.paperAdmin.comments.count(article.id) || 0) + "</span>";

    var title = document.createElement("h3");
    title.className = "item-title";
    title.textContent = article.title;

    var summary = document.createElement("p");
    summary.className = "item-summary";
    summary.textContent = article.summary;

    a.append(meta, title, summary);
    return a;
  }

  function renderList(articles) {
    listEl.innerHTML = "";
    if (!articles.length) {
      if (emptyEl) emptyEl.style.display = "block";
    } else {
      if (emptyEl) emptyEl.style.display = "none";
      articles.forEach(function (a) { listEl.appendChild(renderItem(a)); });
    }
    if (pageEl) pageEl.textContent = "共 " + totalCount + " 篇";
  }

  /* ---------- 分页 ---------- */
  var totalCount = 0;
  var totalPages = 1;

  function renderPagination() {
    if (!pagEl) return;
    pagEl.innerHTML = "";

    if (totalPages <= 1) return;   // 不超过一页则不显示分页

    // 上一页
    var prev = document.createElement("button");
    prev.type = "button";
    prev.className = "p-btn";
    prev.textContent = "← 上一页";
    prev.disabled = currentPage <= 1;
    prev.addEventListener("click", function () { goTo(currentPage - 1); });
    pagEl.appendChild(prev);

    // 页码信息
    var info = document.createElement("span");
    info.className = "p-info";
    info.textContent = "第 " + currentPage + " 页 · 共 " + totalPages + " 页";
    pagEl.appendChild(info);

    // 下一页
    var next = document.createElement("button");
    next.type = "button";
    next.className = "p-btn";
    next.textContent = "下一页 →";
    next.disabled = currentPage >= totalPages;
    next.addEventListener("click", function () { goTo(currentPage + 1); });
    pagEl.appendChild(next);
  }

  function goTo(page) {
    if (page < 1 || page > totalPages || page === currentPage) return;
    currentPage = page;

    // 同步 URL（不刷新页面）
    var url = new URL(location.href);
    if (currentPage > 1) url.searchParams.set("page", String(currentPage));
    else url.searchParams.delete("page");
    history.replaceState(null, "", url);

    load();
    // 翻页后回到列表顶部（尊重系统减少动效偏好）
    var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    window.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" });
  }

  /* ---------- 加载当前页 ---------- */
  async function load() {
    try {
      showLoading(listEl, "正在翻页…");
      if (emptyEl) emptyEl.style.display = "none";

      var opts = currentType === "all" ? {} : { type: currentType };
      var list = await fetchArticles(opts);

      totalCount = list.length;
      totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));
      if (currentPage > totalPages) currentPage = totalPages;

      var slice = list.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
      renderList(slice);
      renderPagination();
    } catch (err) {
      listEl.innerHTML = '<p class="load-error">文章加载失败，请稍后重试。</p>';
      console.error("[纸页之间] 文章加载失败：", err);
    }
  }

  // 运行时筛选（切换时回到第 1 页）
  filterBtns.forEach(function (btn) {
    btn.addEventListener("click", function () {
      filterBtns.forEach(function (b) { b.classList.remove("active"); });
      this.classList.add("active");
      currentType = this.dataset.filter;
      currentPage = 1;
      load();
    });
  });

  load();
})();