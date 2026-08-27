# AI Tool Usage Log

I built this project using Claude Code (Anthropic) as my primary implementation
agent, working in an "auto mode" workflow: I set direction and reviewed
output, Claude Code executed autonomously between checkpoints, and I stepped
in at the moments that actually needed a human call.

## What I actually decided

- **Stack, deliberately matched to the target role**: I chose Java + Spring
  Boot for the backend and Angular for the frontend, over alternatives
  Claude Code proposed (Node/FastAPI, React/Next), specifically because the
  job description calls for Java + Angular. This is the one choice
  everything else in the build follows from.
- **Deployment target and its trade-offs**: I picked free-tier Render
  (backend + Postgres) and Vercel (frontend). When I learned Render's free
  Postgres expires after 30 days, I made an informed call to accept that
  limitation rather than add deployment complexity, since the assessment's
  review window doesn't need long-term persistence.
- **Process requirements, enforced throughout**: I required incremental
  commits that actually show development evolution (mirroring the
  assessment's own instruction), rather than one large commit at the end —
  and I checked that against the real commit log partway through rather
  than just assuming it was happening.
- **Test coverage held to a real bar, not a padded one**: once I had
  coverage tooling wired up (JaCoCo, Vitest/V8) and saw the gaps, I directed
  that they be closed by fixing actual undertested behavior (e.g.
  country/job-band filters that were never exercised end-to-end, a
  batch-flush path in the seed script that no test ever triggered) — not by
  writing throwaway tests that touch a line without asserting anything
  real. When the coverage report surfaced dead code (unused entity
  constructors), I had it removed rather than padded around.
- **Verification before moving forward**: I asked to see the app running
  locally, with both servers actually up and the seeded 10,000-row dataset
  behind it, before I treated any feature as done — and before I agreed to
  move on to deployment.
- **Scope and artifact completeness tracked against the brief**: I worked
  through the assessment's own checklist (requirements doc, architecture
  diagrams, performance write-up, this log, trade-off explanations) point
  by point rather than assuming it was covered.

## How I used the agent

- I gave it only the assessment brief and asked it to read the brief,
  propose a plan, and ask me about genuinely ambiguous decisions rather
  than guessing.
- I directed it to build in dependency order (schema → entities → seed →
  auth → each API → frontend scaffold → each screen → export → tests →
  deploy config → docs), committing after each coherent step.
- I used it to generate the implementation itself: Spring Boot/Angular
  boilerplate, the 10,000-row seed generator, REST endpoints, and both test
  suites — including debugging real issues it hit along the way (a Spring
  Data JPA null-handling behavior change, Spring Boot 4 shipping on Jackson
  3 under a different package, an ngx-charts version dropping the chart
  variant I'd originally planned for the analytics view).
- I asked it to measure test coverage rather than estimate it, then
  directed it to close the gaps found — see `docs/performance.md` and the
  coverage numbers now recorded in `docs/architecture.md`.

## Trade-offs it surfaced to me, and the calls I made

- Fixed FX snapshot instead of a live rates feed, and single-role auth
  instead of a permissions matrix — I accepted both; my reasoning is
  captured in `docs/requirements.md`.
- It can't record an actual screen-capture video demo; I agreed it would
  produce a demo script and I'd record the walkthrough myself.
- I accepted Render's free-tier Postgres expiry (30 days) as reasonable for
  this assessment's timeframe rather than adding a longer-lived DB provider.

## What this demonstrates

The brief explicitly frames this as an assessment of how I think, design,
and build software in an AI-driven environment — not whether I typed every
line by hand. My engineering judgment is in the decisions above: matching
the stack to the job description rather than personal preference, refusing
to let a coverage number stand in for actual test quality, insisting on
running the software before calling it done, and directing an agent through
a multi-week scope of work (schema, auth, five API surfaces, four UI
screens, deployment config) to a coherent, tested result. Working
effectively with AI tooling — steering it, catching what it gets wrong, and
knowing where to draw the line — is itself part of what this role requires.
