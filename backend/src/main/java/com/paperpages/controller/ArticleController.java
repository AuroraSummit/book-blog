package com.paperpages.controller;

import com.paperpages.dto.*;
import com.paperpages.service.ArticleService;
import com.paperpages.service.CommentService;
import com.paperpages.service.LikeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 前台公开接口：文章列表 / 详情 / 搜索 / 随机 / 相关 / 阅读计数 / 点赞 / 评论。 */
@RestController
@RequestMapping("/api")
public class ArticleController {

    private static final String VISITOR_COOKIE = "pp_visitor";

    private final ArticleService articleService;
    private final CommentService commentService;
    private final LikeService likeService;

    public ArticleController(ArticleService articleService,
                             CommentService commentService,
                             LikeService likeService) {
        this.articleService = articleService;
        this.commentService = commentService;
        this.likeService = likeService;
    }

    /** 文章列表（分页，可按类型 / 标签过滤）。 */
    @GetMapping("/articles")
    public Result<PageResult<ArticleListItem>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.ok(articleService.pagePublished(type, tag, page, size));
    }

    /** 站内搜索。 */
    @GetMapping("/articles/search")
    public Result<List<ArticleListItem>> search(@RequestParam String q) {
        return Result.ok(articleService.search(q));
    }

    /** 随机一篇文章（随便翻翻）。 */
    @GetMapping("/articles/random")
    public Result<Map<String, String>> random() {
        return Result.ok(articleService.random());
    }

    /** 相关推荐（按共享标签）。 */
    @GetMapping("/articles/{slug}/related")
    public Result<List<ArticleListItem>> related(@PathVariable String slug,
                                                 @RequestParam(defaultValue = "3") int limit) {
        return Result.ok(articleService.related(slug, limit));
    }

    /** 文章详情。 */
    @GetMapping("/articles/{slug}")
    public Result<ArticleDetail> detail(@PathVariable String slug) {
        return Result.ok(articleService.detail(slug));
    }

    /** 记录一次阅读（前台打开详情页时调用）。 */
    @PostMapping("/articles/{slug}/views")
    public Result<Void> views(@PathVariable String slug) {
        articleService.incrementViews(slug);
        return Result.ok();
    }

    /** 点赞（幂等：同一访客只能赞一次）。 */
    @PostMapping("/articles/{slug}/like")
    public Result<LikeResponse> like(@PathVariable String slug,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        return Result.ok(likeService.like(slug, visitorKey(request, response)));
    }

    /** 查询当前访客是否已点赞及点赞数。 */
    @GetMapping("/articles/{slug}/liked")
    public Result<LikeResponse> liked(@PathVariable String slug,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        return Result.ok(likeService.liked(slug, visitorKey(request, response)));
    }

    /** 文章评论列表（即时发布，全部可见）。 */
    @GetMapping("/articles/{slug}/comments")
    public Result<List<CommentDto>> comments(@PathVariable String slug) {
        return Result.ok(commentService.listByArticle(slug));
    }

    /** 发表评论（ip 用于限频）。 */
    @PostMapping("/articles/{slug}/comments")
    public Result<CommentDto> addComment(@PathVariable String slug,
                                         @Valid @RequestBody CommentRequest request,
                                         HttpServletRequest httpRequest) {
        return Result.ok(commentService.add(slug, request, clientIp(httpRequest)));
    }

    /* ---------- 访客标识：HttpOnly Cookie pp_visitor ---------- */

    private String visitorKey(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (VISITOR_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        String key = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(VISITOR_COOKIE, key);
        cookie.setMaxAge(365 * 24 * 3600);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return key;
    }

    /**
     * 客户端真实 IP（用于评论限频）。
     * 不信任 X-Forwarded-For（客户端可伪造最左值从而绕过限频）；只信任反向代理注入的
     * X-Real-IP；直连（开发/本机）时取 remoteAddr。
     */
    private String clientIp(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
