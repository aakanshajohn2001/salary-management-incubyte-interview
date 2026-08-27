package com.acme.salary.analytics;

import com.acme.salary.auth.AppUser;
import com.acme.salary.auth.AppUserRepository;
import com.acme.salary.auth.Role;
import com.acme.salary.compensation.SalaryRecord;
import com.acme.salary.compensation.SalaryRecordRepository;
import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.JobBand;
import com.acme.salary.reference.Country;
import com.acme.salary.reference.CountryRepository;
import com.acme.salary.reference.Currency;
import com.acme.salary.reference.CurrencyRepository;
import com.acme.salary.reference.Department;
import com.acme.salary.reference.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private SalaryRecordRepository salaryRecordRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CurrencyRepository currencyRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        appUserRepository.deleteAll();
        appUserRepository.save(new AppUser("hr.manager", passwordEncoder.encode("Secret123!"), Role.HR_MANAGER));
        salaryRecordRepository.deleteAll();
        employeeRepository.deleteAll();

        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();
        Country in = countryRepository.findById("IN").orElseThrow();
        Currency usd = currencyRepository.findById("USD").orElseThrow();
        Currency inr = currencyRepository.findById("INR").orElseThrow();

        // US employee, L4, 100,000 USD -> 100,000 USD equivalent.
        Employee usEmployee = employeeRepository.save(new Employee("Ada", "Lovelace", "ada@acme-corp.example",
                engineering, us, JobBand.L4, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(usEmployee, new BigDecimal("100000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        // Indian employee, L4, 1,000,000 INR * fx 0.012 -> 12,000 USD equivalent.
        Employee inEmployee = employeeRepository.save(new Employee("Priya", "Rao", "priya@acme-corp.example",
                engineering, in, JobBand.L4, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(inEmployee, new BigDecimal("1000000.00"), inr,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        // Terminated employee -- must be excluded from every aggregate.
        Employee terminated = employeeRepository.save(new Employee("Bob", "Exit", "bob@acme-corp.example",
                engineering, us, JobBand.L6, LocalDate.of(2018, 1, 1), EmployeeStatus.TERMINATED));
        salaryRecordRepository.save(new SalaryRecord(terminated, new BigDecimal("500000.00"), usd,
                LocalDate.of(2018, 1, 1), "Initial hire"));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"Secret123!\"}"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        token = json.get("token").asText();
    }

    @Test
    void summary_excludesTerminatedAndNormalizesToUsd() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHeadcount").value(2))
                .andExpect(jsonPath("$.totalPayrollUsd").value(112000.00))
                .andExpect(jsonPath("$.averageSalaryUsd").value(56000.00));
    }

    @Test
    void summary_breaksDownByDepartment() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byDepartment.length()").value(1))
                .andExpect(jsonPath("$.byDepartment[0].department").value("Engineering"))
                .andExpect(jsonPath("$.byDepartment[0].headcount").value(2))
                .andExpect(jsonPath("$.byDepartment[0].totalPayrollUsd").value(112000.00));
    }

    @Test
    void summary_breaksDownByCountryWithFxConversion() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCountry.length()").value(2))
                .andExpect(jsonPath("$.byCountry[?(@.countryCode == 'US')].totalPayrollUsd").value(100000.00))
                .andExpect(jsonPath("$.byCountry[?(@.countryCode == 'IN')].totalPayrollUsd").value(12000.00));
    }

    @Test
    void summary_breaksDownByJobBandWithMinMax() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byJobBand.length()").value(1))
                .andExpect(jsonPath("$.byJobBand[0].jobBand").value("L4"))
                .andExpect(jsonPath("$.byJobBand[0].minSalaryUsd").value(12000.00))
                .andExpect(jsonPath("$.byJobBand[0].maxSalaryUsd").value(100000.00));
    }

    @Test
    void summary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }
}
