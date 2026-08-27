package com.acme.salary.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * All amounts are normalized to USD via the fixed FX snapshot in the
 * currency table, so totals/averages are meaningfully comparable across
 * countries -- see docs/requirements.md for why a fixed snapshot (not a
 * live rate feed) is used. Only ACTIVE employees are counted.
 */
public record AnalyticsSummaryDto(
        long totalHeadcount,
        BigDecimal totalPayrollUsd,
        BigDecimal averageSalaryUsd,
        List<DepartmentBreakdownDto> byDepartment,
        List<CountryBreakdownDto> byCountry,
        List<BandBreakdownDto> byJobBand
) {
}
