package com.acme.salary.employee;

import com.acme.salary.compensation.SalaryRecord;

import java.math.BigDecimal;

public final class EmployeeMapper {

    /**
     * An employee earning less than this fraction of their job band's
     * org-wide average (USD-normalized) is flagged as a pay-equity outlier.
     * A common, simple heuristic for a first-pass equity review -- not a
     * legal/compliance threshold.
     */
    private static final BigDecimal EQUITY_THRESHOLD_RATIO = new BigDecimal("0.85");

    private EmployeeMapper() {
    }

    public static EmployeeDto toDto(Employee employee, SalaryRecord currentSalary, BigDecimal bandAverageUsd) {
        return new EmployeeDto(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment().getName(),
                employee.getCountry().getCode(),
                employee.getCountry().getName(),
                employee.getJobBand().name(),
                employee.getHireDate(),
                employee.getStatus().name(),
                currentSalary != null ? currentSalary.getAmount() : null,
                currentSalary != null ? currentSalary.getCurrency().getCode() : null,
                currentSalary != null ? currentSalary.getEffectiveDate() : null,
                belowBandAverage(currentSalary, bandAverageUsd));
    }

    private static Boolean belowBandAverage(SalaryRecord currentSalary, BigDecimal bandAverageUsd) {
        if (currentSalary == null || bandAverageUsd == null) {
            return null;
        }
        BigDecimal usdAmount = currentSalary.getAmount().multiply(currentSalary.getCurrency().getFxToUsd());
        BigDecimal threshold = bandAverageUsd.multiply(EQUITY_THRESHOLD_RATIO);
        return usdAmount.compareTo(threshold) < 0;
    }
}
