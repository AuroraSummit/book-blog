package com.paperpages.service;

import com.paperpages.dto.TagCount;
import com.paperpages.entity.Article;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 标签聚合与治理。 */
@Service
public class TagService {

    private final ArticleRepository articleRepository;

    public TagService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /** 前台标签云：仅统计已发布文章。 */
    public List<TagCount> publicTags() {
        return aggregate(articleRepository.findByStatusOrderByDateDesc("published"));
    }

    /** 后台标签管理：统计全部文章（含草稿）。 */
    public List<TagCount> adminTags() {
        return aggregate(articleRepository.findAllByOrderByDateDesc());
    }

    /** 重命名标签：若新名称已存在则合并（原标签移除，不重复添加）。 */
    @Transactional
    public void rename(String oldName, String newName) {
        String oldTag = normalize(oldName);
        String newTag = normalize(newName);
        if (oldTag.isEmpty()) throw BusinessException.badRequest("标签名不能为空");
        if (newTag.isEmpty()) throw BusinessException.badRequest("新标签名不能为空");
        if (oldTag.equals(newTag)) return;

        for (Article a : articleRepository.findAllByOrderByDateDesc()) {
            List<String> tags = new ArrayList<>(split(a.getTags()));
            if (!tags.contains(oldTag)) continue;
            tags.remove(oldTag);
            if (!tags.contains(newTag)) tags.add(newTag);
            a.setTags(String.join(",", tags));
            articleRepository.save(a);
        }
    }

    /** 删除标签：从所有文章中移除。 */
    @Transactional
    public void delete(String name) {
        String tag = normalize(name);
        if (tag.isEmpty()) throw BusinessException.badRequest("标签名不能为空");
        for (Article a : articleRepository.findAllByOrderByDateDesc()) {
            List<String> tags = new ArrayList<>(split(a.getTags()));
            if (tags.remove(tag)) {
                a.setTags(String.join(",", tags));
                articleRepository.save(a);
            }
        }
    }

    private List<TagCount> aggregate(List<Article> articles) {
        Map<String, Integer> counts = new HashMap<>();
        for (Article a : articles) {
            for (String t : split(a.getTags())) {
                counts.merge(t, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new TagCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(TagCount::count).reversed()
                        .thenComparing(TagCount::name))
                .toList();
    }

    private List<String> split(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
