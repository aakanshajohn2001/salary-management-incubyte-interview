package com.acme.salary.compensation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecentSalaryChangeDto(
        Long employeeId,
        String employeeName,
        String department,
        BigDecimal amount,
        String currencyCode,
        LocalDate effectiveDate,
        String reason,
        Instant createdAt
) {
    public static RecentSalaryChangeDto from(SalaryRecord record) {
        return new RecentSalaryChangeDto(
                record.getEmployee().getId(),
                record.getEmployee().getFirstName() + " " + record.getEmployee().getLastName(),
                record.getEmployee().getDepartment().getName(),
                record.getAmount(),
                record.getCurrency().getCode(),
                record.getEffectiveDate(),
                record.getReason(),
                record.getCreatedAt());
    }
}
