import type { Uuid } from '@/types/api';

/**
 * One place to derive cache keys, so an invalidation cannot miss a query by spelling.
 *
 * Everything project-scoped is nested under ['projects', projectId], which means a mutation can
 * invalidate a whole project with one prefix instead of naming each list it might have changed.
 */
export const queryKeys = {
  currentUser: ['auth', 'me'] as const,
  dashboard: (windowDays: number) => ['dashboard', { windowDays }] as const,

  users: (params: unknown) => ['users', params] as const,
  auditLogs: (params: unknown) => ['audit', params] as const,

  projects: ['projects'] as const,
  projectList: (params: unknown) => ['projects', 'list', params] as const,
  project: (projectId: Uuid) => ['projects', projectId] as const,
  projectMembers: (projectId: Uuid) => ['projects', projectId, 'members'] as const,
  projectLabels: (projectId: Uuid) => ['projects', projectId, 'labels'] as const,
  projectEnvironments: (projectId: Uuid) => ['projects', projectId, 'environments'] as const,
  projectApiKeys: (projectId: Uuid) => ['projects', projectId, 'api-keys'] as const,
  projectBoard: (projectId: Uuid) => ['projects', projectId, 'board'] as const,
  projectIssues: (projectId: Uuid, params: unknown) =>
    ['projects', projectId, 'issues', params] as const,
  projectDeployments: (projectId: Uuid, params: unknown) =>
    ['projects', projectId, 'deployments', params] as const,
  projectAudit: (projectId: Uuid, params: unknown) =>
    ['projects', projectId, 'audit', params] as const,

  issue: (issueId: Uuid) => ['issues', issueId] as const,
  issueComments: (issueId: Uuid) => ['issues', issueId, 'comments'] as const,
};
