package com.acme.salary.employee;

import org.springframework.data.jpa.domain.Specification;

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
}
