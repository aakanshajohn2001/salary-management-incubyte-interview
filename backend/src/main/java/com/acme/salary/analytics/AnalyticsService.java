package com.acme.salary.analytics;

import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
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
}
