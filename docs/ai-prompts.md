# AI Tool Usage Log

This project was built with Claude Code (Anthropic) acting as the primary
engineering agent, in an "auto mode" workflow: the human reviewed and
approved the overall plan and key architectural decisions up front, then
let the agent execute the build end-to-end with incremental commits,
checking in only at decision points that genuinely needed a human call.

## Key decisions made by the human (not the AI)

- Backend/frontend stack: **Java + Spring Boot, Angular** (to match the
  target job description), over alternative options the agent offered
  (Node/FastAPI backend, React/Next frontend).
- Deployment target: free-tier cloud (Render for backend+DB, Vercel for
  frontend) rather than local-only or self-managed deployment.
- Explicit instruction to work autonomously ("auto mode") and to ensure
  commit history shows incremental development, mirroring the
  assessment's own instructions.

## How the agent was used

- Given only `question.txt` (the assessment brief) and asked to read it,
  propose a plan, and clarify only the decisions that were genuinely
  ambiguous (stack choice, deployment target) via targeted questions
  before writing any code.
- Asked to produce the one-page requirements document *before* building,
  per the assessment's own process requirement.
- Directed to build in the same incremental sequence as the commit plan
  (schema → entities → seed → auth → APIs → tests → frontend → deploy),
  committing after each coherent step rather than in one final commit.
- Used to generate the 10,000-row seed data generator, the Spring
  Boot/Angular scaffolding, REST endpoints, and both backend and frontend
  test suites.

## Trade-offs the agent surfaced to the human

- Recommending a fixed FX snapshot over live FX rates, and single-role
  auth over a permissions matrix — both accepted as-is (see
  `requirements.md` for the reasoning).
- Flagging that it cannot record an actual screen-capture video demo;
  agreed the agent would produce a demo script/checklist instead, with
  the human recording the walkthrough.

(This log is appended to as the build progresses — see commit history for
the corresponding code changes at each step.)
