package com.paperpages.controller;

import com.paperpages.dto.Result;
import com.paperpages.dto.TagCount;
import com.paperpages.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台标签云。 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<TagCount>> tags() {
        return Result.ok(tagService.publicTags());
    }
}
