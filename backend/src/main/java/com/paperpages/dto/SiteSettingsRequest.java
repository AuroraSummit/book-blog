package com.paperpages.dto;

import java.util.List;

/** 更新站点设置请求；null 字段表示不修改。 */
public record SiteSettingsRequest(
        String name,
        String subtitle,
        String announcement,
        List<String> aboutBio) {
}
