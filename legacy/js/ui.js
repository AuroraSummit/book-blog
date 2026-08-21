/* ============================================================
   「纸页之间」页面小工具（首页与文章页共用）
   - 阅读进度条：随滚动更新顶部细线（transform: scaleX，无 layout 抖动）
   - 回到顶部：滚动接近底部时淡入，点击平滑回顶
   原则：无花哨动画；尊重 prefers-reduced-motion。
   ============================================================ */

(function () {
  "use strict";

  var progress = document.getElementById("reading-progress");
  var backTop = document.getElementById("back-to-top");

  var ticking = false;

  /** 计算 0~1 的滚动进度，并更新进度条与按钮状态 */
  function update() {
    var doc = document.documentElement;
    var max = doc.scrollHeight - window.innerHeight;
    var ratio = max > 0 ? Math.min(Math.max(window.scrollY / max, 0), 1) : 0;

    if (progress) {
      progress.style.transform = "scaleX(" + ratio + ")";
      progress.setAttribute("aria-valuenow", Math.round(ratio * 100));
    }

    // 接近底部（滚动超过 88%）时显示「回到顶部」
    if (backTop) {
      backTop.classList.toggle("show", ratio > 0.88);
    }

    ticking = false;
  }

  function onScroll() {
    if (!ticking) {
      ticking = true;
      window.requestAnimationFrame(update);
    }
  }

  window.addEventListener("scroll", onScroll, { passive: true });
  update();

  /* ---------- 回到顶部 ---------- */
  if (backTop) {
    backTop.addEventListener("click", function () {
      var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      window.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" });
    });
  }

  /* ---------- 加载提示：逐字跳动 ----------
     在容器内注入一段文字，每个字符包成 span，
     依次向上跳起再落下（animation-delay 错开成波浪）。
     用法：showLoading(container, "正在翻页…") */
  function showLoading(container, text) {
    if (!container) return;
    container.innerHTML = "";
    var p = document.createElement("p");
    p.className = "loading";
    String(text || "正在翻页…").split("").forEach(function (ch, i) {
      var s = document.createElement("span");
      s.className = "wl-char";
      s.textContent = ch;
      s.style.animationDelay = i * 70 + "ms";   // 逐字错开，形成波浪
      p.appendChild(s);
    });
    container.appendChild(p);
  }
  window.showLoading = showLoading;
})();
