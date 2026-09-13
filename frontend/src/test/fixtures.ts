import type {
  ApiKeyResponse,
  AuditLogResponse,
  AuthResponse,
  BoardResponse,
  CommentResponse,
  DashboardResponse,
  DeploymentResponse,
  EnvironmentResponse,
  IssueResponse,
  IssueSummary,
  PageResponse,
  ProjectMemberResponse,
  ProjectResponse,
  UserResponse,
} from '@/types/api';

export const testUser: UserResponse = {
  id: '6f1a2b3c-4d5e-4f60-8a71-92b3c4d5e6f7',
  email: 'ada@example.com',
  username: 'ada',
  fullName: 'Ada Lovelace',
  role: 'DEVELOPER',
  avatarColor: '#6366f1',
  active: true,
  createdAt: '2026-01-04T09:00:00Z',
  lastLoginAt: '2026-09-12T08:30:00Z',
};

export const testAdmin: UserResponse = {
  ...testUser,
  id: '11112222-3333-4444-8555-666677778888',
  email: 'grace@example.com',
  username: 'grace',
  fullName: 'Grace Hopper',
  role: 'ADMIN',
};

export function authResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'access-token-1',
    refreshToken: 'refresh-token-1',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    user: testUser,
    ...overrides,
  };
}

export const testDashboard: DashboardResponse = {
  projects: { total: 3, active: 2, archived: 1 },
  issues: { total: 42, byStatus: { TODO: 10, IN_PROGRESS: 8 }, assignedToMe: 5, overdue: 2 },
  deployments: {
    windowDays: 30,
    total: 12,
    byStatus: { SUCCESS: 9, FAILED: 1 },
    successRate: 0.9,
    recent: [],
  },
};

export function problemDetail(status: number, detail: string, type = 'business-rule') {
  return {
    type: `https://devflow.dev/problems/${type}`,
    title: 'Request failed',
    status,
    detail,
    instance: '/api/test',
    timestamp: '2026-09-12T10:00:00Z',
  };
}

export function page<T>(content: T[], overrides: Partial<PageResponse<T>> = {}): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  };
}

export const testOwner = {
  id: testUser.id,
  username: testUser.username,
  fullName: testUser.fullName,
  avatarColor: testUser.avatarColor,
};

export const testProject: ProjectResponse = {
  id: 'aaaa1111-2222-4333-8444-555566667777',
  projectKey: 'DEVF',
  name: 'DevFlow',
  description: 'The platform itself.',
  repositoryUrl: 'https://github.com/acme/devflow',
  status: 'ACTIVE',
  owner: testOwner,
  memberCount: 2,
  createdAt: '2026-02-01T09:00:00Z',
  updatedAt: '2026-09-10T09:00:00Z',
};

export const archivedProject: ProjectResponse = {
  ...testProject,
  id: 'bbbb1111-2222-4333-8444-555566667777',
  projectKey: 'OLD',
  name: 'Retired service',
  status: 'ARCHIVED',
};

/** The signed-in user owns the project, so they are its administrator. */
export const testMembers: ProjectMemberResponse[] = [
  {
    id: 'm1111111-2222-4333-8444-555566667777',
    user: testOwner,
    role: 'ADMIN',
    owner: true,
    createdAt: '2026-02-01T09:00:00Z',
  },
  {
    id: 'm2222222-2222-4333-8444-555566667777',
    user: {
      id: testAdmin.id,
      username: testAdmin.username,
      fullName: testAdmin.fullName,
      avatarColor: testAdmin.avatarColor,
    },
    role: 'DEVELOPER',
    owner: false,
    createdAt: '2026-02-02T09:00:00Z',
  },
];

/** The same list, but the signed-in user is only a viewer and someone else owns it. */
export const viewerMembers: ProjectMemberResponse[] = [
  {
    ...testMembers[0]!,
    user: {
      id: testAdmin.id,
      username: testAdmin.username,
      fullName: testAdmin.fullName,
      avatarColor: testAdmin.avatarColor,
    },
  },
  { ...testMembers[1]!, user: testOwner, role: 'VIEWER' },
];

/** Someone else owns the project and the signed-in user is a DEVELOPER on it. */
export const developerMembers: ProjectMemberResponse[] = [
  { ...viewerMembers[0]! },
  { ...viewerMembers[1]!, role: 'DEVELOPER' },
];

