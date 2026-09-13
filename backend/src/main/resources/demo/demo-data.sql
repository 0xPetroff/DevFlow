-- Demo data, restored on every start by DemoDataLoader under the demo profile.
--
-- Deliberately not a Flyway migration. A versioned one would reserve a number in the schema's
-- own sequence, and the next real migration below it would refuse to apply. A repeatable one
-- re-runs only when its checksum changes, which is not the same as on every start. This is an
-- ordinary script, executed by the application, so a demo comes back to a known state whenever
-- it restarts and an edit to this file actually takes effect.
--
-- Deleted and re-inserted rather than merged, so whatever visitors did to the seeded rows is
-- undone. Only these fixed ids are touched: a project or account a visitor created alongside
-- them is left alone, which is the price of never issuing an unscoped DELETE.
--
-- Timestamps are relative to now(), so the dashboard's 30-day window always has data in it no
-- matter when the demo is started.
--
-- The demo account is a published credential. Never activate the demo profile on an
-- installation holding real data.

-- Reverse dependency order. Deleting the projects cascades to their issues, labels, comments,
-- environments, deployments and audit entries; the accounts can only go once nothing they
-- created still references them.
DELETE FROM audit_logs WHERE actor_id IN (
    '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000005');

DELETE FROM projects WHERE id IN (
    '10000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000002',
    '10000000-0000-4000-8000-000000000003');

DELETE FROM users WHERE id IN (
    '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000004',
    '00000000-0000-4000-8000-000000000005');

-- Password for every account below is: devflow-demo-1
INSERT INTO users (id, email, username, password_hash, full_name, role, avatar_color, created_at)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'demo@devflow.dev', 'demo',
     '$2y$10$2C5tUBHTJL/W7ir4xJ0NWO9ktCt6ZUon3fwQDe9DbNfPjG4nbBcl.',
     'Demo Admin', 'ADMIN', '#6366f1', now() - interval '90 days'),
    ('00000000-0000-4000-8000-000000000002', 'alice@devflow.dev', 'alice',
     '$2y$10$2C5tUBHTJL/W7ir4xJ0NWO9ktCt6ZUon3fwQDe9DbNfPjG4nbBcl.',
     'Alice Chen', 'DEVELOPER', '#0ea5e9', now() - interval '88 days'),
    ('00000000-0000-4000-8000-000000000003', 'marco@devflow.dev', 'marco',
     '$2y$10$2C5tUBHTJL/W7ir4xJ0NWO9ktCt6ZUon3fwQDe9DbNfPjG4nbBcl.',
     'Marco Ferreira', 'DEVELOPER', '#f59e0b', now() - interval '80 days'),
    ('00000000-0000-4000-8000-000000000004', 'priya@devflow.dev', 'priya',
     '$2y$10$2C5tUBHTJL/W7ir4xJ0NWO9ktCt6ZUon3fwQDe9DbNfPjG4nbBcl.',
     'Priya Nair', 'DEVELOPER', '#10b981', now() - interval '75 days'),
    ('00000000-0000-4000-8000-000000000005', 'sam@devflow.dev', 'sam',
     '$2y$10$2C5tUBHTJL/W7ir4xJ0NWO9ktCt6ZUon3fwQDe9DbNfPjG4nbBcl.',
     'Sam Okafor', 'VIEWER', '#ec4899', now() - interval '60 days');

INSERT INTO projects (id, project_key, name, description, repository_url, status, owner_id,
                      issue_sequence, created_at)
VALUES
    ('10000000-0000-4000-8000-000000000001', 'DF', 'DevFlow Platform',
     'The platform itself: issues, environments, releases and the CI gate.',
     'https://github.com/0xPetroff/DevFlow', 'ACTIVE',
     '00000000-0000-4000-8000-000000000001', 18, now() - interval '90 days'),
    ('10000000-0000-4000-8000-000000000002', 'WEB', 'Marketing Site',
     'Static marketing site and docs. Shares the release pipeline.',
     NULL, 'ACTIVE', '00000000-0000-4000-8000-000000000002', 5, now() - interval '45 days'),
    ('10000000-0000-4000-8000-000000000003', 'LEG', 'Legacy Importer',
     'Retired one-off importer. Archived, kept for its history.',
     NULL, 'ARCHIVED', '00000000-0000-4000-8000-000000000001', 0, now() - interval '120 days');

