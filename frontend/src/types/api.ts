/**
 * The wire contract, mirroring the backend DTOs one for one.
 *
 * The backend runs with `spring.jackson.default-property-inclusion: non_null`, so a null field
 * is left out of the payload entirely rather than sent as null. Nullable response fields are
 * therefore optional AND nullable: read them with `??` or a truthiness check, never `=== null`.
 */

export type Uuid = string;
/** ISO-8601 instant, e.g. 2026-09-12T09:41:07.213Z */
export type Timestamp = string;
/** ISO-8601 local date, e.g. 2026-09-12 */
export type DateOnly = string;

export const ROLES = ['ADMIN', 'DEVELOPER', 'VIEWER'] as const;
export type Role = (typeof ROLES)[number];

export const PROJECT_STATUSES = ['ACTIVE', 'ARCHIVED'] as const;
export type ProjectStatus = (typeof PROJECT_STATUSES)[number];

export const ISSUE_STATUSES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'] as const;
export type IssueStatus = (typeof ISSUE_STATUSES)[number];

export const ISSUE_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
export type IssuePriority = (typeof ISSUE_PRIORITIES)[number];

export const ISSUE_TYPES = ['TASK', 'BUG', 'FEATURE', 'IMPROVEMENT'] as const;
export type IssueType = (typeof ISSUE_TYPES)[number];

export const ENVIRONMENT_TYPES = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'] as const;
export type EnvironmentType = (typeof ENVIRONMENT_TYPES)[number];

export const DEPLOYMENT_STATUSES = [
  'PENDING',
  'RUNNING',
  'SUCCESS',
  'FAILED',
  'CANCELLED',
] as const;
export type DeploymentStatus = (typeof DEPLOYMENT_STATUSES)[number];

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserSummary {
  id: Uuid;
  username: string;
  fullName: string;
  avatarColor: string;
}

