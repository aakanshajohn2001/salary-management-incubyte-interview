package com.acme.salary.analytics;

import java.math.BigDecimal;

public record CountryBreakdownDto(
        String countryCode,
        String countryName,
        long headcount,
        BigDecimal totalPayrollUsd,
        BigDecimal averageSalaryUsd
) {
}
