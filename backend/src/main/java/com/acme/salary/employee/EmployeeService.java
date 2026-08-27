package com.acme.salary.employee;

import com.acme.salary.compensation.SalaryHistoryEntryDto;
import com.acme.salary.compensation.SalaryRecord;
import com.acme.salary.compensation.SalaryRecordRepository;
import com.acme.salary.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmployeeService {

    /**
     * Column id the frontend sorts the directory table's "Current salary"
     * column by. Not a real Employee property (salary lives in the
     * append-only salary_record table), so it's intercepted before reaching
     * the JPA Specification/Pageable path -- see listEmployees.
     */
    static final String CURRENT_SALARY_SORT_PROPERTY = "currentSalaryAmount";

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public EmployeeService(EmployeeRepository employeeRepository, SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    public PageResponse<EmployeeDto> listEmployees(String search, Long departmentId, String countryCode,
                                                    JobBand jobBand, EmployeeStatus status, Pageable pageable) {
        Specification<Employee> spec = buildSpecification(search, departmentId, countryCode, jobBand, status);

        Sort.Order salarySort = pageable.getSort().getOrderFor(CURRENT_SALARY_SORT_PROPERTY);
        Pageable effectivePageable = pageable;
        if (salarySort != null) {
            spec = spec.and(EmployeeSpecifications.orderByCurrentSalaryUsd(salarySort.getDirection()));
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        Page<Employee> page = employeeRepository.findAll(spec, effectivePageable);

        Map<Long, SalaryRecord> currentSalaryByEmployeeId = currentSalaryByEmployeeId(page.getContent());
        Map<JobBand, BigDecimal> bandAverageUsd = bandAverageUsd();
        Page<EmployeeDto> dtoPage = page.map(e -> EmployeeMapper.toDto(
                e, currentSalaryByEmployeeId.get(e.getId()), bandAverageUsd.get(e.getJobBand())));
        return PageResponse.from(dtoPage);
    }

    /**
     * Same filters as the directory listing, but unpaginated -- backs the CSV
     * export, which needs every matching row rather than one page of it.
     */
    public List<EmployeeDto> exportEmployees(String search, Long departmentId, String countryCode,
                                              JobBand jobBand, EmployeeStatus status) {
        Specification<Employee> spec = buildSpecification(search, departmentId, countryCode, jobBand, status);
        List<Employee> employees = employeeRepository.findAll(spec, Sort.by("id"));

        Map<Long, SalaryRecord> currentSalaryByEmployeeId = currentSalaryByEmployeeId(employees);
        Map<JobBand, BigDecimal> bandAverageUsd = bandAverageUsd();
        return employees.stream()
                .map(e -> EmployeeMapper.toDto(e, currentSalaryByEmployeeId.get(e.getId()), bandAverageUsd.get(e.getJobBand())))
                .toList();
    }

    private Map<JobBand, BigDecimal> bandAverageUsd() {
        return salaryRecordRepository.averageCurrentSalaryUsdByJobBand().stream()
                .collect(Collectors.toMap(SalaryRecordRepository.JobBandAverage::getJobBand,
                        SalaryRecordRepository.JobBandAverage::getAverageUsd));
    }

    private Specification<Employee> buildSpecification(String search, Long departmentId, String countryCode,
                                                         JobBand jobBand, EmployeeStatus status) {
        List<Specification<Employee>> filters = Stream.of(
                        EmployeeSpecifications.search(search),
                        EmployeeSpecifications.departmentId(departmentId),
                        EmployeeSpecifications.countryCode(countryCode),
                        EmployeeSpecifications.jobBand(jobBand),
                        EmployeeSpecifications.status(status))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        return Specification.allOf(filters);
    }

    private Map<Long, SalaryRecord> currentSalaryByEmployeeId(List<Employee> employees) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        return employeeIds.isEmpty()
                ? Map.of()
                : salaryRecordRepository.findCurrentForEmployees(employeeIds).stream()
                        .collect(Collectors.toMap(sr -> sr.getEmployee().getId(), Function.identity()));
    }

    public EmployeeDto getEmployee(Long id) {
        Employee employee = employeeRepository.findDetailById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee " + id + " not found"));
        List<SalaryRecord> history = salaryRecordRepository.findHistoryByEmployeeId(id);
        SalaryRecord current = history.isEmpty() ? null : history.get(0);
        BigDecimal bandAverageUsd = bandAverageUsd().get(employee.getJobBand());
        return EmployeeMapper.toDto(employee, current, bandAverageUsd);
    }

    public List<SalaryHistoryEntryDto> getSalaryHistory(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new NoSuchElementException("Employee " + id + " not found");
        }
        return salaryRecordRepository.findHistoryByEmployeeId(id).stream()
                .map(SalaryHistoryEntryDto::from)
                .toList();
    }
}
