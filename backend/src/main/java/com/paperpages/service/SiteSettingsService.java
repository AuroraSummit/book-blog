package com.paperpages.service;

import com.paperpages.dto.AboutDto;
import com.paperpages.dto.SiteAdminDto;
import com.paperpages.dto.SitePublicDto;
import com.paperpages.dto.SiteSettingsRequest;
import com.paperpages.entity.Setting;
import com.paperpages.repository.SettingRepository;
import com.paperpages.util.JsonUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 站点设置（站名/副标题/公告/关于文字），存储于 site_settings 表。 */
@Service
public class SiteSettingsService {

    public static final String KEY_NAME = "site_name";
    public static final String KEY_SUBTITLE = "site_subtitle";
    public static final String KEY_ANNOUNCEMENT = "site_announcement";
    public static final String KEY_ABOUT = "about_bio";

    private final SettingRepository settingRepository;
    private final JsonUtil jsonUtil;

    public SiteSettingsService(SettingRepository settingRepository, JsonUtil jsonUtil) {
        this.settingRepository = settingRepository;
        this.jsonUtil = jsonUtil;
    }

    /** 前台公开信息（页眉站名/副标题、公告栏）。 */
    public SitePublicDto publicInfo() {
        return new SitePublicDto(
                get(KEY_NAME, "纸页之间"),
                get(KEY_SUBTITLE, "读书笔记 · 观影感受 · 安静记录"),
                get(KEY_ANNOUNCEMENT, ""));
    }

    /** 管理端信息（含关于文字，用于设置页回填）。 */
    public SiteAdminDto adminInfo() {
        return new SiteAdminDto(
                get(KEY_NAME, "纸页之间"),
                get(KEY_SUBTITLE, "读书笔记 · 观影感受 · 安静记录"),
                get(KEY_ANNOUNCEMENT, ""),
                getList(KEY_ABOUT));
    }

    /** 关于页文字。 */
    public AboutDto about() {
        return new AboutDto(getList(KEY_ABOUT));
    }

    /** 更新设置：null 字段表示不修改。 */
    @Transactional
    public void update(SiteSettingsRequest req) {
        if (req.name() != null) set(KEY_NAME, req.name().trim());
        if (req.subtitle() != null) set(KEY_SUBTITLE, req.subtitle().trim());
        if (req.announcement() != null) set(KEY_ANNOUNCEMENT, req.announcement().trim());
        if (req.aboutBio() != null) set(KEY_ABOUT, jsonUtil.writeList(req.aboutBio()));
    }

    private String get(String key, String def) {
        return settingRepository.findById(key)
                .map(Setting::getValue)
                .filter(v -> !v.isBlank())
                .orElse(def);
    }

    private List<String> getList(String key) {
        return settingRepository.findById(key)
                .map(s -> jsonUtil.readList(s.getValue()))
                .orElse(List.of());
    }

    private void set(String key, String value) {
        Setting s = settingRepository.findById(key).orElseGet(Setting::new);
        s.setKey(key);
        s.setValue(value == null ? "" : value);
        settingRepository.save(s);
    }
}
