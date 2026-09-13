import type {
  AuditAction,
  DeploymentStatus,
  EnvironmentType,
  IssuePriority,
  IssueStatus,
  IssueType,
  ProjectStatus,
} from '@/types/api';

/** The tones a Badge understands, so a domain map cannot name a colour the component lacks. */
export type Tone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'info';

export const ISSUE_STATUS_LABELS: Record<IssueStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  DONE: 'Done',
};

export const ISSUE_STATUS_TONES: Record<IssueStatus, Tone> = {
  TODO: 'neutral',
  IN_PROGRESS: 'info',
  IN_REVIEW: 'warning',
  DONE: 'success',
};

export const ISSUE_PRIORITY_LABELS: Record<IssuePriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
};

export const ISSUE_PRIORITY_TONES: Record<IssuePriority, Tone> = {
  LOW: 'neutral',
  MEDIUM: 'info',
  HIGH: 'warning',
  CRITICAL: 'danger',
};

export const ISSUE_TYPE_LABELS: Record<IssueType, string> = {
  TASK: 'Task',
  BUG: 'Bug',
  FEATURE: 'Feature',
  IMPROVEMENT: 'Improvement',
};

export const DEPLOYMENT_STATUS_LABELS: Record<DeploymentStatus, string> = {
  PENDING: 'Pending',
  RUNNING: 'Running',
  SUCCESS: 'Success',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled',
};

export const DEPLOYMENT_STATUS_TONES: Record<DeploymentStatus, Tone> = {
  PENDING: 'neutral',
  RUNNING: 'info',
  SUCCESS: 'success',
  FAILED: 'danger',
  CANCELLED: 'warning',
};

export const ENVIRONMENT_TYPE_LABELS: Record<EnvironmentType, string> = {
  DEVELOPMENT: 'Development',
  STAGING: 'Staging',
  PRODUCTION: 'Production',
};

export const ENVIRONMENT_TYPE_TONES: Record<EnvironmentType, Tone> = {
  DEVELOPMENT: 'neutral',
  STAGING: 'info',
  PRODUCTION: 'accent',
};

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  ACTIVE: 'Active',
  ARCHIVED: 'Archived',
};

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  USER_CREATED: 'User created',
  USER_UPDATED: 'User updated',
  USER_LOGGED_IN: 'User signed in',
  PROJECT_CREATED: 'Project created',
  PROJECT_UPDATED: 'Project updated',
  PROJECT_DELETED: 'Project deleted',
  MEMBER_ADDED: 'Member added',
  MEMBER_REMOVED: 'Member removed',
  MEMBER_ROLE_CHANGED: 'Member role changed',
  ISSUE_CREATED: 'Issue created',
  ISSUE_UPDATED: 'Issue updated',
  ISSUE_DELETED: 'Issue deleted',
  COMMENT_CREATED: 'Comment added',
  COMMENT_DELETED: 'Comment deleted',
  ENVIRONMENT_CREATED: 'Environment created',
  ENVIRONMENT_DELETED: 'Environment deleted',
  DEPLOYMENT_CREATED: 'Deployment queued',
  DEPLOYMENT_STARTED: 'Deployment started',
  DEPLOYMENT_SUCCEEDED: 'Deployment succeeded',
  DEPLOYMENT_FAILED: 'Deployment failed',
  DEPLOYMENT_CANCELLED: 'Deployment cancelled',
  API_KEY_CREATED: 'API key issued',
  API_KEY_REVOKED: 'API key revoked',
};

export function formatDuration(seconds: number | null | undefined): string {
  if (seconds == null) {
    return '--';
  }
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m ${seconds % 60}s`;
  }
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}
