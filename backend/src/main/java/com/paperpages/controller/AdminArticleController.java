package com.paperpages.controller;

import com.paperpages.dto.ArticleDetail;
import com.paperpages.dto.ArticleListItem;
import com.paperpages.dto.ArticleRequest;
import com.paperpages.dto.Result;
import com.paperpages.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 后台文章管理接口（需登录）。 */
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 管理端文章列表（含草稿），可按状态过滤。 */
    @GetMapping
    public Result<List<ArticleListItem>> list(@RequestParam(required = false) String status) {
        return Result.ok(articleService.adminList(status));
    }

    /** 管理端单篇详情（含草稿，编辑回填用）。 */
    @GetMapping("/{slug}")
    public Result<ArticleDetail> detail(@PathVariable String slug) {
        return Result.ok(articleService.detailAdmin(slug));
    }

    /** 新建文章。 */
    @PostMapping
    public Result<ArticleDetail> create(@Valid @RequestBody ArticleRequest request) {
        return Result.ok(articleService.create(request));
    }

    /** 编辑文章。 */
    @PutMapping("/{slug}")
    public Result<ArticleDetail> update(@PathVariable String slug,
                                        @Valid @RequestBody ArticleRequest request) {
        return Result.ok(articleService.update(slug, request));
    }

    /** 删除文章（连带删除其评论）。 */
    @DeleteMapping("/{slug}")
    public Result<Void> delete(@PathVariable String slug) {
        articleService.delete(slug);
        return Result.ok();
    }

    /** 发布 / 转草稿。 */
    @PatchMapping("/{slug}/status")
    public Result<ArticleDetail> updateStatus(@PathVariable String slug,
                                              @RequestParam String status) {
        articleService.updateStatus(slug, status);
        return Result.ok(articleService.detailAdmin(slug));
    }
}
