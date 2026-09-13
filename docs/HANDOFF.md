# DevFlow — Build Handoff

Status as of 2026-09-12. **All nine stages are complete, committed and verified.** The backend and
the frontend are feature-complete, both ship as container images, the pipeline and the
infrastructure are written, and the README covers the whole thing. This document is written so a
new session can pick up work without re-deriving anything.

---

## 1. What DevFlow is

A project-management and CI/CD deployment-tracking platform (Jira x GitHub Projects x a
deployment dashboard), built as a portfolio piece demonstrating React, Spring Boot,
PostgreSQL, auth, Docker, Jenkins, AWS and testing. The audience is hiring managers, so
authorization correctness, schema quality, test coverage and the deployment story matter
as much as feature count. Explicitly not a toy CRUD app, and explicitly not microservices:
it is a well-structured modular monolith.

Full original plan: `~/.claude/plans/cozy-forging-clarke.md`

---

## 2. Non-negotiable constraints

- **Minimal comments.** Self-documenting code. Comment only for non-obvious business rules,
  security rationale, or deliberate deviations. No restating what the next line does, no
  boilerplate Javadoc, no section-divider banners.
- **No emojis anywhere** — code, comments, commit messages, logs, README, Jenkinsfile output, UI.
- Report at the end of each stage: what was implemented, files changed, how to run it,
  architectural decisions, remaining work.
- Verify with real builds and real tests. Do not claim something works without running it.

---

## 3. Environment (verified, and full of traps)

| Thing | Value |
|---|---|
| Project root | `/Users/petar/Desktop/Work/DevFlow` |
| Java | Temurin **25** at `/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home` |
| Maven | 3.9.16 (Homebrew) |
| Node / npm | 24.12.0 / 11.6.2 |
| Docker | Colima 0.10.3 + Docker CLI 29.8 (no Docker Desktop) |
| OpenTofu | 1.12.6 (validates Terraform HCL; `terraform` is no longer in Homebrew core) |
| Git | repo initialised, branch `main` |

**Traps that will waste your time if you forget them:**

1. **Maven picks the wrong JDK.** Homebrew's `maven` pulled in JDK 26 as a dependency and
   uses it by default. Always run Maven with:
   ```
   export JAVA_HOME=$(/usr/libexec/java_home -v 25)
   ```
   The `Makefile` already does this.

2. **PATH.** Homebrew binaries need `export PATH="/opt/homebrew/bin:$PATH"` in non-login shells.

3. **Port 5432 is taken by a pre-existing PostgreSQL** on this machine (runs as user
   `postgres`, not Homebrew, not managed by us — leave it alone). DevFlow's container is
   mapped to host port **5433**. Connecting to 5432 gives a confusing
   `password authentication failed`.

4. **Colima's Docker socket is at `~/.colima/default/docker.sock`**, where Testcontainers
   does not look. `backend/pom.xml` has a `colima` profile that auto-activates on that file's
   existence and sets `DOCKER_HOST` + `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` for
   surefire/failsafe. It stays inactive on CI. Do not delete it.

5. **The Desktop is iCloud-synced**, which creates duplicate `"SomeClass 2.class"` files inside
   `backend/target/`. These break the JaCoCo report with
   `Can't add different class with same name`. Fix: `mvn clean verify`. If it recurs often,
   excluding `target/` from iCloud sync is the permanent fix.

6. **`docker compose` plugin** was symlinked manually into `~/.docker/cli-plugins/docker-compose`.
   **buildx** needed the same treatment in Stage 8 (`brew install docker-buildx`, then symlink
   `/opt/homebrew/lib/docker/cli-plugins/docker-buildx` into `~/.docker/cli-plugins/`). Without it
   the CLI silently falls back to the classic builder, which rejects `RUN --mount=type=cache` with
   `the --mount option requires BuildKit`. `docker buildx ls` should show the `colima` builder.

7. **Colima must be running**: `colima start`. Testcontainers tests fail without it.

8. **Never run `mvn verify` without `clean`.** The VS Code Java extension auto-builds into the same
   `backend/target/classes` Maven uses (`.vscode/settings.json` sets
   `java.configuration.updateBuildConfiguration: automatic`). When its project model goes stale it
   writes Eclipse *error-stub* class files over javac's output — the stub throws `Error` and its
   constant pool holds unqualified names. The symptom is a baffling
   `NoClassDefFoundError: AuditLog` (simple name, no package) while Spring introspects a MapStruct
   `...MapperImpl`, and an incremental `mvn verify` will not fix it because the timestamps look
   current. `mvn clean verify` always does. Tell-tale check:
   `javap -v -classpath target/classes <class> | grep 'java/lang/Error'`.

9. **The IDE diagnostics that arrive after an edit are often stale**, for the same reason — they
   report `cannot be resolved` for classes that exist right after a `mvn clean`. Trust `javac`,
   not them. They are still worth reading: they caught a genuine duplicate-erasure overload.

---

## 4. Stack, and the Spring Boot 4 landmines

**Spring Boot 4.1.1 on Java 25, Spring Security 7.1, Hibernate 7.4, Flyway 12.4,
PostgreSQL 17, springdoc 3.1.1, MapStruct 1.6.3, Testcontainers 2.0.5.**

Boot 4 differs from the heavily-documented Boot 3 generation in ways that break code written
from memory. All of these were hit and resolved already:

| Boot 3 habit | Boot 4 reality |
|---|---|
| `spring-boot-starter-web` | **deprecated** — use `spring-boot-starter-webmvc` |
| Flyway auto-configures from classpath | needs **`spring-boot-starter-flyway`** + `flyway-database-postgresql` |
| `spring-boot-starter-test` has MockMvc | needs **`spring-boot-starter-webmvc-test`**, `-data-jpa-test`, `-flyway-test` |
| `@AutoConfigureMockMvc` in `...test.autoconfigure.web.servlet` | now `org.springframework.boot.webmvc.test.autoconfigure` |
| `ObjectMapper` bean, `com.fasterxml.jackson` | **Jackson 3**: inject `JsonMapper`, package `tools.jackson.*` |
| `JsonNode.asText()` | `asString()` |
| `org.testcontainers:postgresql`, `org.testcontainers.containers.PostgreSQLContainer` | `testcontainers-postgresql`, `org.testcontainers.postgresql.PostgreSQLContainer` |
| `@MockBean` / `@SpyBean` | `@MockitoBean` / `@MockitoSpyBean` |
| `HttpStatus.UNPROCESSABLE_ENTITY` | `UNPROCESSABLE_CONTENT` (same for `status().isUnprocessableContent()`) |
| `AntPathRequestMatcher`, `MvcRequestMatcher` | **removed** — `PathPatternRequestMatcher` only |
| `http.csrf()` no-arg, `.and()` chaining | **lambda DSL is mandatory** |
| `Specification.allOf(a, b, null)` | **rejects nulls** — see `common/Specifications.allOfPresent` |
| springdoc 2.x | springdoc **3.x** is the Boot 4 line |

