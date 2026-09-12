import { http } from '@/lib/apiClient';
import type {
  AuditAction,
  AuditLogResponse,
  PageResponse,
  Role,
  UpdateUserRequest,
  UserResponse,
  Uuid,
} from '@/types/api';

export interface UserListParams {
  q?: string | undefined;
  role?: Role | undefined;
  active?: boolean | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string | undefined;
}

export interface AuditListParams {
  actorId?: Uuid | undefined;
  action?: AuditAction | undefined;
  q?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string | undefined;
}

export const usersApi = {
  list: (params: UserListParams): Promise<PageResponse<UserResponse>> =>
    http.get('/users', { params }),

  update: (userId: Uuid, body: UpdateUserRequest): Promise<UserResponse> =>
    http.put(`/users/${userId}`, body),
};

export const auditApi = {
  list: (params: AuditListParams): Promise<PageResponse<AuditLogResponse>> =>
    http.get('/audit-logs', { params }),

  forProject: (projectId: Uuid, params: AuditListParams): Promise<PageResponse<AuditLogResponse>> =>
    http.get(`/projects/${projectId}/audit-logs`, { params }),
};
