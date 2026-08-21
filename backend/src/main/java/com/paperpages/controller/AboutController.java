package com.paperpages.controller;

import com.paperpages.dto.AboutDto;
import com.paperpages.dto.Result;
import com.paperpages.service.SiteSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    private final SiteSettingsService siteSettingsService;

    public AboutController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    /** 关于页文字。 */
    @GetMapping
    public Result<AboutDto> about() {
        return Result.ok(siteSettingsService.about());
    }
}
