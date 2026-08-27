package com.acme.salary.compensation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/{employeeId}/salary-adjustments")
public class SalaryAdjustmentController {

    private final SalaryAdjustmentService salaryAdjustmentService;

    public SalaryAdjustmentController(SalaryAdjustmentService salaryAdjustmentService) {
        this.salaryAdjustmentService = salaryAdjustmentService;
    }

    @PostMapping
    public ResponseEntity<SalaryHistoryEntryDto> create(@PathVariable Long employeeId,
                                                         @Valid @RequestBody SalaryAdjustmentRequest request) {
        SalaryHistoryEntryDto created = salaryAdjustmentService.adjustSalary(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
