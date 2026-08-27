# Architecture Notes

## Stack

| Layer      | Choice                                             | Why |
|------------|-----------------------------------------------------|-----|
| Backend    | Java 17, Spring Boot 3, Maven                       | Matches the target JD (Java/Spring craftsperson role) |
| DB         | PostgreSQL (prod/dev), H2 in-memory (tests)          | Relational, handles 10k-row aggregation well; H2 keeps tests fast/hermetic |
| Migrations | Flyway                                              | Versioned schema = readable history of data-model evolution |
| Auth       | Spring Security + JWT (jjwt)                        | Single-role login, stateless, no session store needed |
| Frontend   | Angular 17 (standalone components) + Angular Material | Matches JD; Material gives production-grade table/form/dialog components out of the box |
| Charts     | ngx-charts                                          | Lightweight, no heavy dependency for a handful of dashboard charts |
| Deploy     | Render (backend + Postgres), Vercel (Angular static build) | Both have workable free tiers for a demo deployment |

## Domain model

```
Country(code PK, name, currency_code, fx_to_usd)
Department(id PK, name)
Employee(id PK, first_name, last_name, email, department_id FK,
         country_code FK, job_band, hire_date, status)
SalaryRecord(id PK, employee_id FK, amount, currency_code,
             effective_date, reason, created_at)
AppUser(id PK, username, password_hash, role)
```

`SalaryRecord` is **append-only**: giving a raise inserts a new row with a
later `effective_date`; it never updates a prior row. "Current salary" is
defined as the record with the latest `effective_date <= now()` per
employee. This is what makes salary history and the audit trail trustworthy,
and lets analytics simply be "aggregate the current record per employee."

`job_band` is a simple enum-like string (e.g. `L1`..`L6`) rather than a
separate table — it doesn't need its own attributes beyond a label, so a
join table would be premature.

## API shape (indicative)

- `POST /api/auth/login` → JWT
- `GET /api/employees?search=&department=&country=&band=&page=&size=` → paginated directory
- `GET /api/employees/{id}` → profile + current salary
- `GET /api/employees/{id}/salary-history` → ordered list of SalaryRecord
- `POST /api/employees/{id}/salary-adjustments` → append a SalaryRecord
- `GET /api/analytics/summary` → headcount/payroll by dept & country, band pay ranges, currency-normalized totals
- `GET /api/employees/export` → CSV of current filtered view

All endpoints except `/api/auth/login` require a valid JWT.

## Performance approach at 10k employees

- Directory listing and analytics are computed with SQL
  aggregation/pagination (`LIMIT`/`OFFSET` + indexed columns, `GROUP BY`
  queries), not by loading all rows into the JVM and filtering in Java.
- Indexes on `employee.department_id`, `employee.country_code`,
  `employee.job_band`, and `salary_record.employee_id` support the
  directory filters and the "latest salary per employee" lookup.
- The 10k-row seed uses batched JDBC inserts (not one `save()` call per
  row) so seeding stays fast and repeatable.

## Testing strategy

- **Backend unit tests** (JUnit5 + Mockito): service-layer logic —
  salary-adjustment validation (no backdating before the last record,
  positive amounts), analytics aggregation math, DTO mapping. No DB, no
  Spring context — fast and deterministic.
- **Backend integration tests** (`@SpringBootTest` + H2): auth flow,
  directory pagination/filtering against real repository queries, salary
  adjustment persisting correctly and appearing in history. H2 in place
  of Postgres keeps these hermetic and fast while still exercising real
  SQL.
- **Frontend unit tests** (Jasmine/Karma): directory filter/search logic,
  salary-adjustment form validation, auth interceptor/guard behavior —
  each isolated from network calls via mocked services.

## What was deliberately kept simple

- One JWT role, no refresh-token rotation or password-reset flow — there's
  a single known HR Manager user, not a self-serve user base.
- Fixed FX snapshot table instead of a live-rates integration (see
  `requirements.md` for the reasoning).
- No caching layer (e.g. Redis) — at 10k rows, well-indexed Postgres
  queries are fast enough that a cache would be premature complexity for
  this exercise.
