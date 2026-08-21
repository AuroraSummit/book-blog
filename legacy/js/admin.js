/* ============================================================
   「纸页之间」管理后台逻辑
   ------------------------------------------------------------
   - 登录守卫：未登录跳转 login.html（演示：localStorage flag）
   - hash 路由：#/dashboard #/articles #/write
   - 文章 CRUD：当前写入 localStorage（由 data.js 提供 paperAdmin），
     所有写操作处均注释了对应的管理端 API 调用
   - 视图内容全部 createElement + textContent 注入（防 XSS）
   ============================================================ */

(function () {
  "use strict";

  /* ---------- 登录守卫 ----------
     后端就绪后：检查 token / httpOnly Cookie 是否存在，
     或调用 GET /api/admin/me 校验，失败则跳回 login.html */
  if (localStorage.getItem("paper-pages:admin") !== "1") {
    location.href = "login.html";
    return;
  }

  var toastEl = document.getElementById("toast");
  var views = ["dashboard", "articles", "write"];
  var currentView = "dashboard";

  /* ---------- 工具 ---------- */
  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }
  function formatDate(iso) {
    var d = new Date(iso);
    if (isNaN(d)) return iso;
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  }
  var toastTimer = null;
  function toast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove("show"); }, 2200);
  }

  /* ---------- 路由 ---------- */
  function route() {
    var hash = location.hash.replace(/^#\/?/, "").split("?")[0];
    var view = views.indexOf(hash) >= 0 ? hash : "dashboard";
    show(view);
  }
  function show(view) {
    currentView = view;
    views.forEach(function (v) {
      document.getElementById("view-" + v).classList.toggle("active", v === view);
    });
    document.querySelectorAll(".admin-side a").forEach(function (a) {
      a.classList.toggle("active", a.dataset.view === view);
    });
    render[view]();
  }

  /* ---------- 保存文章（新建/编辑共用） ----------
     ★ 接口预留位 ★  后端就绪后，替换下方 localStorage 调用：
     const res = await fetch("/api/articles", {
       method: article.id ? "PUT" : "POST",
       headers: {
         "Content-Type": "application/json",
         "Authorization": "Bearer " + localStorage.getItem("token")
       },
       body: JSON.stringify(article)
     });
     if (!res.ok) throw new Error("保存失败");
  */
  function saveArticle(article) {
    window.paperAdmin.saveArticle(article);
  }

  /* ---------- 删除文章 ----------
     ★ 接口预留位 ★
     const res = await fetch("/api/articles/" + id, {
       method: "DELETE",
       headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
     });
  */
  function deleteArticle(id) {
    window.paperAdmin.deleteArticle(id);
  }

  /* ============================================================
     视图 1：仪表盘（统计卡片 + ECharts 图表）
     ============================================================ */
  var dashboard = {
    render: function () {
      var root = document.getElementById("view-dashboard");
      root.innerHTML = "";
      root.appendChild(el("h2", "view-title", "仪表盘"));

      var statGrid = el("div", "stat-grid");
      var card = function (num, label) {
        var c = el("div", "stat-card");
        c.appendChild(el("div", "num", String(num)));
        c.appendChild(el("div", "label", label));
        return c;
      };

      fetchAdminArticles().then(function (all) {
        var books = all.filter(function (a) { return a.type === "reading"; }).length;
        var movies = all.filter(function (a) { return a.type === "movie"; }).length;
        var drafts = all.filter(function (a) { return a.status === "draft"; }).length;

        statGrid.appendChild(card(all.length, "全部文章"));
        statGrid.appendChild(card(books, "读书笔记"));
        statGrid.appendChild(card(movies, "观影感受"));
        statGrid.appendChild(card(drafts, "草稿"));
        root.appendChild(statGrid);

        // 图表区：访客趋势 + 文章发表
        var charts = el("div", "charts-grid");
        charts.innerHTML =
          '<div class="chart-box">' +
            '<p class="chart-title">访客趋势 · 近 14 天</p>' +
            '<div id="chart-visitors" style="height:260px;"></div>' +
          '</div>' +
          '<div class="chart-box">' +
            '<p class="chart-title">文章发表数量 · 按月</p>' +
            '<div id="chart-publish" style="height:260px;"></div>' +
          '</div>';
        root.appendChild(charts);
        renderCharts();

        root.appendChild(el("p", "dash-section-title", "最近的文章"));
        var list = el("div");
        all.slice(0, 5).forEach(function (a) {
          var row = el("div", "article-row");
          row.appendChild(el("span", "row-title", a.title));
          row.appendChild(el("span", "row-meta", a.date + " · " + a.category + (a.status === "draft" ? " · 草稿" : "")));
          var link = el("a", null, "查看");
          link.href = "../article.html?id=" + encodeURIComponent(a.id);
          link.target = "_blank";
          link.rel = "noopener";
          var actions = el("span", "row-actions");
          actions.appendChild(link);
          row.appendChild(actions);
          list.appendChild(row);
        });
        root.appendChild(list);
      }).catch(function () {
        root.appendChild(el("p", "load-error", "数据加载失败。"));
      });
    }
  };

  /* ---------- ECharts：访客趋势 + 文章发表 ----------
     ★ 接口预留位 ★  GET /api/admin/stats → { visitors, published }
  */
  var chartInstances = [];
  function renderCharts() {
    if (!window.echarts) {
      document.querySelectorAll(".chart-box").forEach(function (box) {
        box.appendChild(el("p", "chart-hint", "图表库未加载"));
      });
      return;
    }
    var lineColor = "#8a6a3d";
    var gridColor = "#e0d7c5";
    var labelColor = "#9a9083";

    // 1) 访客趋势（折线）
    var v = window.paperAdmin.visitorSeries(14);
    var vc = echarts.init(document.getElementById("chart-visitors"));
    vc.setOption({
      color: [lineColor],
      tooltip: { trigger: "axis" },
      grid: { left: 44, right: 16, top: 26, bottom: 30 },
      xAxis: { type: "category", boundaryGap: false, data: v.map(function (d) { return d.date; }),
        axisLine: { lineStyle: { color: gridColor } }, axisLabel: { color: labelColor, fontSize: 11 } },
      yAxis: { type: "value", splitLine: { lineStyle: { color: gridColor } },
        axisLabel: { color: labelColor, fontSize: 11 } },
      series: [{
        name: "访客", type: "line", smooth: true,
        areaStyle: { opacity: .08 },
        data: v.map(function (d) { return d.count; })
      }]
    });
    chartInstances.push(vc);

    // 2) 文章发表（柱状，按月聚合真实数据）
    fetchAdminArticles().then(function (list) {
      var map = {};
      list.forEach(function (a) {
        var m = (a.date || "").slice(0, 7);
        if (m) map[m] = (map[m] || 0) + 1;
      });
      var months = Object.keys(map).sort();
      var pc = echarts.init(document.getElementById("chart-publish"));
      pc.setOption({
        color: [lineColor],
        tooltip: { trigger: "axis" },
        grid: { left: 44, right: 16, top: 26, bottom: 30 },
        xAxis: { type: "category", data: months,
          axisLine: { lineStyle: { color: gridColor } }, axisLabel: { color: labelColor, fontSize: 11 } },
        yAxis: { type: "value", splitLine: { lineStyle: { color: gridColor } },
          axisLabel: { color: labelColor, fontSize: 11 } },
        series: [{
          name: "发表", type: "bar", barWidth: 22,
          itemStyle: { color: lineColor, borderRadius: [2, 2, 0, 0] },
          data: months.map(function (m) { return map[m]; })
        }]
      });
      chartInstances.push(pc);
    });
  }

  window.addEventListener("resize", function () {
    chartInstances.forEach(function (c) { c.resize(); });
  });

  /* ============================================================
     视图 2：文章管理（已发布 / 草稿 筛选）
     ============================================================ */
  var articles = {
    filterStatus: "all",   // all | published | draft

    render: function () {
      var root = document.getElementById("view-articles");
      root.innerHTML = "";
      root.appendChild(el("h2", "view-title", "文章管理"));

      var head = el("div", "list-head", null);
      var filters = el("span", "filters", null);
      var mk = function (label, val) {
        var b = el("button", this.filterStatus === val ? "active" : "", label);
        b.type = "button";
        b.addEventListener("click", function () {
          articles.filterStatus = val;
          articles.render();
        });
        return b;
      }.bind(this);
      filters.appendChild(mk("全部", "all"));
      filters.appendChild(mk("已发布", "published"));
      filters.appendChild(mk("草稿", "draft"));
      head.appendChild(filters);

      var btn = el("a", "btn", "＋ 写文章");
      btn.href = "#/write";
      head.appendChild(btn);
      head.style.display = "flex";
      head.style.justifyContent = "space-between";
      head.style.alignItems = "center";
      head.style.marginBottom = "20px";
      root.appendChild(head);

      var listWrap = el("div");
      root.appendChild(listWrap);
      listWrap.appendChild(el("p", "loading", "正在翻页…"));

      fetchAdminArticles().then(function (all) {
        listWrap.innerHTML = "";
        var list = articles.filterStatus === "all"
          ? all
          : all.filter(function (a) { return a.status === articles.filterStatus; });

        if (!list.length) {
          listWrap.appendChild(el("p", "empty-state",
            articles.filterStatus === "draft" ? "还没有草稿。" : "还没有文章。"));
          return;
        }
        list.forEach(function (a) {
          var row = el("div", "article-row");

          var title = el("span", "row-title", a.title);
          var meta = el("span", "row-meta",
            a.date + " · " + a.category + (a.status === "draft" ? " · 草稿" : "") +
            " · 阅读 " + (a.views || 0));

          var actions = el("span", "row-actions");

          if (a.status === "draft") {
            var pub = el("button", null, "发布");
            pub.type = "button";
            pub.addEventListener("click", function () {
              // ★ 接口：PUT /api/articles/:id { status: "published" }
              window.paperAdmin.saveArticle(Object.assign({}, a, { status: "published" }));
              toast("已发布");
              articles.render();
            });
            actions.appendChild(pub);
          }

          var edit = el("a", null, "编辑");
          edit.href = "#/write?id=" + encodeURIComponent(a.id);
          var del = el("button", "danger", "删除");
          del.type = "button";
          del.addEventListener("click", function () {
            if (!window.confirm("确定删除《" + a.title + "》？此操作不可撤销。")) return;
            deleteArticle(a.id);           // ★ 接口：DELETE /api/articles/:id
            toast("已删除");
            articles.render();
          });

          actions.appendChild(edit);
          actions.appendChild(del);
          row.appendChild(title);
          row.appendChild(meta);
          row.appendChild(actions);
          listWrap.appendChild(row);
        });
      }).catch(function () {
        listWrap.innerHTML = "";
        listWrap.appendChild(el("p", "load-error", "文章加载失败。"));
      });
    }
  };

  /* ============================================================
     视图 3：写文章（新建 / 编辑）
     ============================================================ */
  var write = {
    editId: null,

    render: function () {
      var root = document.getElementById("view-write");
      root.innerHTML = "";
      var q = new URLSearchParams(location.hash.split("?")[1] || "");
      this.editId = q.get("id") || null;

      root.appendChild(el("h2", "view-title", this.editId ? "编辑文章" : "写文章"));

      var form = el("form");
      form.id = "article-form";
      form.noValidate = true;

      var f = function (labelText, id, inputHtml) {
        var box = el("div", "form-item");
        var label = el("label", null, labelText);
        label.htmlFor = id;
        box.appendChild(label);
        var wrap = el("div");
        wrap.innerHTML = inputHtml;   // 仅静态模板，无用户数据
        box.appendChild(wrap.firstChild);
        return box;
      };

      form.appendChild(f("标题", "f-title",
        '<input type="text" id="f-title" name="title" required placeholder="例如：《月亮与六便士》读后…">'));

      var row = el("div", "form-row");
      row.appendChild(f("分类", "f-type",
        '<select id="f-type" name="type">' +
          '<option value="reading">读书笔记</option>' +
          '<option value="movie">观影感受</option>' +
        '</select>'));
      row.appendChild(f("日期", "f-date",
        '<input type="date" id="f-date" name="date" required>'));
      form.appendChild(row);

      form.appendChild(f("标签（用逗号分隔）", "f-tags",
        '<input type="text" id="f-tags" name="tags" placeholder="例如：毛姆, 理想主义, 外国文学">'));
      form.appendChild(f("摘要（首页卡片显示）", "f-summary",
        '<textarea id="f-summary" name="summary" rows="2" required></textarea>'));
      form.appendChild(f("正文（每行一段；「## 」开头为小标题，「> 」开头为引用）", "f-content",
        '<textarea id="f-content" name="content" rows="14" required></textarea>'));

      var actions = el("div", null);
      var saveBtn = el("button", "btn btn-primary", this.editId ? "保存修改" : "发布文章");
      saveBtn.type = "submit";
      saveBtn.dataset.status = "published";
      var draftBtn = el("button", "btn btn-ghost", "存为草稿");
      draftBtn.type = "submit";
      draftBtn.dataset.status = "draft";
      var cancelBtn = el("a", "btn btn-ghost", "取消");
      cancelBtn.href = "#/articles";
      actions.appendChild(saveBtn);
      actions.appendChild(draftBtn);
      actions.appendChild(cancelBtn);
      form.appendChild(actions);

      root.appendChild(form);

      // 编辑模式：回填（用 fetchAdminArticles，保证草稿也能编辑）
      if (this.editId) {
        form.appendChild(el("p", "load-error", "正在加载…"));
        fetchAdminArticles().then(function (list) {
          var a = list.find(function (x) { return x.id === write.editId; });
          if (!a) {
            form.innerHTML = "";
            root.appendChild(el("p", "empty-state", "文章不存在或已被删除。"));
            return;
          }
          form.querySelector(".load-error").remove();
          document.getElementById("f-title").value = a.title;
          document.getElementById("f-type").value = a.type;
          document.getElementById("f-date").value = a.date;
          document.getElementById("f-tags").value = (a.tags || []).join(", ");
          document.getElementById("f-summary").value = a.summary;
          document.getElementById("f-content").value = (a.content || []).join("\n");
        });
      }

      // 提交：发布 / 存草稿
      form.addEventListener("submit", function (e) {
        e.preventDefault();
        var status = e.submitter ? e.submitter.dataset.status : "published";
        var title = document.getElementById("f-title").value.trim();
        var type = document.getElementById("f-type").value;
        var date = document.getElementById("f-date").value;
        var tags = document.getElementById("f-tags").value.split(/[,，]/).map(function (s) { return s.trim(); }).filter(Boolean);
        var summary = document.getElementById("f-summary").value.trim();
        var content = document.getElementById("f-content").value.split("\n").map(function (s) { return s.trim(); }).filter(Boolean);

        if (!title || !date || !summary || !content.length) {
          toast("请填写标题、日期、摘要和正文");
          return;
        }

        var article = {
          id: write.editId || ("p" + Date.now()),   // 新建时生成唯一 id
          title: title,
          type: type,
          category: type === "movie" ? "观影感受" : "读书笔记",
          date: date,
          tags: tags,
          summary: summary,
          content: content,
          status: status,                             // published | draft
          updatedAt: new Date().toISOString().slice(0, 10)   // 记录修改时间
        };

        saveArticle(article);   // ★ 接口：POST /api/articles 或 PUT /api/articles/:id
        toast(status === "draft" ? "已存入草稿箱" : (write.editId ? "已保存修改" : "已发布"));
        location.hash = "#/articles";
      });
    }
  };

  var render = {
    dashboard: function () { dashboard.render(); },
    articles: function () { articles.render(); },
    write: function () { write.render(); }
  };

  /* ---------- 事件 ---------- */
  document.getElementById("logout").addEventListener("click", function (e) {
    e.preventDefault();
    localStorage.removeItem("paper-pages:admin");
    location.href = "login.html";
  });

  window.addEventListener("hashchange", route);

  // 首次进入：默认仪表盘
  if (!location.hash) location.hash = "#/dashboard";
  route();
})();