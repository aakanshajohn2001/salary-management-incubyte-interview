package com.acme.salary.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Read-only aggregation queries answering "how does the org pay people".
 * Uses plain JdbcTemplate + hand-written SQL (window function to pick each
 * employee's current salary, then GROUP BY) rather than pulling 10k rows
 * into the JVM and aggregating in Java -- this has to stay fast regardless
 * of headcount. ANSI-standard SQL (ROW_NUMBER() OVER, CURRENT_DATE) so it
 * runs unchanged on H2 and PostgreSQL.
 */
@Repository
public class AnalyticsRepository {

    private static final String CURRENT_SALARY_CTE = """
            with latest_salary as (
                select sr.employee_id, sr.amount, sr.currency_code,
                       row_number() over (partition by sr.employee_id
                                           order by sr.effective_date desc, sr.id desc) as rn
                from salary_record sr
                where sr.effective_date <= current_date
            ),
            current_salary as (
                select employee_id, amount, currency_code from latest_salary where rn = 1
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OverallTotals overall() {
        String sql = CURRENT_SALARY_CTE + """
                select count(*) as headcount,
                       sum(cs.amount * c.fx_to_usd) as total_payroll_usd,
                       avg(cs.amount * c.fx_to_usd) as avg_salary_usd
                from employee e
                join current_salary cs on cs.employee_id = e.id
                join currency c on c.code = cs.currency_code
                where e.status = 'ACTIVE'
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new OverallTotals(
                rs.getLong("headcount"),
                money(rs.getBigDecimal("total_payroll_usd")),
                money(rs.getBigDecimal("avg_salary_usd"))));
    }

    public List<DepartmentBreakdownDto> byDepartment() {
        String sql = CURRENT_SALARY_CTE + """
                select d.name as department,
                       count(*) as headcount,
                       sum(cs.amount * c.fx_to_usd) as total_payroll_usd,
                       avg(cs.amount * c.fx_to_usd) as avg_salary_usd
                from employee e
                join current_salary cs on cs.employee_id = e.id
                join currency c on c.code = cs.currency_code
                join department d on d.id = e.department_id
                where e.status = 'ACTIVE'
                group by d.name
                order by d.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DepartmentBreakdownDto(
                rs.getString("department"),
                rs.getLong("headcount"),
                money(rs.getBigDecimal("total_payroll_usd")),
                money(rs.getBigDecimal("avg_salary_usd"))));
    }

    public List<CountryBreakdownDto> byCountry() {
        String sql = CURRENT_SALARY_CTE + """
                select co.code as country_code, co.name as country_name,
                       count(*) as headcount,
                       sum(cs.amount * c.fx_to_usd) as total_payroll_usd,
                       avg(cs.amount * c.fx_to_usd) as avg_salary_usd
                from employee e
                join current_salary cs on cs.employee_id = e.id
                join currency c on c.code = cs.currency_code
                join country co on co.code = e.country_code
                where e.status = 'ACTIVE'
                group by co.code, co.name
                order by headcount desc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CountryBreakdownDto(
                rs.getString("country_code"),
                rs.getString("country_name"),
                rs.getLong("headcount"),
                money(rs.getBigDecimal("total_payroll_usd")),
                money(rs.getBigDecimal("avg_salary_usd"))));
    }

    public List<BandBreakdownDto> byJobBand() {
        String sql = CURRENT_SALARY_CTE + """
                select e.job_band as job_band,
                       count(*) as headcount,
                       min(cs.amount * c.fx_to_usd) as min_salary_usd,
                       max(cs.amount * c.fx_to_usd) as max_salary_usd,
                       avg(cs.amount * c.fx_to_usd) as avg_salary_usd
                from employee e
                join current_salary cs on cs.employee_id = e.id
                join currency c on c.code = cs.currency_code
                where e.status = 'ACTIVE'
                group by e.job_band
                order by e.job_band
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new BandBreakdownDto(
                rs.getString("job_band"),
                rs.getLong("headcount"),
                money(rs.getBigDecimal("min_salary_usd")),
                money(rs.getBigDecimal("max_salary_usd")),
                money(rs.getBigDecimal("avg_salary_usd"))));
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public record OverallTotals(long headcount, BigDecimal totalPayrollUsd, BigDecimal averageSalaryUsd) {
    }
}
