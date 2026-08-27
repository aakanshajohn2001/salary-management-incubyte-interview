# ACME Salary Management

A web app for ACME's HR Manager to manage and understand salary data for
~10,000 employees across multiple countries — replacing spreadsheets with
searchable/filterable directory, per-employee salary history, salary
adjustments with a clean audit trail, and org-wide pay analytics.

**Live app:** https://salary-management-incubyte-intervie.vercel.app
**Login:** `hr.manager` / `ChangeMe123!`

> The backend runs on Render's free tier, which spins the instance down
> after ~15 minutes of inactivity. The first request after a cold start
> can take 60–90 seconds (that's Spring Boot's own startup time, not a
> bug) — every request after that is fast.

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 4, Maven, Spring Security + JWT |
| Database | PostgreSQL (prod), H2 (local dev / tests), Flyway migrations |
| Frontend | Angular 22 (standalone components), Angular Material, ngx-charts |
| Deploy | Render (backend + Postgres), Vercel (frontend) |

See [`docs/architecture.md`](docs/architecture.md) for the full reasoning
behind these choices, system/domain diagrams, and API shape.

## Repo layout

```
backend/    Spring Boot API (Maven project)
frontend/   Angular app (Angular CLI project)
docs/       Requirements, architecture, performance notes, AI-usage log, demo script
render.yaml Render Blueprint (backend web service + Postgres)
```

## Running locally

**Backend** (defaults to a file-based H2 database, no Docker required):

```bash
cd backend
SEED_ON_STARTUP=true ./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. `SEED_ON_STARTUP=true` seeds 10,000
employees on first boot (idempotent — safe to leave set). To run against a
real local Postgres instead (useful for checking a query behaves the same
as it will on Render), start `docker-compose.yml` and run with
`SPRING_PROFILES_ACTIVE=docker`:

```bash
docker compose -f backend/docker-compose.yml up -d
SPRING_PROFILES_ACTIVE=docker SEED_ON_STARTUP=true ./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm start
```

Runs on `http://localhost:4200`, calling the backend directly at
`http://localhost:8080/api` (see `frontend/src/environments/environment.ts`).

**Local login:** `hr.manager` / `ChangeMe123!` (see `backend/src/main/resources/application.yml`).

## Testing

```bash
cd backend && ./mvnw test          # JUnit 5 + MockMvc + H2, JaCoCo coverage report
cd frontend && ng test             # Vitest + HttpClientTestingModule, V8 coverage report
```

Coverage and testing strategy are detailed in
[`docs/architecture.md`](docs/architecture.md#testing-strategy).

## Documentation

- [`docs/requirements.md`](docs/requirements.md) — goal, scope, and what was deliberately left out (and why)
- [`docs/architecture.md`](docs/architecture.md) — stack reasoning, system/domain diagrams, API shape, testing strategy
- [`docs/performance.md`](docs/performance.md) — measured performance at 10k rows, query patterns, indexes
- [`docs/ai-prompts.md`](docs/ai-prompts.md) — how AI tooling was used throughout the build, and the decisions made along the way
- [`docs/demo-script.md`](docs/demo-script.md) — walkthrough script for the video demo

## Deployment

Backend + Postgres deploy from [`render.yaml`](render.yaml) as a Render
Blueprint; frontend deploys from `frontend/` via [`frontend/vercel.json`](frontend/vercel.json).
Both are wired to auto-deploy from `main`.
