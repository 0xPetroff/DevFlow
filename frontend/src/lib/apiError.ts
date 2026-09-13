import axios from 'axios';

import type { ProblemDetail } from '@/types/api';

/**
 * Every failure the UI sees, whether it came back as an RFC 9457 problem, as an unparseable
 * response, or as a transport error. Status 0 means the request never reached the server.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail | null;
  readonly fieldErrors: Readonly<Record<string, string>>;
  /** The request was given up on rather than refused. Both are status 0; only one is worth repeating. */
  readonly timedOut: boolean;

  constructor(
    message: string,
    status: number,
    problem: ProblemDetail | null = null,
    timedOut = false,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
    this.timedOut = timedOut;
    this.fieldErrors = Object.freeze(collectFieldErrors(problem));
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isValidationError(): boolean {
    return this.status === 400 && Object.keys(this.fieldErrors).length > 0;
  }

  static from(error: unknown): ApiError {
    if (error instanceof ApiError) {
      return error;
    }
    if (axios.isAxiosError(error)) {
      const response = error.response;
      if (!response) {
        const timedOut = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT';
        return new ApiError(
          timedOut
            ? 'The request timed out. Check your connection and try again.'
            : 'Cannot reach the server. Check your connection and try again.',
          0,
          null,
          timedOut,
        );
      }
      const problem = asProblemDetail(response.data);
      return new ApiError(
        problem?.detail ?? problem?.title ?? fallbackMessage(response.status),
        response.status,
        problem,
      );
    }
    return new ApiError(error instanceof Error ? error.message : 'Something went wrong', 0);
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/** The message to show a user, for any thrown value at all. */
export function errorMessage(error: unknown): string {
  return ApiError.from(error).message;
}

function asProblemDetail(data: unknown): ProblemDetail | null {
  if (typeof data !== 'object' || data === null) {
    return null;
  }
  const candidate = data as Partial<ProblemDetail>;
  return typeof candidate.status === 'number' && typeof candidate.title === 'string'
    ? (data as ProblemDetail)
    : null;
}

function collectFieldErrors(problem: ProblemDetail | null): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const entry of problem?.errors ?? []) {
    errors[entry.field] ??= entry.message;
  }
  return errors;
}

function fallbackMessage(status: number): string {
  if (status === 401) return 'Your session has expired. Sign in again.';
  if (status === 403) return 'You do not have permission to do that.';
  if (status === 404) return 'That resource no longer exists.';
  if (status >= 500) return 'The server ran into a problem. Try again in a moment.';
  return 'The request could not be completed.';
}