Two more that cost real debugging time:

- **PostgreSQL cannot infer the type of an untyped null bind.** A JPQL query like
  `WHERE (:search IS NULL OR lower(u.name) LIKE :search)` fails at runtime with
  `function lower(bytea) does not exist`. Use JPA Specifications for all optional filtering.
  `UserSpecifications` is the pattern to copy for issues and deployments.
- **`@Transactional(REQUIRES_NEW)` for audit writes breaks foreign keys.** A new transaction
  runs on a second connection and cannot see rows the caller has not committed, so auditing a
  just-created entity fails its `actor_id` FK. `AuditService.recordAs` therefore uses
  `Propagation.MANDATORY` and joins the caller's transaction. Do not "fix" this back.

---

## 5. What is done

### Stage 1 — skeleton, schema, bootable API (commit `563a926`)

- Monorepo: `backend/`, `frontend/`, `infrastructure/{docker,aws}`, `jenkins/`, `docs/`
- `docker-compose.yml` (PostgreSQL 17 healthcheck-gated, backend, frontend), `Makefile`,
  `.gitignore`, `.env.example`, `infrastructure/docker/nginx.conf`
- **Flyway `V1__baseline.sql`** — the complete schema for the whole project, already covering
  tables not yet used by code:
  `users, projects, project_members, labels, issues, issue_labels, comments, environments,
  deployments, api_keys, refresh_tokens, audit_logs`
  with FKs, deliberate cascade rules, check constraints, 28 indexes and an `updated_at` trigger.
- `BaseEntity` (id + createdAt) / `AuditedEntity` (+ updatedAt + `@Version`)
- Hibernate runs **`ddl-auto=validate`** everywhere. Flyway is the single source of truth.

### Stage 2 — authentication and authorization (commit `ec13a24`)

- `POST /api/auth/register` (first account registered becomes ADMIN), `/login` (email *or*
  username), `/refresh`, `/logout`, `/logout-all`, `GET /api/auth/me`
- `GET /api/users` (filter `q`/`role`/`active`, paginated), `GET /api/users/{id}`,
  `PUT /api/users/{id}` (ADMIN), `PUT /api/users/me`, `PUT /api/users/me/password`
- BCrypt strength 12; access JWT HS256 via Nimbus (15 min); refresh tokens opaque, stored as
  SHA-256 hash only, rotated on every use, **replay revokes the whole token family**
- `GlobalExceptionHandler` producing RFC 9457 `ProblemDetail` for every error path
- Audit module (`AuditAction` enum already lists every action the later stages need)
- OpenAPI config with both `bearerAuth` and `apiKeyAuth` schemes declared
- **22 tests green**: 4 unit (`JwtServiceTest`) + 18 integration against real PostgreSQL
  (`AuthControllerIT` 13, `UserControllerIT` 9)

Test support worth reusing: `support/IntegrationTest` (meta-annotation),
`support/PostgresContainerConfig` (singleton container), `support/TestAccounts`
(registers accounts through the real API, `deleteEverything()` truncates all tables).

### Stage 3 — projects, members and the permission model

- `POST /api/projects` (ADMIN or DEVELOPER; creator becomes owner and first project ADMIN),
  `GET /api/projects` (filter `q`/`status`, paginated, scoped to what the caller can see),
  `GET /api/projects/{id}`, `GET /api/projects/key/{projectKey}`, `PUT /api/projects/{id}`,
  `DELETE /api/projects/{id}`
- `GET|POST /api/projects/{projectId}/members`, `PUT|DELETE /api/projects/{projectId}/members/{userId}`
- **`ProjectAccessService`** (bean name `projectAccess`) is the single authority on project
  permissions, used as `@PreAuthorize("@projectAccess.canWrite(#projectId)")`. `canWrite` has no
  caller yet — Stage 4 is its first — but `ProjectAccessServiceIT` already pins the whole matrix.
- **50 tests green** (22 from Stage 2 + 15 `ProjectControllerIT` + 11 `ProjectMemberControllerIT`
  + 9 `ProjectAccessServiceIT`), plus 4 `JwtServiceTest` unit tests: 57 total.

Rules that are now enforced and tested, so later stages can rely on them:

| Rule | Why |
|---|---|
| Account-wide ADMIN bypasses every project check | The only global-role shortcut in the model |
| Otherwise the **project** role governs, independently of the account role | A global VIEWER added as a project DEVELOPER can write in that project |
| The owner is always kept in `project_members` as ADMIN | Member listings and permission checks see one consistent set |
| The owner cannot be removed or demoted | Transfer ownership instead (`ownerId` on `PUT /api/projects/{id}`) |
| Transferring ownership upgrades the new owner to project ADMIN | Otherwise an owner could be locked out of their own project |
| A project must be ARCHIVED before it can be deleted | Delete cascades to every issue, comment, environment and deployment |
| Membership cannot change on an ARCHIVED project | `ProjectService.requireActiveProject` — reuse it for issues and deployments |
| `project_key` is immutable and stored upper case | Issue keys derive from it |
| Unknown *and* inaccessible projects both return **403**, never 404 | A 404 would let non-members enumerate project ids and keys |
| `PROJECT_DELETED` is audited with `projectId = null` | `audit_logs.project_id` cascades, so the entry would delete itself |

Test support added: `support/TestProjects` (create a project, add a member, archive) — it is
registered in the `@IntegrationTest` meta-annotation alongside `TestAccounts`.

`Role` now carries explicit ranks and `isAtLeast(Role)`. Ranks are explicit rather than ordinal
comparison so reordering the enum constants cannot silently grant access.

### Stage 4 — issues, labels, comments and the board

- Issues: `POST|GET /api/projects/{projectId}/issues`, `GET /api/projects/{projectId}/board`,
  `GET|PUT|DELETE /api/issues/{id}`, `PUT /api/issues/{id}/position`, `GET /api/issues/key/{issueKey}`
- Labels: `GET|POST /api/projects/{projectId}/labels`, `PUT|DELETE /api/projects/{projectId}/labels/{labelId}`
- Comments: `GET|POST /api/issues/{issueId}/comments`, `PUT|DELETE /api/issues/{issueId}/comments/{commentId}`
- **`IssueAccessService`** (bean `issueAccess`) resolves an issue to its project so issue-scoped
  routes stay declarative: `@PreAuthorize("@issueAccess.canWrite(#issueId)")`. It only answers
  *which project*; permission still comes from `ProjectAccessService`. This is where `canWrite`
  finally gets its production callers.
