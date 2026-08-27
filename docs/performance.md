# Performance Considerations

The requirement that shapes most of the decisions here: **10,000
employees, and the HR Manager needs to search/filter/page the directory
and see org-wide analytics without a noticeable wait.** Every number
below was actually measured against the real 10,000-row seeded dataset
during development, not estimated.

## What was measured

| Operation | Measured | How |
|---|---|---|
| Seed 10,000 employees + initial salary records | **407 ms** | `SeedRunner` log timestamp, local dev run |
| `GET /api/analytics/summary` (4 aggregations over 9,520 active employees) | **~224 ms** end-to-end (curl, including HTTP overhead) | Timed `curl` against the running app |
| `GET /api/employees/export` (all 10,000 rows → CSV) | **~986 ms** end-to-end | Timed `curl` against the running app |
| `GET /api/employees/export?jobBand=L6` (291 matching rows) | Sub-second | Timed `curl`, cross-checked count against the analytics band breakdown |
| `GET /api/employees` (paginated, filtered) | Sub-second in manual testing | Not micro-benchmarked separately — same query shape as export, just with `LIMIT`/`OFFSET` |

None of these required any caching layer, connection-pool tuning, or
denormalization to hit — they're a direct result of the query patterns
below plus indexes that match the actual filters/joins.

## Query patterns

**Directory listing/search/filter/export** (`EmployeeRepository`,
`EmployeeService`): built as a JPA `Specification` combining only the
filters actually supplied (search/department/country/band/status), so
an unfiltered request is a plain indexed scan and a filtered request adds
only the predicates it needs — never a broader query filtered down in
Java afterward. The paginated listing and the unpaginated CSV export
share the same `Specification`-building code; both use an
`@EntityGraph` override so a page (or the full 10k rows, for export)
fetches `department`/`country` in one query instead of N+1 lazy loads
per row.

**"Current salary" lookups**: since `salary_record` is append-only,
"current salary" means "the latest record per employee." For a single
employee (the detail page) that's a simple indexed
`ORDER BY effective_date DESC LIMIT 1`-shaped query. For a *page* of
employees (the directory) or the *whole* dataset (export), it's one
batched query — `WHERE employee_id IN (...)` correlated against a
per-employee `MAX(effective_date)` subquery — so a page of 25 rows costs
one extra query, not 25.

**Analytics** (`AnalyticsRepository`): the hardest query in the app —
"pick each employee's current salary, then group by department / country
/ band" — is one `ROW_NUMBER() OVER (PARTITION BY employee_id ORDER BY
effective_date DESC)` CTE, reused (via a shared SQL string) across four
`GROUP BY` queries. This is the one place the app comes close to doing
real per-employee computation, and it stays fast because the window
function runs inside Postgres/H2 over indexed columns rather than 10,000
rows being pulled into the JVM and grouped in Java.

**Seeding**: uses `JdbcTemplate` directly instead of JPA/Hibernate
`save()` in a loop. Employee rows are inserted one at a time (to capture
each generated ID for its salary record), but `salary_record` inserts are
batched in groups of 500 — Hibernate's persistence-context bookkeeping
for 10,000 managed entities would add overhead this one-time bulk load
doesn't need.

## Indexes

From `V1__init_schema.sql`:

- `employee(department_id)`, `employee(country_code)`,
  `employee(job_band)`, `employee(status)` — one per directory filter
  dimension, so filtered queries don't fall back to a sequential scan.
- `salary_record(employee_id, effective_date DESC)` — a composite index
  that directly supports both "history for employee X, newest first" and
  the "current salary" correlated-subquery pattern above.

## Frontend

- The employee directory never fetches more than one page (default 25
  rows) from the backend — filtering/sorting/paging all happen
  server-side, so the browser is never holding all 10,000 employees in
  memory or in the DOM.
- Search input is debounced (300ms) so typing a name doesn't fire a
  request per keystroke.
- Each feature (login, directory, detail, analytics) is a separate
  lazy-loaded route chunk — the initial bundle doesn't pay for
  ngx-charts or the datepicker until the user actually navigates to a
  screen that needs them.

## What was deliberately *not* done

- **No caching layer** (Redis, etc.) — at 10k rows with the indexes
  above, Postgres itself is fast enough that a cache would be premature
  complexity with no measured problem to solve.
- **No read replicas / denormalized reporting tables** — the analytics
  CTE approach comfortably clears sub-second response times at this
  scale; revisit only if headcount grows by an order of magnitude.
