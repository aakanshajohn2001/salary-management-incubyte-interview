package com.acme.salary.compensation;

import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.reference.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Records salary changes as new, append-only rows -- see SalaryRecord. This
 * is the only write path into salary_record, so the ordering/status rules
 * enforced here are what keep the history (and therefore the analytics)
 * trustworthy.
 */
@Service
public class SalaryAdjustmentService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public SalaryAdjustmentService(EmployeeRepository employeeRepository,
                                    SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Transactional
    public SalaryHistoryEntryDto adjustSalary(Long employeeId, SalaryAdjustmentRequest request) {
        Employee employee = employeeRepository.findDetailById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee " + employeeId + " not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot adjust salary for a non-active employee");
        }
        if (request.effectiveDate().isBefore(employee.getHireDate())) {
            throw new IllegalArgumentException("Effective date cannot be before the employee's hire date");
        }

        List<SalaryRecord> history = salaryRecordRepository.findHistoryByEmployeeId(employeeId);
        if (!history.isEmpty() && !request.effectiveDate().isAfter(history.get(0).getEffectiveDate())) {
            throw new IllegalArgumentException(
                    "Effective date must be after the most recent salary record (%s)"
                            .formatted(history.get(0).getEffectiveDate()));
        }

        Currency currency = employee.getCountry().getCurrency();
        SalaryRecord record = new SalaryRecord(employee, request.amount(), currency,
                request.effectiveDate(), request.reason());
        SalaryRecord saved = salaryRecordRepository.save(record);
        return SalaryHistoryEntryDto.from(saved);
    }
}
