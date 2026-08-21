package com.paperpages.dto;

import java.util.List;

/** 站点管理端信息（含关于文字）。 */
public record SiteAdminDto(
        String name,
        String subtitle,
        String announcement,
        List<String> aboutBio) {
}
