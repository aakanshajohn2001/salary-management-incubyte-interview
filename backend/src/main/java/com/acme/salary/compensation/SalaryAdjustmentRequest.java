package com.acme.salary.compensation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryAdjustmentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate effectiveDate,
        @NotBlank @Size(max = 255) String reason
) {
}
