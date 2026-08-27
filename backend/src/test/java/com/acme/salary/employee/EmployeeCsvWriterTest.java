package com.acme.salary.employee;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeCsvWriterTest {

    @Test
    void write_rendersHeaderAndOneRowPerEmployee() {
        EmployeeDto ada = new EmployeeDto(1L, "Ada", "Lovelace", "ada@acme.example", "Engineering",
                "US", "United States", "L5", LocalDate.of(2020, 1, 1), "ACTIVE",
                new BigDecimal("150000.00"), "USD", LocalDate.of(2020, 1, 1));

        String csv = EmployeeCsvWriter.write(List.of(ada));

        assertThat(csv).startsWith(
                "First Name,Last Name,Email,Department,Country,Job Band,Hire Date,Status,Current Salary,Currency\n");
        assertThat(csv).contains("Ada,Lovelace,ada@acme.example,Engineering,United States,L5,2020-01-01,ACTIVE,150000.00,USD");
    }

    @Test
    void write_quotesFieldsContainingCommasOrQuotes() {
        EmployeeDto withComma = new EmployeeDto(2L, "Bob", "O, Brian", "bob@acme.example", "Sales, EMEA",
                "GB", "United Kingdom", "L2", LocalDate.of(2021, 1, 1), "ACTIVE",
                new BigDecimal("60000.00"), "GBP", LocalDate.of(2021, 1, 1));

        String csv = EmployeeCsvWriter.write(List.of(withComma));

        assertThat(csv).contains("\"O, Brian\"");
        assertThat(csv).contains("\"Sales, EMEA\"");
    }

    @Test
    void write_rendersEmptyColumnsWhenNoSalaryRecordExists() {
        EmployeeDto noSalary = new EmployeeDto(3L, "Grace", "Hopper", "grace@acme.example", "Sales",
                "US", "United States", "L4", LocalDate.of(2020, 1, 1), "ACTIVE", null, null, null);

        String csv = EmployeeCsvWriter.write(List.of(noSalary));

        assertThat(csv).contains("Grace,Hopper,grace@acme.example,Sales,United States,L4,2020-01-01,ACTIVE,,\n");
    }

    @Test
    void write_withNoEmployees_rendersHeaderOnly() {
        String csv = EmployeeCsvWriter.write(List.of());

        assertThat(csv).isEqualTo(
                "First Name,Last Name,Email,Department,Country,Job Band,Hire Date,Status,Current Salary,Currency\n");
    }
}
