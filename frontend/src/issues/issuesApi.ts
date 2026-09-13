import { http } from '@/lib/apiClient';
import type {
  BoardResponse,
  CommentRequest,
  CommentResponse,
  CreateIssueRequest,
  IssuePriority,
  IssueResponse,
  IssueStatus,
  IssueType,
  LabelRequest,
  LabelResponse,
  MoveIssueRequest,
  PageResponse,
  UpdateIssueRequest,
  Uuid,
} from '@/types/api';

export interface IssueListParams {
  status?: IssueStatus[] | undefined;
  priority?: IssuePriority[] | undefined;
  type?: IssueType[] | undefined;
  assigneeId?: Uuid | undefined;
  unassigned?: boolean | undefined;
  labelId?: Uuid | undefined;
  q?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string | undefined;
}

export const issuesApi = {
  list: (projectId: Uuid, params: IssueListParams): Promise<PageResponse<IssueResponse>> =>
    http.get(`/projects/${projectId}/issues`, { params }),

  board: (projectId: Uuid): Promise<BoardResponse> => http.get(`/projects/${projectId}/board`),

  create: (projectId: Uuid, body: CreateIssueRequest): Promise<IssueResponse> =>
    http.post(`/projects/${projectId}/issues`, body),

  get: (issueId: Uuid): Promise<IssueResponse> => http.get(`/issues/${issueId}`),

  update: (issueId: Uuid, body: UpdateIssueRequest): Promise<IssueResponse> =>
    http.put(`/issues/${issueId}`, body),

  move: (issueId: Uuid, body: MoveIssueRequest): Promise<IssueResponse> =>
    http.put(`/issues/${issueId}/position`, body),

  remove: (issueId: Uuid): Promise<void> => http.delete(`/issues/${issueId}`),

  labels: (projectId: Uuid): Promise<LabelResponse[]> => http.get(`/projects/${projectId}/labels`),

  createLabel: (projectId: Uuid, body: LabelRequest): Promise<LabelResponse> =>
    http.post(`/projects/${projectId}/labels`, body),

  updateLabel: (projectId: Uuid, labelId: Uuid, body: LabelRequest): Promise<LabelResponse> =>
    http.put(`/projects/${projectId}/labels/${labelId}`, body),

  removeLabel: (projectId: Uuid, labelId: Uuid): Promise<void> =>
    http.delete(`/projects/${projectId}/labels/${labelId}`),

  comments: (issueId: Uuid): Promise<PageResponse<CommentResponse>> =>
    http.get(`/issues/${issueId}/comments`),

  createComment: (issueId: Uuid, body: CommentRequest): Promise<CommentResponse> =>
    http.post(`/issues/${issueId}/comments`, body),

  updateComment: (issueId: Uuid, commentId: Uuid, body: CommentRequest): Promise<CommentResponse> =>
    http.put(`/issues/${issueId}/comments/${commentId}`, body),

  removeComment: (issueId: Uuid, commentId: Uuid): Promise<void> =>
    http.delete(`/issues/${issueId}/comments/${commentId}`),
};
