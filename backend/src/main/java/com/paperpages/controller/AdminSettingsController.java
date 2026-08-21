package com.paperpages.controller;

import com.paperpages.dto.Result;
import com.paperpages.dto.SiteAdminDto;
import com.paperpages.dto.SiteSettingsRequest;
import com.paperpages.service.SiteSettingsService;
import org.springframework.web.bind.annotation.*;

/** 后台站点设置（需登录）。 */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final SiteSettingsService siteSettingsService;

    public AdminSettingsController(SiteSettingsService siteSettingsService) {
        this.siteSettingsService = siteSettingsService;
    }

    /** 当前设置（回填用）。 */
    @GetMapping
    public Result<SiteAdminDto> get() {
        return Result.ok(siteSettingsService.adminInfo());
    }

    /** 更新设置（null 字段不修改）。 */
    @PutMapping
    public Result<SiteAdminDto> update(@RequestBody SiteSettingsRequest request) {
        siteSettingsService.update(request);
        return Result.ok(siteSettingsService.adminInfo());
    }
}
