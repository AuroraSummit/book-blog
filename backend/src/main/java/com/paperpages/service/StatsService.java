package com.paperpages.service;

import com.paperpages.dto.ArticleListItem;
import com.paperpages.dto.StatsDto;
import com.paperpages.dto.TagCount;
import com.paperpages.repository.ArticleLikeRepository;
import com.paperpages.repository.ArticleRepository;
import com.paperpages.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ArticleLikeRepository likeRepository;
    private final ArticleService articleService;
    private final TagService tagService;

    public StatsService(ArticleRepository articleRepository,
                        CommentRepository commentRepository,
                        ArticleLikeRepository likeRepository,
                        ArticleService articleService,
                        TagService tagService) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.articleService = articleService;
        this.tagService = tagService;
    }

    public StatsDto stats() {
        long total = articleRepository.count();
        long books = articleRepository.countByType("reading");
        long movies = articleRepository.countByType("movie");
        long drafts = articleRepository.countByStatus("draft");
        long comments = commentRepository.count();
        long likes = likeRepository.count();

        List<ArticleListItem> recent = articleRepository.findAllByOrderByDateDesc().stream()
                .limit(5)
                .map(articleService::toListItem)
                .toList();

        return new StatsDto(
                total, books, movies, drafts, comments, likes,
                recent,
                visitorSeries(14),
                publishedByMonth(),
                mostViewed(),
                mostLiked(),
                tagService.publicTags());
    }

    /** 最受欢迎（按阅读数）：全部文章取前 5。 */
    private List<Map<String, Object>> mostViewed() {
        List<Map<String, Object>> out = new ArrayList<>();
        articleRepository.findAllByOrderByDateDesc().stream()
                .sorted(Comparator.comparingInt(a -> -a.getViews()))
                .limit(5)
                .forEach(a -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("slug", a.getSlug());
                    item.put("title", a.getTitle());
                    item.put("views", a.getViews());
                    out.add(item);
                });
        return out;
    }

    /** 点赞榜：取前 5。 */
    private List<Map<String, Object>> mostLiked() {
        List<Map<String, Object>> out = new ArrayList<>();
        likeRepository.topLiked().stream().limit(5).forEach(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("slug", row[0]);
            item.put("title", row[1]);
            item.put("likes", row[2]);
            out.add(item);
        });
        return out;
    }

    /** 访客趋势：近 N 天确定性伪随机（同一日期数值稳定，演示用）。 */
    private List<Map<String, Object>> visitorSeries(int days) {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long seed = d.getYear() * 10000L + (d.getMonthValue() * 100L) + d.getDayOfMonth();
            long count = 30 + Math.floorMod(seed * 2654435761L, 700L);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.getMonthValue() + "-" + d.getDayOfMonth());
            point.put("count", count);
            out.add(point);
        }
        return out;
    }

    /** 文章发表数量：按月份聚合。 */
    private List<Map<String, Object>> publishedByMonth() {
        Map<String, Integer> map = new LinkedHashMap<>();
        articleRepository.findAllByOrderByDateDesc().forEach(a -> {
            String m = String.valueOf(a.getDate()).substring(0, 7);
            map.merge(m, 1, Integer::sum);
        });
        List<Map<String, Object>> out = new ArrayList<>();
        map.forEach((month, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("count", count);
            out.add(item);
        });
        return out;
    }
}
