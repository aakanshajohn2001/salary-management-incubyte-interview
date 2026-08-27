package com.acme.salary.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "app.seed.employee-count=25"
})
@ActiveProfiles("test")
class SeedRunnerTest {

    @Autowired
    private SeedRunner seedRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startupSeedsExactlyConfiguredEmployeeCountAndOneSalaryRecordEach() {
        Integer employeeCount = jdbcTemplate.queryForObject("select count(*) from employee", Integer.class);
        Integer salaryCount = jdbcTemplate.queryForObject("select count(*) from salary_record", Integer.class);
        Integer adminCount = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = 'hr.manager'", Integer.class);

        assertThat(employeeCount).isEqualTo(25);
        assertThat(salaryCount).isEqualTo(25);
        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    void runningTheSeederAgainDoesNotDuplicateData() {
        seedRunner.run(new DefaultApplicationArguments());

        Integer employeeCount = jdbcTemplate.queryForObject("select count(*) from employee", Integer.class);
        Integer adminCount = jdbcTemplate.queryForObject("select count(*) from app_user", Integer.class);

        assertThat(employeeCount).isEqualTo(25);
        assertThat(adminCount).isEqualTo(1);
    }
}
