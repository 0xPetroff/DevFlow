# DevFlow

A project-management and deployment-tracking platform: issues on a Kanban board, environments and
release history, and a CI/CD gate that build agents drive through a scoped API key.

Spring Boot 4 on Java 25, PostgreSQL 17, React 19 with TypeScript, all of it containerised.
A modular monolith, not microservices: one deployable API, one database, one bundle.

Check it out on: https://dev-flow-petroff21s-projects.vercel.app — sign in as **`demo`** /
**`devflow-demo-1`**, an installation administrator, so every screen is reachable. The login
page offers the same credentials and fills the form for you.

## Running it

The only prerequisites are Docker with the Compose and Buildx plugins. The JDK and Node live
inside the build.

```bash
cp .env.example .env
openssl rand -base64 48   # once for POSTGRES_PASSWORD, once for DEVFLOW_JWT_SECRET

make up      # build both images and start the stack, waiting until every service is healthy
make smoke   # check every entry point answers
```

Then open **http://localhost:8081** and register. The first account on an empty database becomes
the installation ADMIN; every account after it is a DEVELOPER.

| URL | What it is |
|---|---|
| http://localhost:8081 | The application. It also proxies `/api`, so the browser talks to one origin |
| http://localhost:8080/swagger-ui.html | The API, documented and callable |
| http://localhost:8080/actuator/health | Liveness and readiness |

Also `make ps`, `make logs`, `make restart`, `make down`. For development with both applications
reloading on the host, `make db-up && make backend-run`, then `make frontend-dev` on :5173.
Every variable is documented in `.env.example`.

### With demo data

An empty instance is a login form and nothing else. To come up with seeded projects, a full
board, thirty days of release history and a production deployment waiting for approval:

```bash
SPRING_PROFILES_ACTIVE=prod,demo VITE_DEMO_MODE=true make up
```

Sign in as **`demo`** / **`devflow-demo-1`**, an installation administrator, so every screen is
reachable. The login page prints the credentials when `VITE_DEMO_MODE` is set, and a build
without it never renders them — pinned by a test.

The queued release on the DevFlow Platform project is worth a look: it sits in PENDING because
`production` is marked `requiresApproval`, and approving it from the Deployments tab is the
gate below, working.

**The demo restores itself on every restart.** The `demo` profile runs
`backend/src/main/resources/demo/demo-data.sql`, which deletes the seeded rows by id and
re-inserts them, so whatever visitors did is undone. It touches only those fixed ids, never
issuing an unscoped DELETE, so anything created alongside the demo data survives.

## How this was built

Built with Claude Code, across nine planned stages.

I set the scope, the stack and the constraints, and reviewed each stage before it was
committed. Much of the design was worked out during the build rather than up front.

## A few decisions

The reasoning is written down rather than left in the commits. Four that give the flavour:

- **The deployment approval gate lives in DevFlow, not in the CI provider.** An environment marked
  `requiresApproval` refuses to be started by anyone but a project administrator, so the pipeline
  queues the release, is refused with a 403, and polls until a person approves it in the UI.
  Which environments are gated is application configuration and needs no pipeline edit.
- **Deep links are handled by a CloudFront Function, not a custom error response.** Custom error
  responses are distribution-wide, so the usual `403 -> index.html` SPA fallback would turn every
  API authorization failure into an HTML page with status 200.
- **A board drag posts its neighbours, not a position.** The server owns the ordering, so two
  people dragging at once cannot invent conflicting positions, and a drag stays one UPDATE.
- **An unknown project and an inaccessible one both return 403, never 404.** A 404 would let a
  non-member enumerate project ids and keys.

[docs/HANDOFF.md](docs/HANDOFF.md) has the rest, stage by stage, including the bugs that were hit
and what they cost.

## Tests

```bash
make backend-test     # 164 tests: 155 integration against a real PostgreSQL, 9 unit
make frontend-test    # 91 tests
make frontend-build   # type-check, lint and production build
make infra-validate   # tofu validate and fmt over both environments
```

Almost all of the backend suite is integration tests: they register accounts through the real
API, run against a real PostgreSQL, and assert on the HTTP responses. The rules worth protecting
here are authorization rules and schema behaviour, and neither survives being mocked.

## Deployment

GitHub Actions. [ci.yml](.github/workflows/ci.yml) runs the backend suite, the frontend checks,
`tofu validate` and shellcheck on every pull request.
[deploy.yml](.github/workflows/deploy.yml) calls it as its gate on a push to main, and only then
ships the API to Render and the SPA to Vercel, before smoke-testing the result from outside.
It is called rather than triggered twice: two runs of the same workflow on one push share a
concurrency group, and they cancelled each other.

Both providers' own auto-deploy is switched off, in `render.yaml` and `vercel.json`. Left on,
each push would deploy twice and neither deploy would wait for the tests.

The scripts in [ci/scripts/](ci/scripts/) are the interesting part: `devflow-deployment.sh`
drives DevFlow's own deployment API, so a release is queued, gated, and reported back to the
platform that owns it. Every deployment in the history carries the commit, the branch, a link
back to the build, who or what triggered it, and how long it took.

Terraform for CloudFront and S3, ALB and ECS Fargate, and RDS in private subnets lives in
[infrastructure/aws/terraform/](infrastructure/aws/terraform/), as composable modules with a thin
root per environment. The demo itself runs on Render, Vercel and Neon; `render.yaml` and
`frontend/vercel.json` are the whole of that configuration.

## What is deliberately not here

Stating the edges is more useful than implying there are none.

- **The Terraform has never been applied.** It validates and it is formatted, and that is all
  that can honestly be claimed about it. Same for `deploy-ecs.sh` and `deploy-web.sh`. The
  scripts that talk to DevFlow itself were run end to end against the real API, including the
  approval gate and its timeout.
- **Tokens live in `localStorage`.** The API returns them in the response body, so there is no
  httpOnly-cookie option without changing the contract. The XSS trade-off is real and chosen.
- **Drag and drop has no end-to-end test.** dnd-kit measures layout and jsdom gives every element
  a zero rect, so a simulated drag never starts. The ordering arithmetic is unit-tested instead.
- **Swagger and production sourcemaps are both shipped on purpose**, the first because the API
  documentation is part of what this is meant to show, the second for readable stack traces.
- **No push channel and no scheduled housekeeping.** Two people on a board see each other's moves
  on the next refetch; expired refresh tokens and old audit rows are never pruned.
