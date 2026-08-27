package com.acme.salary.seed;

import com.acme.salary.auth.AdminProperties;
import com.acme.salary.auth.Role;
import com.acme.salary.employee.EmployeeStatus;
import com.acme.salary.employee.JobBand;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates the 10,000-employee dataset the assessment asks for. Runs once,
 * only when {@code app.seed.enabled=true} (SEED_ON_STARTUP env var), and is
 * idempotent -- it skips employee generation if the employee table already
 * has rows, so restarting the app with seeding still enabled never
 * duplicates data.
 *
 * A fixed random seed makes the generated dataset reproducible across runs
 * and environments, which is useful for demos and bug reports ("employee
 * #4213 in the seed always looks the same").
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
    private static final long RANDOM_SEED = 42L;
    private static final int SALARY_BATCH_SIZE = 500;
    private static final String EMAIL_DOMAIN = "acme-corp.example";

    // Rough share of a global headcount, not real ACME data.
    private static final Map<String, Integer> COUNTRY_WEIGHTS = new LinkedHashMap<>();
    static {
        COUNTRY_WEIGHTS.put("US", 30);
        COUNTRY_WEIGHTS.put("IN", 20);
        COUNTRY_WEIGHTS.put("GB", 15);
        COUNTRY_WEIGHTS.put("DE", 10);
        COUNTRY_WEIGHTS.put("FR", 8);
        COUNTRY_WEIGHTS.put("SG", 7);
        COUNTRY_WEIGHTS.put("AU", 5);
        COUNTRY_WEIGHTS.put("BR", 5);
    }

    // Rough seniority shape of an org: mostly mid-level, few executives.
    private static final Map<JobBand, Integer> BAND_WEIGHTS = new LinkedHashMap<>();
    static {
        BAND_WEIGHTS.put(JobBand.L1, 15);
        BAND_WEIGHTS.put(JobBand.L2, 30);
        BAND_WEIGHTS.put(JobBand.L3, 25);
        BAND_WEIGHTS.put(JobBand.L4, 18);
        BAND_WEIGHTS.put(JobBand.L5, 9);
        BAND_WEIGHTS.put(JobBand.L6, 3);
    }

    // Annual base salary range in USD used to *generate* plausible numbers per band.
    private static final Map<JobBand, int[]> BAND_USD_RANGE = Map.of(
            JobBand.L1, new int[]{40_000, 60_000},
            JobBand.L2, new int[]{60_000, 85_000},
            JobBand.L3, new int[]{85_000, 120_000},
            JobBand.L4, new int[]{120_000, 160_000},
            JobBand.L5, new int[]{160_000, 220_000},
            JobBand.L6, new int[]{220_000, 320_000});

    private static final String INSERT_EMPLOYEE_SQL = """
            insert into employee
                (first_name, last_name, email, department_id, country_code, job_band, hire_date, status, created_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_SALARY_SQL = """
            insert into salary_record (employee_id, amount, currency_code, effective_date, reason, created_at)
            values (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;
    private final AdminProperties adminProperties;
    private final TransactionTemplate transactionTemplate;

    public SeedRunner(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                       SeedProperties seedProperties, AdminProperties adminProperties,
                       PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
        this.adminProperties = adminProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
        seedEmployees();
    }

    private void seedAdminUser() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = ?", Integer.class, adminProperties.username());
        if (count != null && count > 0) {
            log.info("Admin user '{}' already exists, skipping.", adminProperties.username());
            return;
        }
        jdbcTemplate.update(
                "insert into app_user (username, password_hash, role, created_at) values (?, ?, ?, ?)",
                adminProperties.username(),
                passwordEncoder.encode(adminProperties.password()),
                Role.HR_MANAGER.name(),
                Timestamp.from(Instant.now()));
        log.info("Created HR manager login '{}'.", adminProperties.username());
    }

    private void seedEmployees() {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from employee", Integer.class);
        if (existing != null && existing > 0) {
            log.info("{} employees already present, skipping employee seed.", existing);
            return;
        }

        long start = System.currentTimeMillis();
        int total = transactionTemplate.execute(status -> seedEmployeesInTransaction());

        log.info("Seeded {} employees with initial salary records in {} ms", total,
                System.currentTimeMillis() - start);
    }

    /**
     * Runs as a single transaction so a failure partway through (e.g. a
     * driver-specific bug hit only against the real production database)
     * leaves either zero employees or the full set -- never a partial
     * count that would fool the "already seeded" check above into
     * skipping the rest of the seed on the next startup.
     */
    private int seedEmployeesInTransaction() {
        int total = seedProperties.employeeCount();

        List<Long> departmentIds = jdbcTemplate.queryForList("select id from department", Long.class);
        Map<String, String> currencyByCountry = queryCurrencyByCountry();
        Map<String, BigDecimal> fxByCurrency = queryFxByCurrency();

        List<String> weightedCountries = expandWeights(COUNTRY_WEIGHTS);
        List<JobBand> weightedBands = expandWeights(BAND_WEIGHTS);

        Faker faker = new Faker(new Random(RANDOM_SEED));
        Random random = new Random(RANDOM_SEED);

        List<Object[]> salaryBatch = new ArrayList<>(SALARY_BATCH_SIZE);
        LocalDate today = LocalDate.now();
        LocalDate earliestHire = today.minusYears(8);
        long hireRangeDays = ChronoUnit.DAYS.between(earliestHire, today);

        for (int i = 0; i < total; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = "%s.%s.%d@%s".formatted(
                    firstName.toLowerCase(), lastName.toLowerCase(), i, EMAIL_DOMAIN);

            long departmentId = departmentIds.get(random.nextInt(departmentIds.size()));
            String countryCode = weightedCountries.get(random.nextInt(weightedCountries.size()));
            JobBand band = weightedBands.get(random.nextInt(weightedBands.size()));
            LocalDate hireDate = earliestHire.plusDays(random.nextLong(hireRangeDays + 1));
            EmployeeStatus status = random.nextInt(100) < 5 ? EmployeeStatus.TERMINATED : EmployeeStatus.ACTIVE;

            long employeeId = insertEmployee(firstName, lastName, email, departmentId, countryCode, band, hireDate, status);

            String currencyCode = currencyByCountry.get(countryCode);
            BigDecimal fxToUsd = fxByCurrency.get(currencyCode);
            BigDecimal usdAmount = randomInRange(random, BAND_USD_RANGE.get(band));
            BigDecimal localAmount = usdAmount.divide(fxToUsd, 2, RoundingMode.HALF_UP);

            salaryBatch.add(new Object[]{
                    employeeId, localAmount, currencyCode, Date.valueOf(hireDate), "Initial hire",
                    Timestamp.from(Instant.now())
            });

            if (salaryBatch.size() == SALARY_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(INSERT_SALARY_SQL, salaryBatch);
                salaryBatch.clear();
            }
        }
        if (!salaryBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SALARY_SQL, salaryBatch);
        }
        return total;
    }

    private long insertEmployee(String firstName, String lastName, String email, long departmentId,
                                 String countryCode, JobBand band, LocalDate hireDate, EmployeeStatus status) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_EMPLOYEE_SQL, new String[]{"id"});
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setLong(4, departmentId);
            ps.setString(5, countryCode);
            ps.setString(6, band.name());
            ps.setDate(7, Date.valueOf(hireDate));
            ps.setString(8, status.name());
            ps.setTimestamp(9, Timestamp.from(Instant.now()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Map<String, String> queryCurrencyByCountry() {
        Map<String, String> map = new LinkedHashMap<>();
        jdbcTemplate.query("select code, currency_code from country", rs -> {
            map.put(rs.getString("code"), rs.getString("currency_code"));
        });
        return map;
    }

    private Map<String, BigDecimal> queryFxByCurrency() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        jdbcTemplate.query("select code, fx_to_usd from currency", rs -> {
            map.put(rs.getString("code"), rs.getBigDecimal("fx_to_usd"));
        });
        return map;
    }

    private static BigDecimal randomInRange(Random random, int[] range) {
        int value = range[0] + random.nextInt(range[1] - range[0] + 1);
        return BigDecimal.valueOf(value);
    }

    private static <K> List<K> expandWeights(Map<K, Integer> weights) {
        List<K> expanded = new ArrayList<>();
        weights.forEach((key, weight) -> {
            for (int i = 0; i < weight; i++) {
                expanded.add(key);
            }
        });
        return expanded;
    }
}
