package com.acme.salary.compensation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryHistoryEntryDto(
        Long id,
        BigDecimal amount,
        String currencyCode,
        LocalDate effectiveDate,
        String reason,
        Instant createdAt
) {
    public static SalaryHistoryEntryDto from(SalaryRecord record) {
        return new SalaryHistoryEntryDto(
                record.getId(),
                record.getAmount(),
                record.getCurrency().getCode(),
                record.getEffectiveDate(),
                record.getReason(),
                record.getCreatedAt());
    }
}
