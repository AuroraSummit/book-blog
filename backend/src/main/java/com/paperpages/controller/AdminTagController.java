package com.paperpages.controller;

import com.paperpages.dto.Result;
import com.paperpages.dto.TagCount;
import com.paperpages.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 后台标签管理（需登录）。 */
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    /** 标签列表（含文章数，统计全部文章）。 */
    @GetMapping
    public Result<List<TagCount>> list() {
        return Result.ok(tagService.adminTags());
    }

    /** 重命名标签（新名已存在则合并）。body: { "newName": "..." } */
    @PutMapping("/{name}")
    public Result<Void> rename(@PathVariable String name, @RequestBody Map<String, String> body) {
        tagService.rename(name, body.get("newName"));
        return Result.ok();
    }

    /** 删除标签（从所有文章移除）。 */
    @DeleteMapping("/{name}")
    public Result<Void> delete(@PathVariable String name) {
        tagService.delete(name);
        return Result.ok();
    }
}
