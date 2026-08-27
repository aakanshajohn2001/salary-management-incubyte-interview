package com.acme.salary.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SeedRunnerTest uses a small employee-count (25) to stay fast, which never
 * exercises the mid-loop batch flush (every 500 rows). This uses a count
 * just over that threshold to confirm the flushed batch actually persists
 * every salary record, not just the final partial batch.
 */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "app.seed.employee-count=600"
})
@ActiveProfiles("test")
class SeedRunnerBatchFlushTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedingPastOneFullBatchPersistsEverySalaryRecord() {
        Integer employeeCount = jdbcTemplate.queryForObject("select count(*) from employee", Integer.class);
        Integer salaryCount = jdbcTemplate.queryForObject("select count(*) from salary_record", Integer.class);

        assertThat(employeeCount).isEqualTo(600);
        assertThat(salaryCount).isEqualTo(600);
    }
}