export const projectOwnedByAnother: ProjectResponse = {
  ...testProject,
  owner: {
    id: testAdmin.id,
    username: testAdmin.username,
    fullName: testAdmin.fullName,
    avatarColor: testAdmin.avatarColor,
  },
};

export function issueSummary(overrides: Partial<IssueSummary> = {}): IssueSummary {
  return {
    id: 'i1111111-2222-4333-8444-555566667777',
    key: 'DEVF-1',
    title: 'Wire the deploy hook',
    status: 'TODO',
    priority: 'HIGH',
    type: 'TASK',
    boardPosition: 1000,
    labels: [],
    ...overrides,
  };
}

export const testIssue: IssueResponse = {
  ...issueSummary(),
  issueNumber: 1,
  project: {
    id: testProject.id,
    projectKey: testProject.projectKey,
    name: testProject.name,
    status: testProject.status,
  },
  description: 'GitHub Actions should post to /api/deployments.',
  creator: testOwner,
  createdAt: '2026-09-01T09:00:00Z',
  updatedAt: '2026-09-02T09:00:00Z',
};

export const testBoard: BoardResponse = {
  project: testIssue.project,
  columns: [
    {
      status: 'TODO',
      total: 2,
      issues: [
        issueSummary(),
        issueSummary({ id: 'i2222222-2222-4333-8444-555566667777', key: 'DEVF-2', title: 'Add metrics' }),
      ],
    },
    { status: 'IN_PROGRESS', total: 0, issues: [] },
    { status: 'IN_REVIEW', total: 0, issues: [] },
    {
      status: 'DONE',
      total: 1,
      issues: [
        issueSummary({
          id: 'i3333333-2222-4333-8444-555566667777',
          key: 'DEVF-3',
          title: 'Ship the schema',
          status: 'DONE',
        }),
      ],
    },
  ],
};

export const testComment: CommentResponse = {
  id: 'c1111111-2222-4333-8444-555566667777',
  author: testOwner,
  body: 'The key needs deployment:write.',
  edited: false,
  createdAt: '2026-09-03T09:00:00Z',
  updatedAt: '2026-09-03T09:00:00Z',
};

export const stagingEnvironment: EnvironmentResponse = {
  id: 'e1111111-2222-4333-8444-555566667777',
  name: 'staging',
  type: 'STAGING',
  url: 'https://staging.example.com',
  requiresApproval: false,
  createdAt: '2026-02-01T09:00:00Z',
  updatedAt: '2026-02-01T09:00:00Z',
};

export const productionEnvironment: EnvironmentResponse = {
  ...stagingEnvironment,
  id: 'e2222222-2222-4333-8444-555566667777',
  name: 'production',
  type: 'PRODUCTION',
  url: 'https://example.com',
  requiresApproval: true,
};

export function deployment(overrides: Partial<DeploymentResponse> = {}): DeploymentResponse {
  return {
    id: 'd1111111-2222-4333-8444-555566667777',
    project: testIssue.project,
    environment: stagingEnvironment,
    releaseVersion: '1.4.0',
    commitHash: '9f2c1ab3d4e5',
    commitMessage: 'Wire the deploy hook',
    branch: 'main',
    status: 'SUCCESS',
    triggeredByLabel: 'github-actions',
    queuedAt: '2026-09-11T09:00:00Z',
    startedAt: '2026-09-11T09:01:00Z',
    finishedAt: '2026-09-11T09:04:00Z',
    durationSeconds: 180,
    ...overrides,
  };
}

export const testApiKey: ApiKeyResponse = {
  id: 'k1111111-2222-4333-8444-555566667777',
  name: 'github-actions',
  keyPrefix: 'a1b2c3d4e5f6',
  scopes: ['deployment:write'],
  createdBy: testOwner,
  lastUsedAt: '2026-09-11T09:00:00Z',
  active: true,
  createdAt: '2026-02-03T09:00:00Z',
};

export const auditEntry: AuditLogResponse = {
  id: 'g1111111-2222-4333-8444-555566667777',
  actorId: testUser.id,
  actorLabel: 'ada',
  action: 'ISSUE_CREATED',
  entityType: 'Issue',
  entityId: testIssue.id,
  projectId: testProject.id,
  summary: 'Created DEVF-1 Wire the deploy hook',
  createdAt: '2026-09-01T09:00:00Z',
};
