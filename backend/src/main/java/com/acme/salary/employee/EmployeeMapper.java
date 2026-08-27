package com.acme.salary.employee;

import com.acme.salary.compensation.SalaryRecord;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeDto toDto(Employee employee, SalaryRecord currentSalary) {
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
                currentSalary != null ? currentSalary.getEffectiveDate() : null);
    }
}