export interface UserResponse {
  id: Uuid;
  email: string;
  username: string;
  fullName: string;
  role: Role;
  avatarColor: string;
  active: boolean;
  createdAt: Timestamp;
  lastLoginAt?: Timestamp | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserResponse;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  fullName: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UpdateProfileRequest {
  fullName: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateUserRequest {
  fullName: string;
  role: Role;
  active: boolean;
}

export interface ProjectSummary {
  id: Uuid;
  projectKey: string;
  name: string;
  status: ProjectStatus;
}

export interface ProjectResponse {
  id: Uuid;
  projectKey: string;
  name: string;
  description?: string | null;
  repositoryUrl?: string | null;
  status: ProjectStatus;
  owner: UserSummary;
  memberCount: number;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface CreateProjectRequest {
  projectKey: string;
  name: string;
  description?: string | null;
  repositoryUrl?: string | null;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string | null;
  repositoryUrl?: string | null;
  status: ProjectStatus;
  ownerId?: Uuid | null;
}

export interface ProjectMemberResponse {
  id: Uuid;
  user: UserSummary;
  role: Role;
  owner: boolean;
  createdAt: Timestamp;
}

export interface AddMemberRequest {
  userId: Uuid;
  role: Role;
}

export interface UpdateMemberRoleRequest {
  role: Role;
}

export interface LabelResponse {
  id: Uuid;
  name: string;
  color: string;
}

export interface LabelRequest {
  name: string;
  color: string;
}

export interface IssueSummary {
  id: Uuid;
  key: string;
  title: string;
  status: IssueStatus;
  priority: IssuePriority;
  type: IssueType;
  assignee?: UserSummary | null;
  dueDate?: DateOnly | null;
  boardPosition: number;
  estimatePoints?: number | null;
  labels: LabelResponse[];
}

export interface IssueResponse {
  id: Uuid;
  key: string;
  issueNumber: number;
  project: ProjectSummary;
  title: string;
  description?: string | null;
  status: IssueStatus;
  priority: IssuePriority;
  type: IssueType;
  assignee?: UserSummary | null;
  creator?: UserSummary | null;
  dueDate?: DateOnly | null;
  boardPosition: number;
  estimatePoints?: number | null;
  labels: LabelResponse[];
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface CreateIssueRequest {
  title: string;
  description?: string | null;
  status?: IssueStatus | null;
  priority?: IssuePriority | null;
  type?: IssueType | null;
  assigneeId?: Uuid | null;
  dueDate?: DateOnly | null;
  estimatePoints?: number | null;
  labelIds?: Uuid[] | null;
}

/** A full replacement: an omitted assignee, due date or label list clears that field. */
export interface UpdateIssueRequest {
  title: string;
  description?: string | null;
  status: IssueStatus;
  priority: IssuePriority;
  type: IssueType;
  assigneeId?: Uuid | null;
  dueDate?: DateOnly | null;
  estimatePoints?: number | null;
  labelIds?: Uuid[] | null;
}

/** Both neighbours are null when a card is dropped into an empty column. */
export interface MoveIssueRequest {
  status: IssueStatus;
  previousIssueId: Uuid | null;
  nextIssueId: Uuid | null;
}

export interface BoardColumn {
  status: IssueStatus;
  total: number;
  issues: IssueSummary[];
}

export interface BoardResponse {
  project: ProjectSummary;
  columns: BoardColumn[];
}

export interface CommentResponse {
  id: Uuid;
  author: UserSummary;
  body: string;
  edited: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface CommentRequest {
  body: string;
}

export interface EnvironmentResponse {
  id: Uuid;
  name: string;
  type: EnvironmentType;
  url?: string | null;
  requiresApproval: boolean;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface EnvironmentRequest {
  name: string;
  type: EnvironmentType;
  url?: string | null;
  requiresApproval?: boolean | null;
}

export interface DeploymentResponse {
  id: Uuid;
  project: ProjectSummary;
  environment: EnvironmentResponse;
  releaseVersion: string;
  commitHash: string;
  commitMessage?: string | null;
  branch: string;
  status: DeploymentStatus;
  triggeredBy?: UserSummary | null;
  triggeredByLabel?: string | null;
  pipelineUrl?: string | null;
  failureReason?: string | null;
  queuedAt: Timestamp;
  startedAt?: Timestamp | null;
  finishedAt?: Timestamp | null;
  durationSeconds?: number | null;
}

export interface CreateDeploymentRequest {
  projectId: Uuid;
  environmentName: string;
  releaseVersion: string;
  commitHash: string;
  commitMessage?: string | null;
  branch: string;
  pipelineUrl?: string | null;
}

export interface DeploymentStatusRequest {
  status: DeploymentStatus;
  failureReason?: string | null;
}

export interface ApiKeyResponse {
  id: Uuid;
  name: string;
  keyPrefix: string;
  scopes: string[];
  createdBy?: UserSummary | null;
  lastUsedAt?: Timestamp | null;
  expiresAt?: Timestamp | null;
  revokedAt?: Timestamp | null;
  active: boolean;
  createdAt: Timestamp;
}

export interface CreateApiKeyRequest {
  name: string;
  expiresInDays?: number | null;
}

/** The only response that ever carries the key itself; it cannot be recovered later. */
export interface IssuedApiKeyResponse {
  apiKey: ApiKeyResponse;
  key: string;
}

export interface DashboardResponse {
  projects: {
    total: number;
    active: number;
    archived: number;
  };
  issues: {
    total: number;
    byStatus: Partial<Record<IssueStatus, number>>;
    assignedToMe: number;
    overdue: number;
  };
  deployments: {
    windowDays: number;
    total: number;
    byStatus: Partial<Record<DeploymentStatus, number>>;
    /** Null rather than zero when nothing finished in the window. */
    successRate?: number | null;
    recent: DeploymentResponse[];
  };
}

export type AuditAction =
  | 'USER_CREATED'
  | 'USER_UPDATED'
  | 'USER_LOGGED_IN'
  | 'PROJECT_CREATED'
  | 'PROJECT_UPDATED'
  | 'PROJECT_DELETED'
  | 'MEMBER_ADDED'
  | 'MEMBER_REMOVED'
  | 'MEMBER_ROLE_CHANGED'
  | 'ISSUE_CREATED'
  | 'ISSUE_UPDATED'
  | 'ISSUE_DELETED'
  | 'COMMENT_CREATED'
  | 'COMMENT_DELETED'
  | 'ENVIRONMENT_CREATED'
  | 'ENVIRONMENT_DELETED'
  | 'DEPLOYMENT_CREATED'
  | 'DEPLOYMENT_STARTED'
  | 'DEPLOYMENT_SUCCEEDED'
  | 'DEPLOYMENT_FAILED'
  | 'DEPLOYMENT_CANCELLED'
  | 'API_KEY_CREATED'
  | 'API_KEY_REVOKED';

export interface AuditLogResponse {
  id: Uuid;
  actorId?: Uuid | null;
  actorLabel?: string | null;
  action: AuditAction;
  entityType?: string | null;
  entityId?: Uuid | null;
  projectId?: Uuid | null;
  summary?: string | null;
  metadata?: Record<string, unknown> | null;
  createdAt: Timestamp;
}

/** RFC 9457, as produced by GlobalExceptionHandler. */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp?: string;
  errors?: { field: string; message: string }[];
  traceId?: string;
}