- **103 tests green** (57 from Stage 3 + 46 new across `IssueControllerIT` 13, `BoardControllerIT` 9,
  `CommentControllerIT` 8, `IssueListingIT` 7, `LabelControllerIT` 6, `IssueNumberingIT` 3).

Design decisions worth keeping:

| Decision | Why |
|---|---|
| Issue numbers come from `UPDATE projects SET issue_sequence = issue_sequence + 1 ... RETURNING` | One statement; the row lock serialises concurrent creates. `IssueNumberingIT` proves it with 8 parallel creates. `Project.issueSequence` stays `insertable=false, updatable=false` so Hibernate never writes a stale counter back over it |
| A drag posts its **neighbours**, not a number (`MoveIssueRequest`) | The server owns the ordering, so two clients dragging at once cannot invent conflicting positions, and a drag stays one UPDATE |
| The new position is computed **before** the status is changed | Otherwise a drop with no neighbours measures the target column against the moved card's own position |
| A gap under 0.0001 renumbers the column *before* splitting | Once the gap is unsplittable the midpoint lands on a neighbour and the order becomes arbitrary |
| Changing status through `PUT /api/issues/{id}` sends the card to the end of its new column | The old position is meaningless in a different column |
| Assignees must be project members; labels must belong to the project | 422, checked in `IssueService` rather than trusted from the client |
| Issue and comment deletion: author/reporter **or** project ADMIN | Writing gets you your own; clearing up someone else's is an administrator's job |
| Comment editing is author-only, and `CommentService.update` flushes before mapping | `@LastModifiedDate` is stamped on flush, so without it the reply would say `edited: false` right after an edit |
| `@BatchSize(50)` on `Issue.labels`, `@EntityGraph` for the to-one associations | A collection fetch combined with pagination makes Hibernate page in memory |
| The board caps each column (`limitPerColumn`, default 100, max 200) but reports the true `total` | A project with thousands of DONE issues must not return an unbounded board |

Also in this stage, closing a Stage 3 follow-up: `common/SortProperties.validate` gives every
listing an explicit sort whitelist (400 instead of the previous 500), with a
`PropertyReferenceException` handler as a safety net. Sorting was otherwise an ordering oracle for
columns the API never exposes, such as `passwordHash`.

`AuditAction` was **not** extended: label changes and comment edits are not audited. Issue and
comment create/update/delete are.

Test support added: `support/TestIssues` (create an issue from a title or raw JSON, create a label,
post a comment), registered in the `@IntegrationTest` meta-annotation.

### Stage 5 — environments, deployments, API keys and the dashboard

- Environments: `GET|POST /api/projects/{projectId}/environments`,
  `PUT|DELETE /api/projects/{projectId}/environments/{environmentId}` (admin only; they are release config)
- Deployments: `POST /api/deployments`, `GET /api/deployments/{id}`,
  `PUT /api/deployments/{id}/status`, `GET /api/projects/{projectId}/deployments`
- API keys: `GET|POST /api/projects/{projectId}/api-keys`, `DELETE .../{keyId}` (revokes, keeps the row)
- Dashboard: `GET /api/dashboard?windowDays=30`
- **First migration since the baseline: `V2__deployment_environment_fk.sql`.**
- **146 tests green** (103 from Stage 4 + 43 new: `DeploymentControllerIT` 11, `ApiKeyControllerIT` 11,
  `EnvironmentControllerIT` 7, `DashboardControllerIT` 6, `DeploymentApprovalIT` 5,
  `DeploymentStatusTest` 5 unit, `ProjectDeletionCascadeIT` 3).

| Decision | Why |
|---|---|
| `DeploymentStatus` owns the transition table; `Deployment.transitionTo` is the only way the status changes | A build agent cannot report SUCCESS for a run it never started, and a finished run cannot be reopened. Timestamps and duration are stamped there too, so they can never drift from the status and the schema's own checks hold by construction |
| `CreateDeploymentRequest` names the environment instead of identifying it by id | This is the request a Jenkinsfile makes, and a pipeline has "staging" to hand far more readily than a UUID. Names are unique per project, so it still resolves to one environment |
| `Environment.requiresApproval` blocks the **start**, not the create | CI queues the production release as PENDING; only a project ADMIN can move it to RUNNING. After that the pipeline reports the outcome itself. This is the gate the Stage 9 Jenkinsfile needs |
| **One** filter chain, with `ApiKeyAuthenticationFilter` before `BearerTokenAuthenticationFilter` | The deployment endpoints serve the dashboard as well as build agents; a separate CI-only chain would have to duplicate the bearer setup to keep serving people |
| `shouldNotFilter` restricts API keys to `/api/deployments` | A leaked key cannot read the issue tracker or the user directory even if a later endpoint forgets to check scopes. Tested |
| An API key is ignored when `Authorization` is also present | A request is never authenticated as two identities; the bearer token wins |
| `deploymentAccess` rather than teaching `projectAccess` about keys | A key holds `deployment:write` on one project; folding it into the general project authority would have let it create issues |
| Keys are **hex**, not base64url | See the bug below |
| API keys are revoked, never deleted | The audit trail should still be able to name the key that did something |
| `successRate` is null, not 0, when nothing has finished | No completed runs is not a 0% success rate |

Two bugs this stage produced and the tests caught, both fixed in the code:

1. **Keys were generated with base64url, whose alphabet contains the `_` used as the key's own
   separator.** Roughly half of all issued keys could not be parsed back and failed with 401. This
   is worth remembering because it is *probabilistic*: single-key tests passed or failed by luck,
   and two tests were passing for the wrong reason. `everyIssuedKeyHasTheSameShapeAndAuthenticates`
   issues ten keys and pins the format, so a regression cannot hide.
2. The Stage 3 cascade trap fired exactly as predicted, and `V2` fixes it — see below.

**The cascade fix, since the reasoning is subtle.** Deleting a project cascades to both
`environments` and `deployments`, and PostgreSQL fires those in constraint-creation order, so
environments go first. `ON DELETE RESTRICT` is checked the instant that row is deleted, while the
deployments still reference it, so the delete failed. `V2` changes that one FK to **NO ACTION**,
which still refuses to delete an environment that has deployment history but defers the check to
the end of the statement, by which point the project's own cascade has removed the deployments.
`ProjectDeletionCascadeIT` covers it. `EnvironmentService.delete` also refuses explicitly with a
422 rather than leaving it to an opaque integrity error.

Test support added: `support/TestDeployments` (create an environment, deploy as a person or as a
key, transition, issue an API key), registered in `@IntegrationTest`.

`AuditAction` still unchanged: deployments audit create, start and each outcome with the acting
key's label via `recordAs`, which is what the Stage 2 note about CI service accounts anticipated.

### Stage 6 — frontend foundation

