import { http } from '@/lib/apiClient';
import type {
  ApiKeyResponse,
  CreateApiKeyRequest,
  CreateDeploymentRequest,
  DeploymentResponse,
  DeploymentStatus,
  DeploymentStatusRequest,
  EnvironmentRequest,
  EnvironmentResponse,
  IssuedApiKeyResponse,
  PageResponse,
  Uuid,
} from '@/types/api';

export interface DeploymentListParams {
  status?: DeploymentStatus[] | undefined;
  environmentId?: Uuid | undefined;
  branch?: string | undefined;
  q?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string | undefined;
}

export const deploymentsApi = {
  list: (
    projectId: Uuid,
    params: DeploymentListParams,
  ): Promise<PageResponse<DeploymentResponse>> =>
    http.get(`/projects/${projectId}/deployments`, { params }),

  create: (body: CreateDeploymentRequest): Promise<DeploymentResponse> =>
    http.post('/deployments', body),

  transition: (deploymentId: Uuid, body: DeploymentStatusRequest): Promise<DeploymentResponse> =>
    http.put(`/deployments/${deploymentId}/status`, body),

  environments: (projectId: Uuid): Promise<EnvironmentResponse[]> =>
    http.get(`/projects/${projectId}/environments`),

  createEnvironment: (projectId: Uuid, body: EnvironmentRequest): Promise<EnvironmentResponse> =>
    http.post(`/projects/${projectId}/environments`, body),

  updateEnvironment: (
    projectId: Uuid,
    environmentId: Uuid,
    body: EnvironmentRequest,
  ): Promise<EnvironmentResponse> =>
    http.put(`/projects/${projectId}/environments/${environmentId}`, body),

  removeEnvironment: (projectId: Uuid, environmentId: Uuid): Promise<void> =>
    http.delete(`/projects/${projectId}/environments/${environmentId}`),

  apiKeys: (projectId: Uuid): Promise<ApiKeyResponse[]> =>
    http.get(`/projects/${projectId}/api-keys`),

  createApiKey: (projectId: Uuid, body: CreateApiKeyRequest): Promise<IssuedApiKeyResponse> =>
    http.post(`/projects/${projectId}/api-keys`, body),

  revokeApiKey: (projectId: Uuid, keyId: Uuid): Promise<void> =>
    http.delete(`/projects/${projectId}/api-keys/${keyId}`),
};
