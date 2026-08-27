package com.acme.salary.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * Overridden to eagerly fetch department/country so a page of directory
     * rows doesn't trigger an N+1 lazy-load per row.
     */
    @Override
    @EntityGraph(attributePaths = {"department", "country"})
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    /**
     * Unpaginated variant used by CSV export -- still needs the entity graph
     * so exporting up to the full 10k employees doesn't lazy-load
     * department/country per row.
     */
    @Override
    @EntityGraph(attributePaths = {"department", "country"})
    List<Employee> findAll(Specification<Employee> spec, Sort sort);

    @Query("select e from Employee e join fetch e.department join fetch e.country where e.id = :id")
    Optional<Employee> findDetailById(@Param("id") Long id);

    long countByStatus(EmployeeStatus status);
}
