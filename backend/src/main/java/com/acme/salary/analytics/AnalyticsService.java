package com.acme.salary.analytics;

import com.acme.salary.compensation.RecentSalaryChangeDto;
import com.acme.salary.compensation.SalaryRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    /** Hard cap on the "recent changes" feed so a careless ?limit= can't force an unbounded query. */
    private static final int MAX_RECENT_CHANGES = 100;

    private final AnalyticsRepository analyticsRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository, SalaryRecordRepository salaryRecordRepository) {
        this.analyticsRepository = analyticsRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    public AnalyticsSummaryDto getSummary() {
        AnalyticsRepository.OverallTotals overall = analyticsRepository.overall();
        return new AnalyticsSummaryDto(
                overall.headcount(),
                overall.totalPayrollUsd(),
                overall.averageSalaryUsd(),
                analyticsRepository.byDepartment(),
                analyticsRepository.byCountry(),
                analyticsRepository.byJobBand());
    }

    public List<RecentSalaryChangeDto> getRecentChanges(int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_RECENT_CHANGES);
        return salaryRecordRepository.findRecentAcrossOrg(PageRequest.of(0, clampedLimit)).stream()
                .map(RecentSalaryChangeDto::from)
                .toList();
    }
}
