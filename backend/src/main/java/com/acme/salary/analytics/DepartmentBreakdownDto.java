package com.acme.salary.analytics;

import java.math.BigDecimal;

public record DepartmentBreakdownDto(
        String department,
        long headcount,
        BigDecimal totalPayrollUsd,
        BigDecimal averageSalaryUsd
) {
}
