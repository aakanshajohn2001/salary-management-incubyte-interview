package com.acme.salary.employee;

import com.acme.salary.auth.AppUser;
import com.acme.salary.auth.AppUserRepository;
import com.acme.salary.auth.Role;
import com.acme.salary.compensation.SalaryRecord;
import com.acme.salary.compensation.SalaryRecordRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeExportTest {

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
        Department sales = departmentRepository.findByName("Sales").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();
        Currency usd = currencyRepository.findById("USD").orElseThrow();

        var ada = employeeRepository.save(new Employee("Ada", "Lovelace", "ada@acme-corp.example",
                engineering, us, JobBand.L5, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(ada, new BigDecimal("150000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        employeeRepository.save(new Employee("Grace", "Hopper", "grace@acme-corp.example",
                sales, us, JobBand.L4, LocalDate.of(2020, 2, 1), EmployeeStatus.ACTIVE));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"Secret123!\"}"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        token = json.get("token").asText();
    }

    @Test
    void export_returnsCsvWithAllMatchingEmployees() throws Exception {
        mockMvc.perform(get("/api/employees/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"employees.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "First Name,Last Name,Email,Department,Country,Job Band,Hire Date,Status,Current Salary,Currency")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ada,Lovelace,ada@acme-corp.example")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("150000.00,USD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Grace,Hopper")));
    }

    @Test
    void export_respectsDepartmentFilter() throws Exception {
        Long salesId = departmentRepository.findByName("Sales").orElseThrow().getId();

        var response = mockMvc.perform(get("/api/employees/export")
                        .param("departmentId", salesId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response).contains("Grace,Hopper");
        org.assertj.core.api.Assertions.assertThat(response).doesNotContain("Ada,Lovelace");
    }

    @Test
    void export_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/employees/export")).andExpect(status().isUnauthorized());
    }
}
