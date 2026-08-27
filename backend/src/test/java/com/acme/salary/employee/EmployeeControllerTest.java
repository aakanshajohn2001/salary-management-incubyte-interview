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

    @Test
    void list_flagsEmployeesPaidWellBelowTheirBandAverage() throws Exception {
        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();
        Currency usd = currencyRepository.findById("USD").orElseThrow();

        // Ada is already L5/165000 from setUp. Add two more L5s so the band
        // average (415000/3 = 138333.33, threshold 117583.33) puts the
        // underpaid employee below it and the well-paid one above it.
        var wellPaid = employeeRepository.save(new Employee("Well", "Paid", "well.paid@acme-corp.example",
                engineering, us, JobBand.L5, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(wellPaid, new BigDecimal("200000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        var underpaid = employeeRepository.save(new Employee("Under", "Paid", "under.paid@acme-corp.example",
                engineering, us, JobBand.L5, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(underpaid, new BigDecimal("50000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        mockMvc.perform(get("/api/employees").param("jobBand", "L5").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].email").value("ada.lovelace@acme-corp.example"))
                .andExpect(jsonPath("$.content[0].belowBandAverage").value(false))
                .andExpect(jsonPath("$.content[1].email").value("well.paid@acme-corp.example"))
                .andExpect(jsonPath("$.content[1].belowBandAverage").value(false))
                .andExpect(jsonPath("$.content[2].email").value("under.paid@acme-corp.example"))
                .andExpect(jsonPath("$.content[2].belowBandAverage").value(true));
    }

    @Test
    void list_sortedByCurrentSalaryAmountDescending_ordersHighestPaidFirst() throws Exception {
        Department engineering = departmentRepository.findByName("Engineering").orElseThrow();
        Country us = countryRepository.findById("US").orElseThrow();
        Currency usd = currencyRepository.findById("USD").orElseThrow();

        var lowest = employeeRepository.save(new Employee("Lowest", "Paid", "lowest.paid@acme-corp.example",
                engineering, us, JobBand.L1, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(lowest, new BigDecimal("40000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        var highest = employeeRepository.save(new Employee("Highest", "Paid", "highest.paid@acme-corp.example",
                engineering, us, JobBand.L6, LocalDate.of(2020, 1, 1), EmployeeStatus.ACTIVE));
        salaryRecordRepository.save(new SalaryRecord(highest, new BigDecimal("300000.00"), usd,
                LocalDate.of(2020, 1, 1), "Initial hire"));

        // Scoped to Engineering so Grace (Sales, no salary record) doesn't
        // introduce a null-salary row into the ordering being asserted on.
        Long engineeringId = engineering.getId();

        mockMvc.perform(get("/api/employees").param("departmentId", engineeringId.toString())
                        .param("sort", "currentSalaryAmount,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("highest.paid@acme-corp.example"))
                .andExpect(jsonPath("$.content[1].email").value("ada.lovelace@acme-corp.example"))
                .andExpect(jsonPath("$.content[2].email").value("lowest.paid@acme-corp.example"));

        mockMvc.perform(get("/api/employees").param("departmentId", engineeringId.toString())
                        .param("sort", "currentSalaryAmount,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("lowest.paid@acme-corp.example"))
                .andExpect(jsonPath("$.content[2].email").value("highest.paid@acme-corp.example"));
    }

    @Test
    void list_sortedByLastNameDescending_ordersAlphabeticallyReversed() throws Exception {
        mockMvc.perform(get("/api/employees").param("sort", "lastName,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ada.lovelace@acme-corp.example"))
                .andExpect(jsonPath("$.content[1].email").value("grace.hopper@acme-corp.example"));
    }

    @Test
    void list_sortedByDepartmentNameAscending_ordersByJoinedEntityProperty() throws Exception {
        mockMvc.perform(get("/api/employees").param("sort", "department.name,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ada.lovelace@acme-corp.example"))
                .andExpect(jsonPath("$.content[1].email").value("grace.hopper@acme-corp.example"));
    }
}
