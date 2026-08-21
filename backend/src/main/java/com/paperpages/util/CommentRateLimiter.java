package com.paperpages.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 评论限频器：同一 (IP + 文章) 两次评论间隔至少 30 秒。
 * 简单内存实现，个人博客规模足够；条目超过阈值时顺带清理过期记录。
 */
@Component
public class CommentRateLimiter {

    private static final long MIN_INTERVAL_MS = 30_000;
    private static final long MAX_AGE_MS = 10 * 60_000;
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Long> lastTime = new HashMap<>();

    public boolean allow(String key) {
        synchronized (lastTime) {
            if (lastTime.size() > MAX_ENTRIES) {
                long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
                lastTime.entrySet().removeIf(e -> e.getValue() < cutoff);
            }
            long now = System.currentTimeMillis();
            Long prev = lastTime.get(key);
            if (prev != null && now - prev < MIN_INTERVAL_MS) {
                return false;
            }
            lastTime.put(key, now);
            return true;
        }
    }
}
