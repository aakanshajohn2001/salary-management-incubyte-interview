package com.acme.salary.employee;

import java.util.List;

/**
 * Minimal CSV rendering for the employee export -- no external dependency
 * needed for a handful of plain scalar columns.
 */
final class EmployeeCsvWriter {

    private static final String HEADER = "First Name,Last Name,Email,Department,Country,Job Band,"
            + "Hire Date,Status,Current Salary,Currency\n";

    private EmployeeCsvWriter() {
    }

    static String write(List<EmployeeDto> employees) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (EmployeeDto employee : employees) {
            csv.append(escape(employee.firstName())).append(',')
                    .append(escape(employee.lastName())).append(',')
                    .append(escape(employee.email())).append(',')
                    .append(escape(employee.department())).append(',')
                    .append(escape(employee.countryName())).append(',')
                    .append(escape(employee.jobBand())).append(',')
                    .append(escape(String.valueOf(employee.hireDate()))).append(',')
                    .append(escape(employee.status())).append(',')
                    .append(employee.currentSalaryAmount() == null ? "" : employee.currentSalaryAmount()).append(',')
                    .append(escape(employee.currentSalaryCurrency())).append('\n');
        }
        return csv.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
