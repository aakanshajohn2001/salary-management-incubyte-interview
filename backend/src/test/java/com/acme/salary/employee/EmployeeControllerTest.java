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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

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
    private Employee ada;

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

        ada = employeeRepository.save(new Employee("Ada", "Lovelace", "ada.lovelace@acme-corp.example",
                engineering, us, JobBand.L5, LocalDate.of(2019, 4, 1), EmployeeStatus.ACTIVE));
        employeeRepository.save(new Employee("Grace", "Hopper", "grace.hopper@acme-corp.example",
                sales, us, JobBand.L4, LocalDate.of(2020, 2, 15), EmployeeStatus.ACTIVE));

        salaryRecordRepository.save(new SalaryRecord(ada, new BigDecimal("150000.00"), usd,
                LocalDate.of(2019, 4, 1), "Initial hire"));
        salaryRecordRepository.save(new SalaryRecord(ada, new BigDecimal("165000.00"), usd,
                LocalDate.of(2022, 1, 1), "Annual raise"));

        token = login();
    }

    private String login() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void list_withoutSearch_returnsAllSeededEmployeesPaginated() throws Exception {
        mockMvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void list_filteredBySearch_returnsOnlyMatchingEmployee() throws Exception {
        mockMvc.perform(get("/api/employees").param("search", "lovelace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("ada.lovelace@acme-corp.example"))
                .andExpect(jsonPath("$.content[0].currentSalaryAmount").value(165000.00))
                .andExpect(jsonPath("$.content[0].currentSalaryCurrency").value("USD"));
    }

    @Test
    void list_filteredByDepartment_returnsOnlyThatDepartment() throws Exception {
        Long salesId = departmentRepository.findByName("Sales").orElseThrow().getId();

        mockMvc.perform(get("/api/employees").param("departmentId", salesId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("grace.hopper@acme-corp.example"));
    }

    @Test
    void getById_returnsProfileWithCurrentSalary() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", ada.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.currentSalaryAmount").value(165000.00));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", 999_999).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void salaryHistory_returnsRecordsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/salary-history", ada.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(165000.00))
                .andExpect(jsonPath("$[0].reason").value("Annual raise"))
                .andExpect(jsonPath("$[1].amount").value(150000.00));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void salaryHistory_unknownEmployee_returns404() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/salary-history", 999_999)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_filteredByCountryCode_returnsOnlyThatCountry() throws Exception {
        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country in = countryRepository.findById("IN").orElseThrow();
        employeeRepository.save(new Employee("Priya", "Rao", "priya.rao@acme-corp.example",
                engineering, in, JobBand.L3, LocalDate.of(2021, 6, 1), EmployeeStatus.ACTIVE));

        mockMvc.perform(get("/api/employees").param("countryCode", "IN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("priya.rao@acme-corp.example"));
    }

    @Test
    void list_filteredByJobBand_returnsOnlyThatBand() throws Exception {
        mockMvc.perform(get("/api/employees").param("jobBand", "L4").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("grace.hopper@acme-corp.example"));
    }

    @Test
    void list_filteredByStatus_returnsOnlyMatchingStatus() throws Exception {
        mockMvc.perform(get("/api/employees").param("status", "ACTIVE").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/employees").param("status", "TERMINATED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void list_withNoMatchingResults_returnsEmptyPageNotError() throws Exception {
        mockMvc.perform(get("/api/employees").param("search", "no-such-employee")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void list_withBlankSearchAndCountryFilters_isTreatedAsNoFilter() throws Exception {
        mockMvc.perform(get("/api/employees").param("search", "").param("countryCode", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void list_withInvalidJobBand_returns400() throws Exception {
        mockMvc.perform(get("/api/employees").param("jobBand", "NOT_A_BAND")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
