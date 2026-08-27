# Requirements — ACME Employee Salary Management Software

## Goal

Give ACME's HR Manager a single web application to manage and understand
salary data for ~10,000 employees across multiple countries, replacing
spreadsheets. The tool must let them (1) find and review any employee's
compensation, (2) record salary changes with a clean audit trail, and
(3) answer aggregate questions about how the org pays people — by
department, country, and level — without exporting data to Excel first.

## Persona

Single persona: **HR Manager**. One authenticated role; no self-service
employee portal, no multi-level approval chains. This keeps the auth
surface small and lets the UI be built around one coherent workflow
instead of a permissions matrix.

## Scope — In

1. **Authentication** — HR Manager logs in (JWT-based session). Single role.
2. **Employee directory** — search/filter (name, department, country, job
   band) and paginate across all 10,000 employees without a full-table
   scan on every keystroke.
3. **Employee detail** — profile plus current compensation and full salary
   history (every change is a new row, nothing is overwritten).
4. **Salary adjustment** — record a raise/adjustment with an effective
   date and a reason; this is the primary "write" workflow.
5. **Analytics** — headcount and total payroll cost by department and
   country; average/median salary by department and job band; payroll
   normalized to a common currency (fixed FX snapshot) so a single "total
   payroll cost" number is meaningful across countries; min/max salary per
   band as a basic pay-equity signal. This is the direct answer to "how do
   we pay people."
6. **CSV export** of the currently filtered directory view, for whatever
   ad-hoc slicing the analytics screen doesn't cover.
7. **Seed data** — a script that generates 10,000 employees spread across
   ~6-8 countries, several departments, and job bands with realistic,
   correlated salaries, so the app can be evaluated at real scale.

## Scope — Deliberately Out (and why)

- **Multi-role permissions / approval workflows** — only one persona (HR
  Manager) was specified. Building an approval chain or role matrix with
  no second persona to design it against would be speculative scope, not
  a real requirement.
- **Payroll/banking system integration, tax withholding** — genuine
  payroll processing is a regulated, country-specific domain (tax tables,
  statutory deductions, banking rails). None of that was asked for; this
  tool manages the *record* of what people are paid, not the act of
  paying them.
- **Live FX rate feeds** — a fixed FX snapshot is enough to make
  cross-country totals comparable for reporting purposes. Wiring a live
  rates API adds an external dependency and non-determinism for no
  requirement that actually needs live rates.
- **SSO / enterprise identity, i18n, mobile app** — none were requested;
  a single HR Manager login covers the stated persona, and a responsive
  web UI is sufficient without a dedicated mobile client.
- **Employee self-service portal** — the brief is explicitly about the HR
  Manager's workflow, not employees viewing their own pay.

Cutting this scope keeps the system small enough to be built to a high
standard (tested, readable, deployed) rather than spread thin across
features nobody asked for.

## Non-functional priorities

- **Correctness of the salary record**: adjustments are append-only —
  history is never mutated, only extended. This is what makes the audit
  trail and the analytics trustworthy.
- **Performance at 10k rows**: directory and analytics queries are
  server-side paginated/aggregated in SQL, not loaded wholesale into the
  browser or into Java memory.
- **Deployability**: the app must run as a real deployed service, not
  just on localhost, since that's an explicit deliverable.
