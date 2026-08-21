package com.paperpages.controller;

import com.paperpages.dto.Result;
import com.paperpages.dto.SitePublicDto;
import com.paperpages.service.SiteSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前台站点公开信息（站名/副标题/公告）。 */
@RestController
@RequestMapping("/api/site")
public class SiteController {

    private final SiteSettingsService siteSettingsService;

    public SiteController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public Result<SitePublicDto> info() {
        return Result.ok(siteSettingsService.publicInfo());
    }
}
