package com.paperpages.controller;

import com.paperpages.dto.AdminReplyRequest;
import com.paperpages.dto.CommentDto;
import com.paperpages.dto.PageResult;
import com.paperpages.dto.AdminCommentDto;
import com.paperpages.dto.Result;
import com.paperpages.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 后台评论管理接口（需登录）：检索 / 通过 / 驳回 / 删除 / 博主回复。 */
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 评论分页检索：status=all|pending|approved|rejected，keyword 模糊匹配作者或内容。 */
    @GetMapping
    public Result<PageResult<AdminCommentDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(commentService.adminList(status, articleId, keyword, page, size));
    }

    /** 各状态评论数（后台入口角标）。 */
    @GetMapping("/counts")
    public Result<Map<String, Long>> counts() {
        return Result.ok(commentService.statusCounts());
    }

    /** 博主回复某条评论。 */
    @PostMapping
    public Result<CommentDto> reply(@Valid @RequestBody AdminReplyRequest request) {
        return Result.ok(commentService.authorReply(request.parentId(), request.content()));
    }

    /** 删除评论。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    /** 通过 / 驳回（status=pending|approved|rejected）。 */
    @PatchMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam String status) {
        commentService.setStatus(id, status);
        return Result.ok();
    }
}