**Vite 8.3 + React 19.3 + TypeScript 6.0 (strict) + Tailwind 4.3 + React Router 8.3 +
TanStack Query 5.102 + axios 1.20 + react-hook-form 7.88 + zod 4.6. Vitest 5 + RTL + MSW 2.**

- `src/lib/apiClient.ts` — axios with the token attached per request, **a single-flight refresh**
  and a replay of the request that hit the 401
- `src/lib/sessionStore.ts` — credentials outside React (`useSyncExternalStore`), persisted, with
  cross-tab sync
- `src/lib/apiError.ts` — every failure becomes one `ApiError` carrying status, the RFC 9457
  problem and its per-field validation messages
- `src/types/api.ts` — the whole wire contract, every backend DTO, so Stage 7 writes no new types
- Auth: `AuthProvider`, `useAuth`, `RequireAuth`, `RequireRole`, login and register pages
- Layout: sidebar (role-filtered) + topbar + account menu, responsive drawer under `lg`
- Theme: light/dark/system, class-based, applied before first paint by an inline script
- Pages: dashboard (live stat tiles), settings (profile, password, sign out everywhere),
  403, 404 and a route error boundary. Projects and Users are placeholders for Stage 7
- **38 tests green** across 5 files: `apiClient` 9, `sessionStore` 8, `apiError` 6,
  `auth` (whole-app, through the real router) 12, `DashboardPage` 3

Run it: `make frontend-install`, then `make frontend-dev` (:5173, proxies `/api` to :8080).
`make frontend-test`, `make frontend-build` (typecheck, lint, build).

**Version landmines, all resolved. These cost real time; do not undo them.**

| Trap | Resolution |
|---|---|
| **TypeScript 7.0.2 is `latest` but typescript-eslint requires `<6.1.0`** | Pinned to **TypeScript 6.0.3**, the newest release that type-aware linting supports. Taking `latest` loses the entire lint layer |
| `jsdom@30` requires Node `^24.15`, this machine has 24.12 | Pinned `jsdom@^29.1.1` |
| `eslint-plugin-react-hooks@7` exposes two config shapes | Flat config is at `configs.flat['recommended-latest']`. `configs['recommended-latest']` is eslintrc and makes ESLint 10 fail to start |
| `eslint-plugin-jsx-a11y` does not support ESLint 10 | Not installed |
| Vite 8 is rolldown-based; `manualChunks` is gone | `build.rolldownOptions.output.codeSplitting.groups`. `advancedChunks` works but warns it is deprecated |
| React Router 8 | Everything imports from `react-router`; `react-router-dom` is not a dependency |
| Tailwind v4 has no config file | `@theme inline` in `src/index.css`, plus `@custom-variant dark` for class-based dark mode rather than the media default |

**The contract bug real verification caught.** The API is configured with
`spring.jackson.default-property-inclusion: non_null` (`application.yml:32`), so a null field is
**absent from the payload, not sent as null**. Types written as `field: T | null` were a lie, and
`successRate === null` was therefore false for `undefined`, rendering `NaN%` on a fresh instance.
Every nullable response field in `types/api.ts` is now `field?: T | null`, and consumers use `??`
or `== null`, never `=== null`. `DashboardPage.test.tsx` pins it with a payload copied verbatim
from the running server. Both that test and the single-flight refresh test were verified by
breaking the code and watching them fail.

| Decision | Why |
|---|---|
| The refresh is deduplicated into one in-flight promise | Refresh tokens rotate and a replay revokes the whole family, so five requests hitting an expired token must send exactly one refresh. Pinned by `sends exactly one refresh for requests that fail concurrently` |
| A request refreshes **ahead** of expiry, within 30s of it | Saves a guaranteed-401 round trip on every page load after a quiet period |
| A refresh that fails on the network keeps the session | A blip is not proof the token is dead; only a 400/401/403 from the server clears it |
| Tokens live in `localStorage` | The API returns them in the JSON body, so there is no httpOnly-cookie option without changing the contract. The XSS trade-off is real and deliberate |
| `sessionStore` sits outside React | The axios interceptors need the same source of truth, and a context would make them depend on render order |
| The query cache is cleared when the user id changes | Cached data belongs to the identity that fetched it |
| Tests render the **whole app** through `createMemoryRouter` | Guards, layouts and fetching are exercised the way a browser does, not as a hand-assembled subset |
| `VITE_API_BASE_URL` defaults to the relative `/api` | The dev proxy serves it locally and nginx serves it in the container, so one build works in both |
| Vendor split into `react` and `vendor` chunks | nginx caches hashed assets immutably, so a release only invalidates what changed. 181 kB gzip total, 10 kB of it app code |
| Client-side role guards are a courtesy | The API enforces every rule again. `RequireRole` only spares the user a page that would fail |

### Stage 7 — frontend features, and the audit read API

**Added `@dnd-kit/core` 6.3 + `/sortable` 10 + `/utilities` 3.2, and `recharts` 3.10.**

One backend gap closed, the one Stage 6 predicted: there was no way to read `audit_logs`.

- `GET /api/projects/{projectId}/audit-logs` — `@projectAccess.canRead`, filters `actorId`,
  `action`, `since`, `q`, paginated, newest first
- `GET /api/audit-logs` — the same for the whole installation, `hasRole('ADMIN')`
- `AuditQueryService` is separate from `AuditService`: writing an entry stays a
  transaction-joining side effect, reading one stays an ordinary query
- The dead `AuditLogRepository.search` (a JPQL query with the `:param IS NULL OR` pattern that
  the Stage 4 note warns about) and the unused `findByProjectIdInOrProjectIdIsNull` are gone,
  replaced by `AuditLogSpecifications`
- `AuditLogResponse` does not carry `ip_address`, and `AuditLogControllerIT` pins that
- **154 backend tests green** (146 from Stage 5 + 8 `AuditLogControllerIT`)

Frontend, as root-level feature folders beside the existing `auth/` and `theme/`:
`projects/`, `issues/`, `deployments/`, `admin/`, `toast/`.

- Projects: list with search, status filter and pagination; create dialog; a project shell
  (`ProjectLayout`) with tabs for Board, Issues, Deployments, Environments, Members, Activity
  and Settings
- Board: `@dnd-kit`, cards draggable within and between columns, optimistic reordering
- Issues: filtered list, detail page with comments, create and edit dialog
- Deployments: filtered history, the approval gate, and the full status machine
- Environments: CRUD plus the API-key panel, which shows an issued key once
- Members, project settings (details, ownership transfer, labels, archive and delete)
- Admin: the account directory and the account-wide audit log
- Dashboard: two charts and a recent-deployment list
- Toast system (`toast/`), the gap Stage 6 recorded
- **88 frontend tests green** (38 from Stage 6 + 50 new)

