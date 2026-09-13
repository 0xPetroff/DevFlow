import { HttpResponse, http } from 'msw';

import {
  auditEntry,
  authResponse,
  deployment,
  page,
  problemDetail,
  stagingEnvironment,
  testApiKey,
  testBoard,
  testComment,
  testDashboard,
  testIssue,
  testMembers,
  testProject,
  testUser,
} from '@/test/fixtures';

export const API = '*/api';

/** The happy path for every endpoint the app touches. Cases override what they care about. */
export const handlers = [
  http.post(`${API}/auth/login`, () => HttpResponse.json(authResponse())),

  http.post(`${API}/auth/register`, () => HttpResponse.json(authResponse(), { status: 201 })),

  http.post(`${API}/auth/refresh`, () =>
    HttpResponse.json(
      authResponse({ accessToken: 'access-token-2', refreshToken: 'refresh-token-2' }),
    ),
  ),

  http.post(`${API}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  http.post(`${API}/auth/logout-all`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${API}/auth/me`, ({ request }) =>
    request.headers.get('Authorization')
      ? HttpResponse.json(testUser)
      : HttpResponse.json(problemDetail(401, 'Invalid credentials', 'authentication-failed'), {
          status: 401,
        }),
  ),

  http.get(`${API}/dashboard`, () => HttpResponse.json(testDashboard)),

  http.get(`${API}/users`, () => HttpResponse.json(page([testUser]))),

  http.get(`${API}/audit-logs`, () => HttpResponse.json(page([auditEntry]))),

  http.get(`${API}/projects`, () => HttpResponse.json(page([testProject]))),
  http.post(`${API}/projects`, () => HttpResponse.json(testProject, { status: 201 })),
  http.get(`${API}/projects/:projectId`, () => HttpResponse.json(testProject)),
  http.put(`${API}/projects/:projectId`, () => HttpResponse.json(testProject)),
  http.delete(`${API}/projects/:projectId`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${API}/projects/:projectId/members`, () => HttpResponse.json(testMembers)),
  http.post(`${API}/projects/:projectId/members`, () =>
    HttpResponse.json(testMembers[1], { status: 201 }),
  ),
  http.put(`${API}/projects/:projectId/members/:userId`, () => HttpResponse.json(testMembers[1])),
  http.delete(
    `${API}/projects/:projectId/members/:userId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${API}/projects/:projectId/board`, () => HttpResponse.json(testBoard)),
  http.get(`${API}/projects/:projectId/issues`, () => HttpResponse.json(page([testIssue]))),
  http.post(`${API}/projects/:projectId/issues`, () => HttpResponse.json(testIssue, { status: 201 })),
  http.get(`${API}/projects/:projectId/labels`, () => HttpResponse.json([])),

  http.get(`${API}/issues/:issueId`, () => HttpResponse.json(testIssue)),
  http.put(`${API}/issues/:issueId`, () => HttpResponse.json(testIssue)),
  http.put(`${API}/issues/:issueId/position`, () => HttpResponse.json(testIssue)),
  http.delete(`${API}/issues/:issueId`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${API}/issues/:issueId/comments`, () => HttpResponse.json(page([testComment]))),
  http.post(`${API}/issues/:issueId/comments`, () =>
    HttpResponse.json(testComment, { status: 201 }),
  ),
  http.put(`${API}/issues/:issueId/comments/:commentId`, () => HttpResponse.json(testComment)),
  http.delete(
    `${API}/issues/:issueId/comments/:commentId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${API}/projects/:projectId/environments`, () => HttpResponse.json([stagingEnvironment])),
  http.post(`${API}/projects/:projectId/environments`, () =>
    HttpResponse.json(stagingEnvironment, { status: 201 }),
  ),
  http.put(`${API}/projects/:projectId/environments/:environmentId`, () =>
    HttpResponse.json(stagingEnvironment),
  ),
  http.delete(
    `${API}/projects/:projectId/environments/:environmentId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${API}/projects/:projectId/deployments`, () => HttpResponse.json(page([deployment()]))),
  http.post(`${API}/deployments`, () =>
    HttpResponse.json(deployment({ status: 'PENDING' }), { status: 201 }),
  ),
  http.put(`${API}/deployments/:deploymentId/status`, () =>
    HttpResponse.json(deployment({ status: 'RUNNING' })),
  ),

  http.get(`${API}/projects/:projectId/api-keys`, () => HttpResponse.json([testApiKey])),
  http.post(`${API}/projects/:projectId/api-keys`, () =>
    HttpResponse.json({ apiKey: testApiKey, key: 'a1b2c3d4e5f6_deadbeef' }, { status: 201 }),
  ),
  http.delete(
    `${API}/projects/:projectId/api-keys/:keyId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.get(`${API}/projects/:projectId/audit-logs`, () => HttpResponse.json(page([auditEntry]))),
];
