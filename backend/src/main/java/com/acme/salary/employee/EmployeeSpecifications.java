package com.acme.salary.employee;

import com.acme.salary.compensation.SalaryRecord;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like),
                cb.like(cb.lower(root.get("email")), like));
    }

    public static Specification<Employee> departmentId(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> countryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("country").get("code"), countryCode.toUpperCase());
    }

    public static Specification<Employee> jobBand(JobBand jobBand) {
        if (jobBand == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("jobBand"), jobBand);
    }

    public static Specification<Employee> status(EmployeeStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Sorts by current salary normalized to USD -- not a real Employee
     * column (salary lives in the append-only salary_record table), so it
     * can't be expressed as a plain Pageable Sort order. Sets the query's
     * ORDER BY directly via a correlated subquery (the same "latest record
     * with effectiveDate <= today" definition used everywhere else) rather
     * than pulling rows into Java to sort, keeping this correct at 10k rows.
     */
    public static Specification<Employee> orderByCurrentSalaryUsd(Sort.Direction direction) {
        return (root, query, cb) -> {
            Subquery<LocalDate> latestEffectiveDate = query.subquery(LocalDate.class);
            Root<SalaryRecord> sr2 = latestEffectiveDate.from(SalaryRecord.class);
            latestEffectiveDate.select(cb.greatest(sr2.<LocalDate>get("effectiveDate")))
                    .where(cb.equal(sr2.get("employee"), root),
                            cb.lessThanOrEqualTo(sr2.get("effectiveDate"), cb.currentDate()));

            Subquery<BigDecimal> currentSalaryUsd = query.subquery(BigDecimal.class);
            Root<SalaryRecord> sr = currentSalaryUsd.from(SalaryRecord.class);
            currentSalaryUsd.select(cb.prod(sr.<BigDecimal>get("amount"), sr.get("currency").<BigDecimal>get("fxToUsd")))
                    .where(cb.equal(sr.get("employee"), root),
                            cb.equal(sr.<LocalDate>get("effectiveDate"), latestEffectiveDate));

            query.orderBy(direction.isAscending() ? cb.asc(currentSalaryUsd) : cb.desc(currentSalaryUsd));
            return cb.conjunction();
        };
    }
}