| Decision | Why |
|---|---|
| The client derives the caller's project role from the members list rather than a new DTO field | `ProjectResponse` has no `myRole`, and the members list is already fetched for the Members tab. `ProjectLayout` mirrors the backend's two tiers exactly, ownership check included |
| `writer`/`admin` are false on an ARCHIVED project, `projectAdmin` is not | Every write path on the API calls `requireActiveProject`. Un-archiving and deleting are the two things an administrator does to an archived project, so those stay available |
| Pointer drag listeners on the whole card, the same listeners **also** on a grip button | The card's title is a link. Enter on a focused link navigates, and the keyboard sensor wants Enter too, so the keyboard drag needs its own focusable control. Pointer users still drag anywhere on the card |
| The drag reorders the **query cache**, not component state | Two sources of truth meant the optimistic order flashed back to the server's while the request was in flight. The cache is the only copy, and the refetch simply replaces it |
| `boardOrdering.ts` is a separate, pure module | The neighbour computation is where the subtle bugs are, and dnd-kit cannot be driven in jsdom (nothing has a size). 15 unit tests cover it instead |
| Form dialogs are **mounted only while open** | They were resetting their defaults in an effect on open, which the React compiler lint rule rejects and which is a stale-state bug waiting to happen. Conditional mounting gets the right defaults at construction |
| Issue status is charted as an **ordinal** ramp, deployment status with the reserved status colours | The issue stages run in an order, so one hue stepping in lightness shows it. Deployment status means good or bad, which is what the status palette is for. Every bar is labelled on the axis and at its tip, so colour never carries identity alone |
| The chart is `aria-hidden` with the same numbers repeated as an `sr-only` table | An SVG of unlabelled paths reads as noise. The table is also the only assertion that works in jsdom |

**Two real bugs the new tests caught, both fixed:**

1. **Axios serialises an array parameter as `status[]=DONE`, which Spring does not bind to a
   `List` and drops silently** — every multi-value filter would have been ignored with no error.
   `apiClient` now sets `paramsSerializer: { indexes: null }`. Verified by removing the fix and
   watching `asks the API for one status at a time as a repeated parameter` fail.
2. **`neighbours()` returned the first card as the "next" neighbour for an id not in the
   column**, because `findIndex` returns -1 and `issues[-1 + 1]` is `issues[0]`. That would have
   sent a bogus position to the server.

**The chunking trap.** The two lazy routes were splitting correctly but `charts` was still a
modulepreload on the entry. The cause: rolldown matches `codeSplitting.groups` in order, and
`clsx` — which the app imports directly and recharts also depends on — was being claimed by the
`charts` group, dragging the whole 100 kB gzip charting bundle into the eager graph. The fix is a
`vendor` group listing the app's own direct dependencies **before** the lazy groups. Check it by
looking at what `dist/index.html` preloads, not at the chunk list.

### Stage 8 — container images and the compose stack

- **`backend/Dockerfile`** — three stages on `maven:3.9.16-eclipse-temurin-25-alpine`, runtime on
  `eclipse-temurin:25-jre-alpine`, running as uid 10001. 439 MB.
- **`frontend/Dockerfile`** — `node:24-alpine` build, runtime on
  `nginxinc/nginx-unprivileged:1.30-alpine` as uid 101. 98 MB.
- `nginx.conf` moved from `infrastructure/docker/` to `frontend/`, next to the Dockerfile that
  ships it, so `docker build ./frontend` needs no wider context. `infrastructure/docker/` is gone;
  `infrastructure/aws/` stays for Stage 9. `security-headers.inc` is new.
- `.dockerignore` in both service directories.
- Compose: `security_opt: no-new-privileges` on all three services, healthchecks on 127.0.0.1,
  and the backend now defaults to the **prod** profile — a container is a deployment. Host ports
  moved off 80 (`FRONTEND_PORT=8081`) because Colima's forwarding cannot bind a privileged port.
- Makefile: `images`, `up` (now `--wait`), `restart`, `ps`, `smoke`.
- **`README.md` now exists**, covering how to run the stack, how to run it for development, the
  test commands, the configuration table and the two images. Stage 9 extends it with the
  architecture, the pipeline and the AWS story rather than starting it from scratch.
- **154 backend and 88 frontend tests still green**, and the stack was verified end to end.

The whole stack comes up healthy in roughly 23 s, and `make smoke` checks all four entry points.

| Decision | Why |
|---|---|
| The jar is exploded with `-Djarmode=tools ... extract --layers --launcher` into four COPY layers | Dependencies are 66.7 MB and the application 1.2 MB, so a code-only release rebuilds and pushes one thin layer. Boot 4 dropped the `layertools` jarmode that every Boot 3 example uses; the `tools` one replaces it. Confirmed in the built image's layer sizes, not assumed |
| `--destination` must be an empty directory | `extract --destination .` into the directory holding the jar fails with `already exists and is not empty`. The jar stays in `target/` and the layers land in `/layers` |
| Tests are **not** run inside the image build | They need a Docker daemon for Testcontainers, which the build does not have. `mvn verify` is its own pipeline stage against a real database, which is Stage 9's job |
| `MaxRAMPercentage` rather than a fixed `-Xmx` | The JVM sees the container limit, so a percentage is the only setting that survives being given a different memory allocation. `ExitOnOutOfMemoryError` lets the orchestrator replace a dying container instead of leaving one up failing every request |
| `curl` is installed in the backend runtime image | It serves the healthcheck. Busybox `wget` cannot fail on a 4xx, which is exactly what an unready actuator probe returns |
| The security headers live in an `.inc` included per location | nginx **discards every inherited `add_header`** in a location that declares one of its own, so the server-level block was silently not applying to `/` or `/assets/`. `.inc` rather than `.conf` keeps nginx from loading it as a server of its own |
| `Cache-Control` on `/assets/` is one `add_header`, not `expires` plus one | `expires 1y` emits its own `Cache-Control: max-age=...`, so the response carried the header twice |
| `VITE_API_BASE_URL` defaults to `/api` everywhere now | The same nginx serves the app and proxies the API, so the browser makes no cross-origin request and needs no CORS grant. The root `.env.example` previously set an absolute `http://localhost:8080/api`, which worked only because CORS happened to allow it |

**Two bugs real verification caught:**

1. **The frontend container was permanently unhealthy while serving perfectly.** `listen 8080;`
   binds IPv4 only, `localhost` inside the container resolves to `::1` first, and the healthcheck
   got `Connection refused`. Only `docker compose up --wait` surfaced it — a plain `up -d` returns
   before the first check runs, and the service answers fine from the host the whole time. Both
   healthchecks now name `127.0.0.1`.
2. `dependency:go-offline` and `extract` both failed on first run for the reasons in the table
   above; neither would have shown up without building the image.

