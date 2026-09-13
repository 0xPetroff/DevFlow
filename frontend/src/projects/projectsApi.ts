import { http } from '@/lib/apiClient';
import type {
  AddMemberRequest,
  CreateProjectRequest,
  PageResponse,
  ProjectMemberResponse,
  ProjectResponse,
  ProjectStatus,
  UpdateMemberRoleRequest,
  UpdateProjectRequest,
  Uuid,
} from '@/types/api';

export interface ProjectListParams {
  q?: string | undefined;
  status?: ProjectStatus | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string | undefined;
}

export const projectsApi = {
  list: (params: ProjectListParams): Promise<PageResponse<ProjectResponse>> =>
    http.get('/projects', { params }),

  get: (projectId: Uuid): Promise<ProjectResponse> => http.get(`/projects/${projectId}`),

  create: (body: CreateProjectRequest): Promise<ProjectResponse> => http.post('/projects', body),

  update: (projectId: Uuid, body: UpdateProjectRequest): Promise<ProjectResponse> =>
    http.put(`/projects/${projectId}`, body),

  remove: (projectId: Uuid): Promise<void> => http.delete(`/projects/${projectId}`),

  members: (projectId: Uuid): Promise<ProjectMemberResponse[]> =>
    http.get(`/projects/${projectId}/members`),

  addMember: (projectId: Uuid, body: AddMemberRequest): Promise<ProjectMemberResponse> =>
    http.post(`/projects/${projectId}/members`, body),

  changeMemberRole: (
    projectId: Uuid,
    userId: Uuid,
    body: UpdateMemberRoleRequest,
  ): Promise<ProjectMemberResponse> => http.put(`/projects/${projectId}/members/${userId}`, body),

  removeMember: (projectId: Uuid, userId: Uuid): Promise<void> =>
    http.delete(`/projects/${projectId}/members/${userId}`),
};
