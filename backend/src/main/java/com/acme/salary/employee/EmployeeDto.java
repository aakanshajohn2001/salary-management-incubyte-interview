package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String department,
        String countryCode,
        String countryName,
        String jobBand,
        LocalDate hireDate,
        String status,
        BigDecimal currentSalaryAmount,
        String currentSalaryCurrency,
        LocalDate currentSalaryEffectiveDate,
        Boolean belowBandAverage
) {
}