### Stage 9 — Jenkins, AWS and the README

> **Superseded.** Jenkins was removed after this stage, in favour of GitHub Actions — see
> "GitHub Actions, Neon and the hosted demo" below. The scripts survived the move and the
> approval-gate reasoning below still holds; only the runner changed. Kept as a record of what
> was built and why.

- **`Jenkinsfile`** at the root. Multibranch: `feature/*` and PRs run Verify, Images and Scan;
  `develop` also pushes and deploys to staging; `main` does the same to production behind
  approval. Stages are Metadata, Verify (backend and frontend in parallel), Images, Scan, Push,
  Deploy.
- **`jenkins/scripts/`** — `devflow-deployment.sh` (create, start, finish against the DevFlow
  API), `deploy-ecs.sh`, `deploy-web.sh`, `smoke-test.sh`.
- **`infrastructure/aws/terraform/`** — modules `network`, `database`, `api`, `web`,
  `certificate` and the composing `environment`, with thin roots in `envs/staging` and
  `envs/production`. Both roots pass `tofu validate` and `tofu fmt -check`; `make infra-validate`
  runs both.
- **README** gained Architecture, The permission model, The deployment pipeline, AWS and What is
  deliberately not here, and a table of contents.
- One backend change: `Deployment.transitionTo` now keeps `failureReason` for **CANCELLED** as
  well as FAILED. **155 backend tests green** (154 plus `aCancelledRunKeepsItsReason`), 88
  frontend tests still green.

**What was actually verified, and what was not.** `devflow-deployment.sh` was run end to end
against the running stack: the ungated staging path, the production approval gate with a real
administrator approving mid-poll, the approval timeout cancelling the deployment and failing,
a reported failure with its reason, and a rejected key. The Jenkinsfile parses as valid Groovy
(checked by compiling it with a Groovy 4 jar fetched into the scratchpad, and confirmed to
reject a deliberately broken copy) but has never run on a Jenkins instance. The Terraform
validates and its dependency graph builds with no cycle, but **has never been applied** — there
is no AWS account behind it, and `deploy-ecs.sh` and `deploy-web.sh` have never run for real.

| Decision | Why |
|---|---|
| The approval gate lives in DevFlow, not in a Jenkins `input` step | `Environment.requiresApproval` already refuses a start from anyone but a project administrator, and that is the gate Stage 5 built. A Jenkins `input` would be a second, unrelated gate that the application knows nothing about |
| `start` attempts the transition and falls back to polling on 403 | The pipeline never hardcodes which environments are gated; the DevFlow environment configuration owns it, and turning the gate on for staging needs no pipeline edit. A 403 here cannot mean a bad key, because an unusable key fails at `create` |
| An approval that never arrives cancels the deployment and fails the build | A queued PENDING release left behind forever is worse than a recorded cancellation |
| Only the API image is pushed to ECR | On AWS the SPA is static objects behind CloudFront. The frontend image is still built and scanned on every branch, because it is the compose stack's deliverable |
| Images are built on every branch, pushed on two | A Dockerfile that no longer builds is a broken build, not something to find on the way to production |
| Trivy is advisory except on `main` | A fixable HIGH on a base image should stop a production release and annotate everything else |
| CloudFront handles deep links with a **Function**, not a custom error response | Custom error responses are distribution-wide, so rewriting 403/404 to `index.html` would turn every API authorization failure into an HTML page with status 200, which the client parses as RFC 9457. `smoke-test.sh` checks both halves |
| The ECS service `ignore_changes` its task definition and desired count | The pipeline registers each release's revision and autoscaling moves the count. Terraform owns the shape of the task, not the image on it |
| `deploy-ecs.sh` reads the running revision and swaps only the image | Everything else keeps what the infrastructure declared, so a release cannot quietly become the source of truth for configuration |
| The ALB accepts only CloudFront's managed prefix list, and forwards only on a shared `X-Origin-Verify` | Two independent reasons the origin cannot be addressed directly |
| The database's only ingress rule references the API's security group | No CIDR list to widen. The api and database modules reference each other, which resolves fine: Terraform's graph is resource-level, confirmed by building it |
| Terraform generates the JWT key and the origin secret into Secrets Manager | The execution role resolves them at container start, so no secret is in a task definition, a plan, or the pipeline |
| `CANCELLED` now keeps its `failureReason` | A pipeline giving up on approval is the commonest cancellation, and why is the only useful thing to know afterwards. The frontend already rendered the field conditionally, so it needed no change |

### Demo data (after Stage 9)

The gap Stage 9 left: an empty instance is a login form and nothing else, which made both the
hosted-demo idea and README screenshots pointless.

- **`demo/demo-data.sql`** — 5 accounts, 3 projects, 23 issues across all four board columns,
  labels, comments, 3 environments, 31 deployments over the last 30 days, an API key and 14
  audit entries.
- **`DemoDataLoader`** — `@Profile("demo")` `ApplicationRunner` that executes the script on
  every start, so the demo returns to a known state after visitors have had their way with it.
- **`application-demo.yml`** — carries no configuration; the profile's only effect is activating
  the loader, so `SPRING_PROFILES_ACTIVE=prod,demo` is production plus demo data and nothing
  else.
- **`VITE_DEMO_MODE`** — build arg through `frontend/Dockerfile` and compose. When set, the login
  page publishes the seeded credentials and offers a button that fills the form.
- **164 backend tests** (unchanged; the seed cannot reach them, tests run under `test`) and
  **91 frontend tests** (88 plus 3 new).

| Decision | Why |
|---|---|
| Not a Flyway migration at all | A versioned one takes a number out of the schema's own sequence, and the next real migration numbered below it refuses to apply. A repeatable one re-runs only when its **checksum** changes, which is not the same as on every start. An ordinary script run by the application gives a reset per restart and makes an edit to the file actually take effect |
| The script deletes by fixed id, then inserts | A reset that undoes what visitors did, without ever issuing an unscoped DELETE. The cost is that a project or account a visitor creates alongside the demo data is left behind. Verified by renaming a project, deleting an issue and approving the queued release, then restarting: all three came back |
| Timestamps are `now() - interval` | The dashboard's window is 30 days. Absolute dates would leave the charts empty a month after the file was written |
| A `demo` profile layered on `prod`, not a fork of it | The demo should differ from a real deployment in exactly one respect, the seed location. Everything else stays identical, so the demo is evidence about the real configuration |
| `VITE_DEMO_MODE` gates the credentials panel | A real installation must never render a published password. `auth.test.tsx` pins the absence, and forcing the flag on was confirmed to fail it |
| The seeded API key's hash has no known preimage | The panel needs a key to display; nothing should be able to authenticate with it |
| One production release is left **PENDING** | It is the approval gate, live: sign in as demo and approve it. Better than any screenshot of the feature |
| The demo account is an installation ADMIN | Every screen, including the admin directory and audit log, is reachable without a second login. The `prod` profile never loads the seed, which is what keeps this safe |

