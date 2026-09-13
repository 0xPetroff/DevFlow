import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';

import { ApiError } from '@/lib/apiError';
import { env } from '@/lib/env';
import { sessionStore, type Session } from '@/lib/sessionStore';
import type { AuthResponse } from '@/types/api';

// The public demo's API is a free Render instance, which stops after 15 minutes without traffic.
// Waking it boots a JVM and runs Flyway: measured at about 55 seconds. A timeout shorter than
// that turns the first visit after an idle period into a hard failure instead of a slow load,
// and the login that a visitor starts with is a mutation, so it is not retried.
const REQUEST_TIMEOUT_MS = 90_000;

/** Refresh this far ahead of expiry so a request in flight cannot cross the boundary. */
const REFRESH_MARGIN_MS = 30_000;

interface RetriableConfig extends InternalAxiosRequestConfig {
  retriedAfterRefresh?: boolean;
}

/** For the auth endpoints themselves: it carries no token, so it can never recurse into a refresh. */
export const plainClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: REQUEST_TIMEOUT_MS,
});

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: REQUEST_TIMEOUT_MS,
  // Repeat the key for a list (status=TODO&status=DONE) rather than axios's default status[]=TODO,
  // which Spring does not bind to a List parameter and would drop on the floor without an error.
  paramsSerializer: { indexes: null },
});

let refreshInFlight: Promise<Session> | null = null;

/**
 * Deduplicated on purpose. Refresh tokens rotate and a replayed one revokes the whole family,
 * so a page that fires five requests into an expired token must send exactly one refresh and
 * let the other four wait for it.
 */
function refreshSession(): Promise<Session> {
  refreshInFlight ??= requestNewSession().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function requestNewSession(): Promise<Session> {
  const current = sessionStore.getSession();
  if (!current) {
    throw new ApiError('Not signed in', 401);
  }
  const response = await plainClient.post<AuthResponse>('/auth/refresh', {
    refreshToken: current.refreshToken,
  });
  return sessionStore.fromAuthResponse(response.data);
}

async function authorizationToken(): Promise<string | null> {
  const session = sessionStore.getSession();
  if (!session) {
    return null;
  }
  if (session.expiresAt - Date.now() > REFRESH_MARGIN_MS) {
    return session.accessToken;
  }
  try {
    return (await refreshSession()).accessToken;
  } catch (error) {
    if (isCredentialRejection(error)) {
      sessionStore.clear('expired');
      return null;
    }
    // A network blip is not proof the token is dead; let the server be the one to decide.
    return session.accessToken;
  }
}

apiClient.interceptors.request.use(async (config) => {
  const token = await authorizationToken();
  if (token !== null) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const config = axios.isAxiosError(error)
      ? (error.config as RetriableConfig | undefined)
      : undefined;
    const status = axios.isAxiosError(error) ? error.response?.status : undefined;

    if (!config || status !== 401 || config.retriedAfterRefresh || !sessionStore.getSession()) {
      throw ApiError.from(error);
    }

    config.retriedAfterRefresh = true;
    try {
      const session = await refreshSession();
      config.headers.set('Authorization', `Bearer ${session.accessToken}`);
      return await apiClient.request(config);
    } catch (refreshError) {
      if (isCredentialRejection(refreshError)) {
        sessionStore.clear('expired');
      }
      throw ApiError.from(error);
    }
  },
);

plainClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    throw ApiError.from(error);
  },
);

function isCredentialRejection(error: unknown): boolean {
  const status = ApiError.from(error).status;
  return status === 400 || status === 401 || status === 403;
}

/**
 * Thin wrappers so feature code deals in payloads rather than in axios responses. The
 * noContent pair is for the endpoints that answer 204 and have no payload to unwrap.
 */
export const http = {
  get: <T>(url: string, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.get<T>(url, config).then((response) => response.data),

  post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.post<T>(url, body, config).then((response) => response.data),

  put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> =>
    apiClient.put<T>(url, body, config).then((response) => response.data),

  postNoContent: (url: string, body?: unknown, config?: AxiosRequestConfig): Promise<void> =>
    apiClient.post(url, body, config).then(() => undefined),

  putNoContent: (url: string, body?: unknown, config?: AxiosRequestConfig): Promise<void> =>
    apiClient.put(url, body, config).then(() => undefined),

  delete: (url: string, config?: AxiosRequestConfig): Promise<void> =>
    apiClient.delete(url, config).then(() => undefined),
};

/** Test seam: the in-flight refresh is module state that must not leak between cases. */
export function resetRefreshState(): void {
  refreshInFlight = null;
}
