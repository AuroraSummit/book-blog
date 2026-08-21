/* ============================================================
   「纸页之间」关于页逻辑
   - 调 fetchAbout() 获取一段简短文字描述
   - 只渲染标题 + 文字，不展示任何个人信息
   - createElement + textContent 注入，防 XSS
   ============================================================ */

(function () {
  "use strict";

  const root = document.getElementById("about-page");
  if (!root) return;

  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }

  async function load() {
    try {
      showLoading(root, "正在翻页…");
      var me = await fetchAbout();

      root.innerHTML = "";
      root.appendChild(el("h2", "about-page-title", "关于这里"));

      var bio = el("div", "about-bio");
      (me.bio || []).forEach(function (p) { bio.appendChild(el("p", null, p)); });
      root.appendChild(bio);
    } catch (err) {
      root.innerHTML = '<p class="load-error">关于信息加载失败。</p>';
      console.error("[纸页之间] 关于加载失败：", err);
    }
  }

  load();
})();