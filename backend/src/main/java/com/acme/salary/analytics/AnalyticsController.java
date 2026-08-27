package com.acme.salary.analytics;

import com.acme.salary.compensation.RecentSalaryChangeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryDto summary() {
        return analyticsService.getSummary();
    }

    @GetMapping("/recent-changes")
    public List<RecentSalaryChangeDto> recentChanges(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getRecentChanges(limit);
    }
}
