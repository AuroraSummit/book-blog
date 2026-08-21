package com.paperpages.controller;

import com.paperpages.dto.Result;
import com.paperpages.dto.StatsDto;
import com.paperpages.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台仪表盘统计接口（需登录）。 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public Result<StatsDto> stats() {
        return Result.ok(statsService.stats());
    }
}
