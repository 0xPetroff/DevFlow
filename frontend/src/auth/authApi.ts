import { http, plainClient } from '@/lib/apiClient';
import type {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  UpdateProfileRequest,
  UserResponse,
} from '@/types/api';

export const authApi = {
  login: (body: LoginRequest): Promise<AuthResponse> =>
    plainClient.post<AuthResponse>('/auth/login', body).then((response) => response.data),

  register: (body: RegisterRequest): Promise<AuthResponse> =>
    plainClient.post<AuthResponse>('/auth/register', body).then((response) => response.data),

  /** Authenticated: the access token identifies the caller, the body names the token to revoke. */
  logout: (refreshToken: string): Promise<void> =>
    http.postNoContent('/auth/logout', { refreshToken }),

  logoutEverywhere: (): Promise<void> => http.postNoContent('/auth/logout-all'),

  me: (): Promise<UserResponse> => http.get<UserResponse>('/auth/me'),

  updateProfile: (body: UpdateProfileRequest): Promise<UserResponse> =>
    http.put<UserResponse>('/users/me', body),

  changePassword: (body: ChangePasswordRequest): Promise<void> =>
    http.putNoContent('/users/me/password', body),
};
