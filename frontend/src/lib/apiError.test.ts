import { AxiosError, AxiosHeaders } from 'axios';
import { describe, expect, it } from 'vitest';

import { ApiError, errorMessage } from '@/lib/apiError';
import type { ProblemDetail } from '@/types/api';

function axiosErrorWith(status: number, data: unknown): AxiosError {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  });
}

describe('ApiError', () => {
  it('takes its message from the problem detail', () => {
    const problem: ProblemDetail = {
      type: 'https://devflow.dev/problems/business-rule',
      title: 'Request cannot be processed',
      status: 422,
      detail: 'A project must be archived before it can be deleted',
      instance: '/api/projects/1',
    };

    const error = ApiError.from(axiosErrorWith(422, problem));

    expect(error.status).toBe(422);
    expect(error.message).toBe('A project must be archived before it can be deleted');
    expect(error.problem).toEqual(problem);
  });

  it('collects validation failures by field', () => {
    const error = ApiError.from(
      axiosErrorWith(400, {
        type: 'https://devflow.dev/problems/validation',
        title: 'Validation failed',
        status: 400,
        detail: 'One or more fields are invalid',
        instance: '/api/auth/register',
        errors: [
          { field: 'username', message: 'must be between 3 and 50 characters' },
          { field: 'password', message: 'must contain at least one digit' },
        ],
      }),
    );

    expect(error.isValidationError).toBe(true);
    expect(error.fieldErrors).toEqual({
      username: 'must be between 3 and 50 characters',
      password: 'must contain at least one digit',
    });
  });

  it('falls back to a status-specific message when the body is not a problem detail', () => {
    const error = ApiError.from(axiosErrorWith(500, '<html>Gateway</html>'));

    expect(error.message).toBe('The server ran into a problem. Try again in a moment.');
    expect(error.problem).toBeNull();
  });

  it('reports an unreachable server as status zero', () => {
    const error = ApiError.from(new AxiosError('Network Error', 'ERR_NETWORK'));

    expect(error.isNetworkError).toBe(true);
    expect(error.status).toBe(0);
  });

  it('separates a timeout from a refused connection', () => {
    const refused = ApiError.from(new AxiosError('Network Error', 'ERR_NETWORK'));
    const timedOut = ApiError.from(new AxiosError('timeout exceeded', 'ECONNABORTED'));

    expect(refused.timedOut).toBe(false);
    expect(timedOut.timedOut).toBe(true);
    expect(timedOut.isNetworkError).toBe(true);
    expect(timedOut.message).toBe('The request timed out. Check your connection and try again.');
  });

  it('passes an existing ApiError through unchanged', () => {
    const original = new ApiError('Already wrapped', 403);
    expect(ApiError.from(original)).toBe(original);
  });

  it('gives any thrown value a message', () => {
    expect(errorMessage(new Error('boom'))).toBe('boom');
    expect(errorMessage('a bare string')).toBe('Something went wrong');
  });
});
