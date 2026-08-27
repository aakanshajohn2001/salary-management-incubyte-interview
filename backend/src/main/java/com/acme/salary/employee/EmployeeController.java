package com.acme.salary.employee;

import com.acme.salary.common.PageResponse;
import com.acme.salary.compensation.SalaryHistoryEntryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public PageResponse<EmployeeDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) JobBand jobBand,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 25, sort = "id") Pageable pageable) {
        return employeeService.listEmployees(search, departmentId, countryCode, jobBand, status, pageable);
    }

    @GetMapping("/{id}")
    public EmployeeDto get(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }

    @GetMapping("/{id}/salary-history")
    public List<SalaryHistoryEntryDto> salaryHistory(@PathVariable Long id) {
        return employeeService.getSalaryHistory(id);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) JobBand jobBand,
            @RequestParam(required = false) EmployeeStatus status) {
        List<EmployeeDto> employees = employeeService.exportEmployees(search, departmentId, countryCode, jobBand, status);
        String csv = EmployeeCsvWriter.write(employees);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