-- The owner is always kept here as ADMIN, which is the invariant ProjectService enforces.
INSERT INTO project_members (id, project_id, user_id, role, created_at)
VALUES
    ('11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', 'ADMIN', now() - interval '90 days'),
    ('11000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'DEVELOPER', now() - interval '88 days'),
    ('11000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'DEVELOPER', now() - interval '80 days'),
    ('11000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000004', 'DEVELOPER', now() - interval '75 days'),
    ('11000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000005', 'VIEWER', now() - interval '60 days'),
    ('11000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'ADMIN', now() - interval '45 days'),
    ('11000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001', 'ADMIN', now() - interval '45 days'),
    ('11000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000003', 'DEVELOPER', now() - interval '40 days'),
    ('11000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001', 'ADMIN', now() - interval '120 days');

INSERT INTO labels (id, project_id, name, color, created_at)
VALUES
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'backend', '#2563eb', now() - interval '89 days'),
    ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'frontend', '#7c3aed', now() - interval '89 days'),
    ('20000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'infrastructure', '#0891b2', now() - interval '89 days'),
    ('20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'security', '#dc2626', now() - interval '89 days'),
    ('20000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 'tech-debt', '#a16207', now() - interval '70 days'),
    ('20000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000002', 'content', '#16a34a', now() - interval '44 days'),
    ('20000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000002', 'design', '#db2777', now() - interval '44 days');

INSERT INTO issues (id, project_id, issue_number, title, description, status, priority, type,
                    assignee_id, creator_id, due_date, board_position, estimate_points, created_at)
VALUES
    -- TODO
    ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 1,
     'Refresh tokens are never garbage collected',
     'RefreshTokenRepository.deleteExpiredBefore exists and nothing calls it. A scheduled bean could cover this and the audit rows in one pass.',
     'TODO', 'HIGH', 'TASK', '00000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001',
     current_date + 9, 1000, 3, now() - interval '12 days'),
    ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 2,
     'Prune audit rows on a schedule',
     'Nothing purges audit_logs. Same housekeeping bean as the refresh tokens.',
     'TODO', 'MEDIUM', 'TASK', NULL, '00000000-0000-4000-8000-000000000001',
     NULL, 2000, 2, now() - interval '11 days'),
    ('30000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 3,
     'Expose the dashboard window in the UI',
     'windowDays is a query parameter the API supports and the dashboard never offers. Fixed at 30 days today.',
     'TODO', 'LOW', 'IMPROVEMENT', '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002',
     NULL, 3000, 2, now() - interval '9 days'),
    ('30000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 4,
     'Board columns do not virtualise',
     'A column caps at 100 cards so nothing is unbounded today, but the cap is doing the work a virtual list should.',
     'TODO', 'LOW', 'IMPROVEMENT', NULL, '00000000-0000-4000-8000-000000000002',
     NULL, 4000, 5, now() - interval '8 days'),
    ('30000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 5,
     'Filter state should live in the URL',
     'A filtered issue list cannot be linked or restored by a refresh. useSearchParams is the drop-in.',
     'TODO', 'MEDIUM', 'IMPROVEMENT', '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000004',
     current_date + 14, 5000, 3, now() - interval '6 days'),
    ('30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', 6,
     'IssueResponse carries no comment count',
     'The frontend needs a second call to show a count on a card. Copy the batched-count pattern from ProjectService.countMembers.',
     'TODO', 'LOW', 'IMPROVEMENT', NULL, '00000000-0000-4000-8000-000000000003',
     NULL, 6000, 1, now() - interval '5 days'),

    -- IN_PROGRESS
    ('30000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', 7,
     'Playwright coverage for drag and drop',
     'dnd-kit measures layout and jsdom gives every element a zero rect, so a simulated drag never starts. Only a real browser closes this.',
     'IN_PROGRESS', 'HIGH', 'TASK', '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001',
     current_date + 4, 1000, 5, now() - interval '15 days'),
    ('30000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000001', 8,
     'Cut the API image with jlink',
     'The runtime image is 439 MB, almost all of it the full JRE. A module set derived from jdeps would cut it substantially.',
     'IN_PROGRESS', 'MEDIUM', 'IMPROVEMENT', '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001',
     NULL, 2000, 5, now() - interval '10 days'),
    ('30000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000001', 9,
     'Nothing prunes deployment history',
     'A project dashboard caps its window at a year, but the table grows without limit.',
     'IN_PROGRESS', 'MEDIUM', 'TASK', '00000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000003',
     NULL, 3000, 3, now() - interval '7 days'),

    -- IN_REVIEW
    ('30000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000001', 10,
     'Cancelled deployments lose their reason',
     'transitionTo only stored failureReason for FAILED, so a release cancelled after an approval timeout recorded no explanation.',
     'IN_REVIEW', 'HIGH', 'BUG', '00000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001',
     current_date + 2, 1000, 2, now() - interval '4 days'),
    ('30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', 11,
     'Add myRole to ProjectResponse',
     'ProjectLayout fetches the member list on every project route to re-derive a rule the server already knows.',
     'IN_REVIEW', 'MEDIUM', 'IMPROVEMENT', '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002',
     NULL, 2000, 3, now() - interval '3 days'),

    -- DONE
    ('30000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000001', 12,
     'Approval gate for production releases',
     'An environment marked requiresApproval refuses to be started by anyone but a project administrator. CI queues the release and polls.',
     'DONE', 'CRITICAL', 'FEATURE', '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001',
     NULL, 1000, 8, now() - interval '30 days'),
    ('30000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000001', 13,
     'API keys were generated with a base64url alphabet',
     'The alphabet contains the underscore used as the key''s own separator, so roughly half of all issued keys failed to parse and returned 401. Keys are hex now.',
     'DONE', 'CRITICAL', 'BUG', '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000004',
     NULL, 2000, 3, now() - interval '28 days'),
    ('30000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000001', 14,
     'Deleting a project failed when an environment had deployments',
     'Cascades fire in constraint-creation order, so environments went first and ON DELETE RESTRICT fired while deployments still referenced them. V2 changes that FK to NO ACTION.',
     'DONE', 'HIGH', 'BUG', '00000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001',
     NULL, 3000, 5, now() - interval '26 days'),
    ('30000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000001', 15,
     'A board drag posts its neighbours, not a position',
     'The server owns the ordering, so two clients dragging at once cannot invent conflicting positions and a drag stays one UPDATE.',
     'DONE', 'HIGH', 'FEATURE', '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     NULL, 4000, 8, now() - interval '22 days'),
    ('30000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000001', 16,
     'Single-flight refresh for concurrent 401s',
     'Refresh tokens rotate and a replay revokes the whole family, so five requests hitting an expired token must send exactly one refresh.',
     'DONE', 'HIGH', 'FEATURE', '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     NULL, 5000, 5, now() - interval '20 days'),
    ('30000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000001', 17,
     'Audit read API',
     'Writing an entry stays a transaction-joining side effect, and reading one is an ordinary query, so AuditQueryService is separate from AuditService.',
     'DONE', 'MEDIUM', 'FEATURE', '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001',
     NULL, 6000, 5, now() - interval '18 days'),
    ('30000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000001', 18,
     'Container images for the API and the web app',
     'Boot jar exploded into its layers, so a code-only release rebuilds about 1 MB rather than the 67 MB of dependencies underneath it.',
     'DONE', 'HIGH', 'TASK', '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001',
     NULL, 7000, 5, now() - interval '14 days'),

    -- Marketing site
    ('30000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000002', 1,
     'Rewrite the landing page copy', NULL, 'IN_PROGRESS', 'HIGH', 'TASK',
     '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', NULL, 1000, 3, now() - interval '20 days'),
    ('30000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000002', 2,
     'Dark mode for the docs theme', NULL, 'TODO', 'MEDIUM', 'FEATURE',
     '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', NULL, 1000, 5, now() - interval '18 days'),
    ('30000000-0000-4000-8000-000000000021', '10000000-0000-4000-8000-000000000002', 3,
     'Broken anchor links in the API reference', NULL, 'TODO', 'LOW', 'BUG',
     NULL, '00000000-0000-4000-8000-000000000003', NULL, 2000, 1, now() - interval '15 days'),
    ('30000000-0000-4000-8000-000000000022', '10000000-0000-4000-8000-000000000002', 4,
     'Add an OG image to every page', NULL, 'DONE', 'LOW', 'IMPROVEMENT',
     '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', NULL, 1000, 2, now() - interval '12 days'),
    ('30000000-0000-4000-8000-000000000023', '10000000-0000-4000-8000-000000000002', 5,
     'Lighthouse score dropped below 90', NULL, 'IN_REVIEW', 'MEDIUM', 'BUG',
     '00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', current_date + 5, 1000, 3, now() - interval '6 days');

INSERT INTO issue_labels (issue_id, label_id)
VALUES
    ('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000005'),
    ('30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000003'),
    ('30000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000003'),
    ('30000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000004'),
    ('30000000-0000-4000-8000-000000000013', '20000000-0000-4000-8000-000000000004'),
    ('30000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000015', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000004'),
    ('30000000-0000-4000-8000-000000000017', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000018', '20000000-0000-4000-8000-000000000003'),
    ('30000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000006'),
    ('30000000-0000-4000-8000-000000000020', '20000000-0000-4000-8000-000000000007');

INSERT INTO comments (id, issue_id, author_id, body, created_at)
VALUES
    ('60000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000010',
     '00000000-0000-4000-8000-000000000001',
     'Caught this while wiring the approval gate. A pipeline that gives up waiting is the commonest way a release ends CANCELLED, and it was recording nothing about why.',
     now() - interval '4 days'),
    ('60000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000010',
     '00000000-0000-4000-8000-000000000004',
     'Storing it under failure_reason is a slight misnomer, but the column is nullable text and a second one would be worse. Fine by me.',
     now() - interval '3 days'),
    ('60000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000012',
     '00000000-0000-4000-8000-000000000002',
     'Worth noting the pipeline never learns which environments are gated. It tries the transition, takes the 403, and polls. Turning the gate on for staging needs no pipeline change.',
     now() - interval '29 days'),
    ('60000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000013',
     '00000000-0000-4000-8000-000000000004',
     'The nasty part was that it was probabilistic. Single-key tests passed or failed by luck and two were green for the wrong reason.',
     now() - interval '27 days'),
    ('60000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000013',
     '00000000-0000-4000-8000-000000000001',
     'Added a test that issues ten keys and pins the format, so a regression cannot hide in the noise.',
     now() - interval '27 days'),
    ('60000000-0000-4000-8000-000000000006', '30000000-0000-4000-8000-000000000007',
     '00000000-0000-4000-8000-000000000002',
     'Blocked on getting the compose stack running in CI. The ordering arithmetic is unit-tested, it is the wiring to dnd-kit that is uncovered.',
     now() - interval '5 days');

INSERT INTO environments (id, project_id, name, type, url, requires_approval, created_at)
VALUES
    ('40000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'development', 'DEVELOPMENT', 'http://localhost:8081', FALSE, now() - interval '60 days'),
    ('40000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'staging', 'STAGING', 'https://staging.devflow.example.com', FALSE, now() - interval '60 days'),
    -- The gate the pipeline actually hits.
    ('40000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'production', 'PRODUCTION', 'https://devflow.example.com', TRUE, now() - interval '60 days'),
    ('40000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000002', 'staging', 'STAGING', 'https://staging.example.com', FALSE, now() - interval '40 days'),
    ('40000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000002', 'production', 'PRODUCTION', 'https://example.com', TRUE, now() - interval '40 days');

-- Routine staging releases, one every day or two, so the dashboard's 30-day charts have shape.
-- Generated rather than listed: the interesting deployments are spelled out separately below.
INSERT INTO deployments (id, project_id, environment_id, release_version, commit_hash,
                         commit_message, branch, status, triggered_by_id, triggered_by_label,
                         pipeline_url, queued_at, started_at, finished_at, duration_seconds, created_at)
SELECT
    ('50000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-4000-8000-000000000001',
    '40000000-0000-4000-8000-000000000002',
    (120 + n) || '-' || substr(md5(n::text), 1, 7),
    substr(md5(n::text || 'commit'), 1, 40),
    (ARRAY[
        'Tighten the board ordering test',
        'Bump springdoc to 3.1.1',
        'Drop the dead AuditLogRepository.search',
        'Batch the label fetch on the board',
        'Correct the readiness probe path',
        'Add a sort whitelist to the issue listing',
        'Use PathPatternRequestMatcher in the security chain',
        'Cache the members query on the project shell'
    ])[1 + (n % 8)],
    'develop',
    CASE WHEN n % 9 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
    NULL,
    'api-key:github-actions',
    'https://ci.example.com/devflow/actions/runs/' || (120 + n),
    now() - make_interval(days => 29 - n, hours => 3),
    now() - make_interval(days => 29 - n, hours => 3) + interval '20 seconds',
    now() - make_interval(days => 29 - n, hours => 3) + interval '20 seconds' + make_interval(secs => 95 + (n % 7) * 20),
    95 + (n % 7) * 20,
    now() - make_interval(days => 29 - n, hours => 3)
FROM generate_series(1, 24) AS n;

INSERT INTO deployments (id, project_id, environment_id, release_version, commit_hash,
                         commit_message, branch, status, triggered_by_id, triggered_by_label,
                         pipeline_url, failure_reason, queued_at, started_at, finished_at,
                         duration_seconds, created_at)
VALUES
    -- Production releases, each one started by a person because the environment is gated.
    ('50000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000001',
     '40000000-0000-4000-8000-000000000003', '118-4c1d9ab', '4c1d9ab77f0e3b2a95c6d4e8f1a0b3c5d7e9f2a4',
     'Release 1.4.0', 'main', 'SUCCESS', '00000000-0000-4000-8000-000000000001', 'demo',
     'https://ci.example.com/devflow/actions/runs/118', NULL,
     now() - interval '21 days', now() - interval '21 days' + interval '11 minutes',
     now() - interval '21 days' + interval '14 minutes', 180, now() - interval '21 days'),
    ('50000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000001',
     '40000000-0000-4000-8000-000000000003', '131-9f3c0de', '9f3c0de11a4b6c8d2e5f7a9b0c3d6e8f1a2b4c5d',
     'Release 1.5.0', 'main', 'SUCCESS', '00000000-0000-4000-8000-000000000001', 'demo',
     'https://ci.example.com/devflow/actions/runs/131', NULL,
     now() - interval '9 days', now() - interval '9 days' + interval '6 minutes',
     now() - interval '9 days' + interval '9 minutes', 174, now() - interval '9 days'),
    -- A real failure, with the reason the pipeline reported.
    ('50000000-0000-4000-8000-000000000103', '10000000-0000-4000-8000-000000000001',
     '40000000-0000-4000-8000-000000000003', '134-2a7e5bc', '2a7e5bc33d6f9a1c4e7b0d2f5a8c1e4b7d0f3a6c',
     'Release 1.5.1', 'main', 'FAILED', '00000000-0000-4000-8000-000000000004', 'priya',
     'https://ci.example.com/devflow/actions/runs/134',
     'Smoke test failed: deep link served by the SPA expected 200, got 404',
     now() - interval '5 days', now() - interval '5 days' + interval '4 minutes',
     now() - interval '5 days' + interval '7 minutes', 168, now() - interval '5 days'),
    -- Nobody approved this one in time, so the pipeline cancelled it and failed the build.
    ('50000000-0000-4000-8000-000000000104', '10000000-0000-4000-8000-000000000001',
     '40000000-0000-4000-8000-000000000003', '135-8b2d4fa', '8b2d4fa55e8a0c3d6f9b2e5a8d1c4f7b0e3a6d9c',
     'Release 1.5.2', 'main', 'CANCELLED', NULL, 'api-key:github-actions',
     'https://ci.example.com/devflow/actions/runs/135',
     'No approval within 1800s',
     now() - interval '3 days', NULL, NULL, NULL, now() - interval '3 days'),
    -- Queued and waiting. Sign in as demo and approve it from the Deployments tab.
    ('50000000-0000-4000-8000-000000000105', '10000000-0000-4000-8000-000000000001',
     '40000000-0000-4000-8000-000000000003', '136-6d9a1cf', '6d9a1cf77b0e3a5c8f1d4b7e0a3c6f9b2d5e8a1c',
     'Release 1.5.3', 'main', 'PENDING', NULL, 'api-key:github-actions',
     'https://ci.example.com/devflow/actions/runs/136', NULL,
     now() - interval '25 minutes', NULL, NULL, NULL, now() - interval '25 minutes'),
    -- Marketing site
    ('50000000-0000-4000-8000-000000000106', '10000000-0000-4000-8000-000000000002',
     '40000000-0000-4000-8000-000000000004', '42-c3f8b1d', 'c3f8b1d99a2c5e8f0b3d6a9c2e5f8b1d4a7c0e3f',
     'Docs restructure', 'develop', 'SUCCESS', NULL, 'api-key:github-actions',
     'https://ci.example.com/marketing/actions/runs/42', NULL,
     now() - interval '6 days', now() - interval '6 days' + interval '10 seconds',
     now() - interval '6 days' + interval '52 seconds', 42, now() - interval '6 days'),
    ('50000000-0000-4000-8000-000000000107', '10000000-0000-4000-8000-000000000002',
     '40000000-0000-4000-8000-000000000005', '43-e7a2d5b', 'e7a2d5b11c4f7a0d3e6b9c2f5a8d1e4b7c0f3a6d',
     'Launch the pricing page', 'main', 'SUCCESS', '00000000-0000-4000-8000-000000000002', 'alice',
     'https://ci.example.com/marketing/actions/runs/43', NULL,
     now() - interval '2 days', now() - interval '2 days' + interval '3 minutes',
     now() - interval '2 days' + interval '4 minutes', 61, now() - interval '2 days');

-- The secret behind this hash was never generated, so the key cannot authenticate. It exists so
-- the API key panel has something to show.
INSERT INTO api_keys (id, name, key_prefix, key_hash, project_id, scopes, created_by_id,
                      last_used_at, created_at)
VALUES
    ('70000000-0000-4000-8000-000000000001', 'github-actions', 'a3f91c7d0e42',
     '0000000000000000000000000000000000000000000000000000000000000000',
     '10000000-0000-4000-8000-000000000001', 'deployment:write',
     '00000000-0000-4000-8000-000000000001', now() - interval '25 minutes', now() - interval '58 days');

INSERT INTO audit_logs (id, actor_id, actor_label, action, entity_type, entity_id, project_id,
                        summary, metadata, created_at)
VALUES
    ('80000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', 'demo', 'PROJECT_CREATED', 'Project', '10000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Created project DevFlow Platform', '{"projectKey": "DF"}', now() - interval '90 days'),
    ('80000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001', 'demo', 'MEMBER_ADDED', 'ProjectMember', '00000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Added alice as DEVELOPER', '{"role": "DEVELOPER"}', now() - interval '88 days'),
    ('80000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001', 'demo', 'ENVIRONMENT_CREATED', 'Environment', '40000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'Created environment production', '{"requiresApproval": true, "type": "PRODUCTION"}', now() - interval '60 days'),
    ('80000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001', 'demo', 'API_KEY_CREATED', 'ApiKey', '70000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Issued API key github-actions', '{"scopes": "deployment:write"}', now() - interval '58 days'),
    ('80000000-0000-4000-8000-000000000005', '00000000-0000-4000-8000-000000000002', 'alice', 'ISSUE_CREATED', 'Issue', '30000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000001', 'Created DF-15', '{"type": "FEATURE", "priority": "HIGH"}', now() - interval '22 days'),
    ('80000000-0000-4000-8000-000000000006', '00000000-0000-4000-8000-000000000001', 'demo', 'DEPLOYMENT_STARTED', 'Deployment', '50000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000001', 'Started 118-4c1d9ab to production', '{"statusTo": "RUNNING", "environment": "production", "statusFrom": "PENDING"}', now() - interval '21 days'),
    ('80000000-0000-4000-8000-000000000007', '00000000-0000-4000-8000-000000000001', 'demo', 'DEPLOYMENT_SUCCEEDED', 'Deployment', '50000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000001', 'Deployed 118-4c1d9ab to production', '{"environment": "production", "durationSeconds": 180}', now() - interval '21 days'),
    ('80000000-0000-4000-8000-000000000008', '00000000-0000-4000-8000-000000000004', 'priya', 'ISSUE_UPDATED', 'Issue', '30000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000001', 'Updated DF-14', '{"status": "DONE"}', now() - interval '26 days'),
    ('80000000-0000-4000-8000-000000000009', NULL, 'api-key:github-actions', 'DEPLOYMENT_CREATED', 'Deployment', '50000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000001', 'Queued 131-9f3c0de to production', '{"branch": "main", "environment": "production"}', now() - interval '9 days'),
    ('80000000-0000-4000-8000-000000000010', '00000000-0000-4000-8000-000000000001', 'demo', 'DEPLOYMENT_SUCCEEDED', 'Deployment', '50000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000001', 'Deployed 131-9f3c0de to production', '{"environment": "production", "durationSeconds": 174}', now() - interval '9 days'),
    ('80000000-0000-4000-8000-000000000011', '00000000-0000-4000-8000-000000000004', 'priya', 'DEPLOYMENT_FAILED', 'Deployment', '50000000-0000-4000-8000-000000000103', '10000000-0000-4000-8000-000000000001', 'Failed deploying 134-2a7e5bc to production', '{"environment": "production", "statusTo": "FAILED"}', now() - interval '5 days'),
    ('80000000-0000-4000-8000-000000000012', NULL, 'api-key:github-actions', 'DEPLOYMENT_CANCELLED', 'Deployment', '50000000-0000-4000-8000-000000000104', '10000000-0000-4000-8000-000000000001', 'Cancelled 135-8b2d4fa to production', '{"environment": "production", "statusTo": "CANCELLED"}', now() - interval '3 days'),
    ('80000000-0000-4000-8000-000000000013', '00000000-0000-4000-8000-000000000003', 'marco', 'COMMENT_CREATED', 'Comment', '60000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', 'Commented on DF-7', '{}', now() - interval '5 days'),
    ('80000000-0000-4000-8000-000000000014', NULL, 'api-key:github-actions', 'DEPLOYMENT_CREATED', 'Deployment', '50000000-0000-4000-8000-000000000105', '10000000-0000-4000-8000-000000000001', 'Queued 136-6d9a1cf to production', '{"branch": "main", "environment": "production"}', now() - interval '25 minutes');
