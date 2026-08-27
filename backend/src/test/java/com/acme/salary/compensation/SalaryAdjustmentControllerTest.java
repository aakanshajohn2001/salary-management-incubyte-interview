package com.acme.salary.compensation;

import com.acme.salary.auth.AppUser;
import com.acme.salary.auth.AppUserRepository;
import com.acme.salary.auth.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalaryAdjustmentControllerTest {

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
    private Employee activeEmployee;
    private Employee terminatedEmployee;

    @BeforeEach
    void setUp() throws Exception {
        appUserRepository.deleteAll();
        appUserRepository.save(new AppUser("hr.manager", passwordEncoder.encode("Secret123!"), Role.HR_MANAGER));
        salaryRecordRepository.deleteAll();
        employeeRepository.deleteAll();

        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();
        Currency usd = currencyRepository.findById("USD").orElseThrow();

        activeEmployee = employeeRepository.save(new Employee("Ada", "Lovelace", "ada@acme-corp.example",
                engineering, us, JobBand.L5, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(activeEmployee, new BigDecimal("150000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        terminatedEmployee = employeeRepository.save(new Employee("Bob", "Exit", "bob@acme-corp.example",
                engineering, us, JobBand.L3, LocalDate.of(2018, 1, 1), EmployeeStatus.TERMINATED));
        salaryRecordRepository.save(new SalaryRecord(terminatedEmployee, new BigDecimal("90000.00"), usd,
                LocalDate.of(2018, 1, 1), "Initial hire"));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"Secret123!\"}"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        token = json.get("token").asText();
    }

    @Test
    void adjustSalary_withValidRequest_appendsNewRecordInCountryCurrency() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", activeEmployee.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 165000.00, "effectiveDate": "2022-01-01", "reason": "Annual raise"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(165000.00))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.reason").value("Annual raise"));

        var history = salaryRecordRepository.findHistoryByEmployeeId(activeEmployee.getId());
        org.assertj.core.api.Assertions.assertThat(history).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(history.get(0).getAmount()).isEqualByComparingTo("165000.00");
    }

    @Test
    void adjustSalary_effectiveDateNotAfterLastRecord_returns400() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", activeEmployee.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 165000.00, "effectiveDate": "2020-01-01", "reason": "Backdated raise"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustSalary_effectiveDateBeforeHireDate_returns400() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", activeEmployee.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 165000.00, "effectiveDate": "2019-01-01", "reason": "Too early"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustSalary_forTerminatedEmployee_returns400() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", terminatedEmployee.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 95000.00, "effectiveDate": "2026-01-01", "reason": "Should not apply"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustSalary_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", activeEmployee.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": -5, "effectiveDate": "2022-01-01", "reason": "Invalid"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustSalary_unknownEmployee_returns404() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", 999_999)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100000.00, "effectiveDate": "2026-01-01", "reason": "Raise"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void adjustSalary_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/employees/{id}/salary-adjustments", activeEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100000.00, "effectiveDate": "2026-01-01", "reason": "Raise"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
