package com.acme.salary.compensation;

import com.acme.salary.employee.JobBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    @Query("""
            select sr from SalaryRecord sr join fetch sr.currency
            where sr.employee.id = :employeeId
            order by sr.effectiveDate desc, sr.id desc
            """)
    List<SalaryRecord> findHistoryByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * The single most-recent-as-of-today record for each of the given employees.
     * Used by the directory listing so a page of rows needs one query, not one
     * per employee.
     */
    @Query("""
            select sr from SalaryRecord sr join fetch sr.currency
            where sr.employee.id in :employeeIds
              and sr.effectiveDate = (
                  select max(sr2.effectiveDate) from SalaryRecord sr2
                  where sr2.employee.id = sr.employee.id
                    and sr2.effectiveDate <= current_date
              )
            """)
    List<SalaryRecord> findCurrentForEmployees(@Param("employeeIds") Collection<Long> employeeIds);

    /**
     * Org-wide average current salary (USD-normalized) per job band, active
     * employees only -- backs the pay-equity outlier flag on the directory
     * and detail views. Same "current salary" definition as
     * findCurrentForEmployees: the latest record with effectiveDate <= today.
     */
    @Query("""
            select e.jobBand as jobBand, avg(sr.amount * sr.currency.fxToUsd) as averageUsd
            from SalaryRecord sr join sr.employee e
            where sr.effectiveDate = (
                    select max(sr2.effectiveDate) from SalaryRecord sr2
                    where sr2.employee = sr.employee and sr2.effectiveDate <= current_date
                )
              and e.status = com.acme.salary.employee.EmployeeStatus.ACTIVE
            group by e.jobBand
            """)
    List<JobBandAverage> averageCurrentSalaryUsdByJobBand();

    interface JobBandAverage {
        JobBand getJobBand();

        BigDecimal getAverageUsd();
    }
}