The bcrypt hash was generated with `htpasswd -nbBC 12` and then verified against the project's own
`BCryptPasswordEncoder(12)` — `$2y$` is accepted by Spring's encoder, which was confirmed rather
than assumed.

### GitHub Actions, Neon and the hosted demo (after the demo data)

Jenkins was dropped. It needs a server nobody was going to run, and the repository already lives
on GitHub, so the pipeline moved to Actions and the demo went onto free hosting.

- **`Jenkinsfile` deleted**, `jenkins/scripts/` renamed to **`ci/scripts/`**. The scripts are
  unchanged apart from comments: they were always plain bash reading environment variables, so
  nothing about them was Jenkins-specific.
- **`.github/workflows/ci.yml`** — backend `mvn clean verify`, frontend typecheck, lint, test and
  build, `tofu fmt -check` and `validate` on both roots, and shellcheck over `ci/scripts`. The
  last two are new coverage; the Jenkinsfile never checked either.
- **`.github/workflows/deploy.yml`** — calls `ci.yml`, then ships the API to Render through a
  deploy hook and the SPA to Vercel through the CLI, waits for readiness, smoke-tests from
  outside, and optionally records the release through `devflow-deployment.sh`.
- **`.github/workflows/keep-demo-warm.yml`** — superseded. Render's free tier sleeps a service
  after 15 minutes idle and waking it costs about 55 seconds, but GitHub's cron does not keep to
  its schedule: over one 7.5 hour window it started this workflow three times, with gaps of 120
  and 319 minutes against the 10 minutes asked for. A **cron-job.org** job now pings
  `/actuator/health/readiness` every 2 minutes instead, which is what actually holds the service
  open. Measured against it: the JVM ran 4.8 hours without a restart on the external pings alone.
- **`render.yaml`** and **`frontend/vercel.json`**. The hosting runbook was dropped; the Neon
  and provider details are in the decision table below.

| Decision | Why |
|---|---|
| Auto-deploy switched off on **both** providers (`autoDeploy: false`, `git.deploymentEnabled: false`) | Vercel and Render each deploy on push by default. Left on, every push deploys twice and neither deploy waits for a test, so the CI workflow would be decoration |
| `deploy.yml` calls `ci.yml` with `workflow_call` rather than duplicating the steps | One definition of what "passing" means, and the deploy cannot drift from it |
| Every deploy step is skipped, not failed, when its secret is missing | The workflow is harmless in a fork and can be set up one provider at a time |
| The DevFlow recording job is opt-in on a repository variable | It needs an API key, and the demo wipes its seeded rows on every restart along with any key issued through the UI. It only makes sense against a DevFlow that persists, so it stays off rather than failing every deploy |
| The **direct** Neon endpoint, not the pooled one | The pooler is PgBouncer in transaction mode, which breaks Hibernate's server-side prepared statements unless `prepareThreshold=0`. One instance with a pool of five does not need a pooler |
| `channel_binding` dropped, credentials split out of the URL | It is a libpq option the JDBC driver does not take, and a password in a URL ends up in logs |
| No Neon CLI, `neon.ts` or `neon deploy` | That tooling deploys an application *to* Neon. This one deploys to Render, so the connection string is the whole integration |

Verified against the real Neon project: Flyway migrated all thirteen tables onto **PostgreSQL
18.6** (the project develops against 17) with no complaint, the demo profile seeded it, and the
dashboard answered with 23 issues and 31 deployments over a live connection.

---

## 6. What is left

Nothing is outstanding from the original plan. Section 8 lists the known follow-ups, which are
improvements rather than gaps in the build.

### Decisions already made (do not re-litigate)

- **Jenkins authenticates with a scoped, hashed, revocable API key** in the `X-DevFlow-Api-Key`
  header. Issued per project by an ADMIN, shown once, stored as SHA-256 hash + indexed 8-char
  prefix, carries expiry and `lastUsedAt`, grants only `deployment:write` on one project. The
  `api_keys` table and the OpenAPI security scheme already exist. Chosen over signed webhook
  (no payload contract to version) and service-account JWT (refresh is awkward for CI).
- **API paths carry no version prefix** (`/api/projects`, not `/api/v1/projects`) — matches the
  original spec.
- **AWS**: CloudFront + S3 for the SPA, ALB + ECS Fargate for the API, RDS PostgreSQL in private
  subnets, Secrets Manager for secrets. Terraform HCL, validated with `tofu validate`, never applied.
- **Frontend styling**: Tailwind v4 with hand-built components, dark-first developer-tool aesthetic.

---

## 7. Commands

```bash
export PATH="/opt/homebrew/bin:$PATH"
export JAVA_HOME=$(/usr/libexec/java_home -v 25)

colima start                 # required for Testcontainers and compose

make db-up                   # PostgreSQL 17 on localhost:5433
make db-reset                # destroy volume and re-migrate (needed if V1 is edited)
make backend-run             # API on :8080
make backend-test            # mvn verify: 164 tests (155 integration, 9 unit)
make db-shell

cd backend && mvn clean verify   # prefer clean: avoids the iCloud duplicate-class issue

make frontend-install        # npm ci
make frontend-dev            # Vite on :5173, proxying /api to :8080
make frontend-test           # vitest run: 91 tests
make frontend-build          # typecheck, lint, production build

make infra-validate          # tofu validate and fmt over both environments

make images                  # build both container images
make up                      # build and start the stack, waiting for health
make smoke                   # check every entry point on a running stack
make ps / make logs / make down / make restart
```

The containerised stack serves the app on `http://localhost:8081` (`FRONTEND_PORT`), which also
proxies `/api` to the backend, so the browser only ever talks to one origin. The API is published
on :8080 as well, for Swagger and for direct calls.

Useful URLs: `http://localhost:8080/actuator/health`, `/swagger-ui.html`, `/v3/api-docs`

`.env` exists locally with real generated secrets and is gitignored. `.env.example` is committed.
Regenerate secrets with `openssl rand -base64 48`.

---

## 8. Known follow-ups

- `/actuator/**` beyond health and info is restricted to ROLE_ADMIN. Confirm this is what the
  Jenkins health-check stage should hit; `/actuator/health` is public, so it is fine as is.
- No `@DataJpaTest` repository-layer tests yet; API-level integration tests cover the same
  ground so far.
- Refresh tokens are never garbage-collected. `RefreshTokenRepository.deleteExpiredBefore`
  exists but nothing calls it — a `@Scheduled` cleanup is worth adding.
