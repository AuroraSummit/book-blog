/**
 * 从 legacy/js/data.js 提取 ARTICLES / ABOUT，生成后端种子数据 JSON。
 * 用法：node scripts/extract-seed.mjs
 */
import fs from "node:fs";
import vm from "node:vm";
import path from "node:path";

const dataJs = path.resolve("legacy/js/data.js");
const code = fs.readFileSync(dataJs, "utf8");

const sandbox = { window: {}, console };
vm.createContext(sandbox);
// const/let 不挂全局，追加一段同作用域代码把值导出
const boot = code + "\n;globalThis.__out = { articles: ARTICLES, about: ABOUT };";
vm.runInContext(boot, sandbox, { filename: "data.js" });

const { articles: rawArticles, about: rawAbout } = sandbox.__out || {};

const articles = (rawArticles || []).map((a) => ({
  slug: a.id,
  title: a.title,
  type: a.type,
  category: a.category,
  date: a.date,
  tags: a.tags || [],
  summary: a.summary,
  content: a.content || [],
  views: typeof a.views === "number" ? a.views : null,
  status: a.status || "published",
  updatedAt: a.updatedAt || a.date
}));

const about = { bio: (rawAbout && rawAbout.bio) || [] };

const seedDir = path.resolve("backend/src/main/resources/seed");
fs.mkdirSync(seedDir, { recursive: true });
fs.writeFileSync(path.join(seedDir, "articles.json"), JSON.stringify(articles, null, 2), "utf8");
fs.writeFileSync(path.join(seedDir, "about.json"), JSON.stringify(about, null, 2), "utf8");

console.log(`OK: articles=${articles.length} aboutParagraphs=${about.bio.length}`);
