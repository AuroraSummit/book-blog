package com.paperpages.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpages.entity.AdminUser;
import com.paperpages.entity.Article;
import com.paperpages.entity.Setting;
import com.paperpages.repository.AdminUserRepository;
import com.paperpages.repository.ArticleRepository;
import com.paperpages.repository.SettingRepository;
import com.paperpages.service.ArticleService;
import com.paperpages.service.SiteSettingsService;
import com.paperpages.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * 首次启动数据初始化：
 * - 无博主账号 → 按配置创建（BCrypt 加密）
 * - 无文章 → 从 seed/articles.json 导入示例文章
 * - 无关于文字 → 从 seed/about.json 导入
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final ArticleRepository articleRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JsonUtil jsonUtil;
    private final ObjectMapper objectMapper;

    @Value("${paper.admin.username}") private String adminUsername;
    @Value("${paper.admin.password}") private String adminPassword;

    public DataInitializer(AdminUserRepository adminUserRepository,
                           ArticleRepository articleRepository,
                           SettingRepository settingRepository,
                           PasswordEncoder passwordEncoder,
                           JsonUtil jsonUtil,
                           ObjectMapper objectMapper) {
        this.adminUserRepository = adminUserRepository;
        this.articleRepository = articleRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jsonUtil = jsonUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initAdmin();
        initArticles();
        initAbout();
    }

    private void initAdmin() {
        if (adminUserRepository.count() > 0) return;
        AdminUser user = new AdminUser();
        user.setUsername(adminUsername);
        user.setPassword(passwordEncoder.encode(adminPassword));
        adminUserRepository.save(user);
        log.info("[初始化] 已创建博主账号：{}（请尽快修改默认密码）", adminUsername);
    }

    private void initArticles() throws Exception {
        if (articleRepository.count() > 0) return;
        List<ArticleSeed> seeds = readJson("seed/articles.json", new TypeReference<>() {});
        for (ArticleSeed s : seeds) {
            Article a = new Article();
            a.setSlug(s.slug());
            a.setTitle(s.title());
            a.setType(s.type());
            a.setCategory(s.category());
            a.setDate(LocalDate.parse(s.date()));
            a.setTags(String.join(",", s.tags() == null ? List.of() : s.tags()));
            a.setSummary(s.summary());
            a.setContent(jsonUtil.writeList(s.content()));
            a.setViews(s.views() != null ? s.views() : ArticleService.defaultViews(s.slug()));
            a.setStatus(s.status() == null ? "published" : s.status());
            a.setUpdatedAt(s.updatedAt() == null ? LocalDate.parse(s.date()) : LocalDate.parse(s.updatedAt()));
            articleRepository.save(a);
        }
        log.info("[初始化] 已导入 {} 篇示例文章", seeds.size());
    }

    private void initAbout() throws Exception {
        if (settingRepository.existsById(SiteSettingsService.KEY_ABOUT)) return;
        AboutSeed about = readJson("seed/about.json", new TypeReference<>() {});
        Setting setting = new Setting();
        setting.setKey(SiteSettingsService.KEY_ABOUT);
        setting.setValue(jsonUtil.writeList(about.bio() == null ? List.of() : about.bio()));
        settingRepository.save(setting);
        log.info("[初始化] 已导入关于页文字");
    }

    private <T> T readJson(String path, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }

    /** 与 seed/articles.json 结构对应 */
    record ArticleSeed(String slug, String title, String type, String category, String date,
                       List<String> tags, String summary, List<String> content,
                       Integer views, String status, String updatedAt) {
    }

    record AboutSeed(List<String> bio) {
    }
}
