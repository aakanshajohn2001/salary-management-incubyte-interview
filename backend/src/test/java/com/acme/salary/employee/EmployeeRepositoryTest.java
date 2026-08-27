package com.acme.salary.employee;

import com.acme.salary.reference.Country;
import com.acme.salary.reference.CountryRepository;
import com.acme.salary.reference.Department;
import com.acme.salary.reference.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Test
    void findAllWithDepartmentFilter_returnsOnlyMatchingEmployees() {
        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Department sales = departmentRepository.findByName("Sales").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();

        employeeRepository.save(new Employee("Ada", "Lovelace", "ada@acme.com", engineering, us,
                JobBand.L4, LocalDate.of(2020, 1, 15), EmployeeStatus.ACTIVE));
        employeeRepository.save(new Employee("Grace", "Hopper", "grace@acme.com", sales, us,
                JobBand.L5, LocalDate.of(2019, 3, 1), EmployeeStatus.ACTIVE));

        Specification<Employee> byEngineering = (root, query, cb) ->
                cb.equal(root.get("department").get("id"), engineering.getId());

        var page = employeeRepository.findAll(byEngineering,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("ada@acme.com");
    }

    @Test
    void findDetailById_fetchesDepartmentAndCountryEagerly() {
        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country in = countryRepository.findById("IN").orElseThrow();

        Employee saved = employeeRepository.save(new Employee("Priya", "Rao", "priya@acme.com",
                engineering, in, JobBand.L3, LocalDate.of(2021, 6, 1), EmployeeStatus.ACTIVE));

        Employee found = employeeRepository.findDetailById(saved.getId()).orElseThrow();

        assertThat(found.getDepartment().getName()).isEqualTo("Engineering");
        assertThat(found.getCountry().getName()).isEqualTo("India");
    }
}
