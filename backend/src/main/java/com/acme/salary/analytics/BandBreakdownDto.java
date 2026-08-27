package com.acme.salary.analytics;

import java.math.BigDecimal;

public record BandBreakdownDto(
        String jobBand,
        long headcount,
        BigDecimal minSalaryUsd,
        BigDecimal maxSalaryUsd,
        BigDecimal averageSalaryUsd
) {
}