- `AuditService.record` reads the actor from the SecurityContext; CI-triggered deployments will
  need `recordAs` with an explicit service-account label.
- ~~The dev seed location is configured but empty.~~ Closed: `demo/demo-data.sql` under the
  `demo` profile. The `classpath:db/seed` Flyway location is gone, since the script is run by
  the application rather than migrated.
- `ApiKeyService.authenticate` writes `last_used_at` on every authenticated CI request. That is
  what the column is for, but it does mean one UPDATE per request; if a pipeline ever polls hard,
  make it throttle to, say, once a minute.
- API key prefixes are 12 hex characters, not the 8 the Stage 2 note sketched. 8 would be 32 bits,
  and the prefix carries a unique constraint, so a collision would surface as a failed issuance.
- Nothing prunes deployment history, and a project's dashboard window is capped at a year.
- `DashboardService` runs about six queries. Fine at this size, and each one is a grouped
  aggregate rather than a fetch, but it is the first place to look if the landing page feels slow.
- `IssueResponse` carries no comment count, so the frontend needs a second call to show one on a
  card. The batched-count pattern in `ProjectService.countMembers` is what to copy if it is wanted.
- `IssueService.board` issues two queries per column (eight in total) plus the label batches. Fine
  at this size; a single grouped query would be the optimisation if a board ever feels slow.
- `Issue.getKey()` touches the lazy `project` association. Every current path either join-fetches
  it or runs inside a transaction, but a new caller mapping issues outside one would trip it.
- Nothing purges audit rows, and the Stage 2 note about refresh-token cleanup still stands; one
  `@Scheduled` housekeeping bean could cover both.

Frontend, from Stage 6:

- Production sourcemaps are built and shipped. Deliberate, for debuggable stack traces, but it is
  a choice worth restating in the README.
- The account menu's Settings link does not close the mobile nav drawer, which only matters if
  the drawer is open under `lg`.

Docker, from Stage 8:

- **nginx resolves `backend` once, at startup.** If the backend container is replaced and gets a
  new IP, the frontend keeps proxying to the old one until it is restarted too. The Docker-specific
  fix is `resolver 127.0.0.11` plus a variable in `proxy_pass`, which would not carry to AWS, where
  the SPA sits on CloudFront and S3 and this proxy is not in the path at all.
- The backend image is 439 MB, almost all of it the full JRE. `jlink` with a module set derived
  from `jdeps` would cut it substantially; layering already keeps the *push* small.
- Springdoc logs two warnings on every prod start, advising that `/v3/api-docs` and
  `/swagger-ui.html` be disabled. They stay on deliberately: the API documentation is part of what
  the project is meant to show. Worth restating in the README.
- The JVM exits 143 on `docker compose stop`, which is SIGTERM's status, not a failed shutdown.
  Graceful shutdown is configured and runs; under the prod profile its log lines are below WARN.
- `make backend-run` still runs the API on the host under the **dev** profile against the
  containerised database. Only the containers run prod. Both read the same `.env`.
- No image is tagged or pushed anywhere yet, and nothing scans them. Stage 9 owns tagging by
  commit, pushing to ECR, and a `trivy`-style scan stage if it wants one.

Frontend, from Stage 7:

- **Drag and drop is not covered by an end-to-end test.** dnd-kit measures layout, and jsdom
  gives every element a zero rect, so a simulated drag never starts. `boardOrdering.ts` is
  unit-tested instead, and the wiring between it and dnd-kit is not. A Playwright test against
  the real stack is the honest way to close this, which Stage 8's compose stack would enable.
- The board does not subscribe to anything, so two people dragging the same board see each
  other's moves only on the next refetch. Polling `refetchInterval` on the board query would be
  the cheap fix; the API has no push channel.
- Filter state lives in component state, not the URL, so a filtered issue list cannot be linked
  or restored by a refresh. `useSearchParams` is the drop-in.
- `IssuesPage` sends one status and one priority at a time even though the API takes lists, and
  the label filter is not wired up at all. The types and the request already support both.
- Issue and project lists refetch rather than update in place after a mutation. Correct, but it
  is a round trip where `setQueryData` would do, the way the board does it.
- `ProjectLayout` fetches the member list on every project route to decide what the caller may
  do. It is cached, but a `myRole` field on `ProjectResponse` would make it one request and one
  source of truth rather than a client-side re-derivation of the server's rule.
- The dashboard charts are fixed to a 30-day window; `windowDays` is a query parameter the UI
  never exposes.
- No virtualisation anywhere. A board column caps at 100 cards and lists paginate, so nothing
  renders an unbounded list today.
- The audit views expose `action`, `actorId` and `q` but not `since`, which the API supports.

Pipeline and infrastructure, from Stage 9:

- **The Actions workflows have never completed a run.** Their YAML is valid and the scripts they
  call are verified against a real API, but the action versions, the Vercel CLI invocation and
  the Render deploy hook are unexercised until the first push. Expect the first run to need a
  correction or two.
- The `/api` rewrite destination in `vercel.json` is a literal Render URL and has to be edited by
  hand. Vercel does not interpolate environment variables into rewrites, so there is no tidier
  way short of generating the file at build time.
- Deployments recorded in the hosted demo are wiped by the next restart, because the reset
  deletes the seeded projects and the cascade takes every deployment with them. The recording job
  is therefore off by default.
- **The Terraform has never been applied**, so nothing has been proved beyond `validate`, `fmt` and
  a clean dependency graph. First apply will find the usual things validate cannot: quota limits,
  ACM validation timing, and whether the CloudFront Function's URI rewrite behaves on real paths.
- The provider lock file is gitignored (`.gitignore` from Stage 1). Committing it properly means
  `tofu providers lock` for every platform that might run it, which is not worth doing for a
  configuration nothing applies.
- There is no bootstrap root for the state bucket and the ECR repository, so the very first
  `tofu init -backend-config` has nowhere to write. A tiny `envs/bootstrap` with a local backend
  is the usual answer.
- Nothing rolls back a release except the ECS circuit breaker. Redeploying an older tag works
  because ECR tags are immutable, but there is no pipeline stage for it and no migration
  rollback story at all — Flyway migrations are forward-only, which is a deliberate constraint
  rather than an oversight.
- `deploy-web.sh` publishes the SPA before `smoke-test.sh` runs, so a failed smoke test leaves the
  new bundle live while the deployment is reported FAILED. Splitting the S3 sync from the
  CloudFront invalidation, and invalidating only after the smoke test, would close that.
- The pipeline polls `GET /api/deployments/{id}` every 15 seconds while waiting for approval, and
  `ApiKeyService.authenticate` writes `last_used_at` on every authenticated request. Half an hour
  of waiting is 120 UPDATEs. The throttle noted above is the fix if it ever matters.
