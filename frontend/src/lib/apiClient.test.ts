import { HttpResponse, delay, http as mswHttp } from 'msw';
import { describe, expect, it } from 'vitest';

import { http } from '@/lib/apiClient';
import { ApiError } from '@/lib/apiError';
import { sessionStore } from '@/lib/sessionStore';
import { authResponse, problemDetail } from '@/test/fixtures';
import { API } from '@/test/handlers';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

const unauthorized = () =>
  HttpResponse.json(problemDetail(401, 'Invalid credentials', 'authentication-failed'), {
    status: 401,
  });

/** Accepts only the token the refresh handler hands out, so a stale token is rejected. */
const acceptsRefreshedTokenOnly = (onCall?: () => void) =>
  mswHttp.get(`${API}/things`, ({ request }) => {
    onCall?.();
    return request.headers.get('Authorization') === 'Bearer access-token-2'
      ? HttpResponse.json({ ok: true })
      : unauthorized();
  });

function countRefreshes(): () => number {
  let count = 0;
  server.use(
    mswHttp.post(`${API}/auth/refresh`, async () => {
      count += 1;
      await delay(10);
      return HttpResponse.json(
        authResponse({ accessToken: 'access-token-2', refreshToken: 'refresh-token-2' }),
      );
    }),
  );
  return () => count;
}

describe('apiClient', () => {
  it('attaches the access token to every request', async () => {
    seedSession();
    server.use(
      mswHttp.get(`${API}/things`, ({ request }) =>
        HttpResponse.json({ auth: request.headers.get('Authorization') }),
      ),
    );

    await expect(http.get('/things')).resolves.toEqual({ auth: 'Bearer access-token-1' });
  });

  it('sends no authorization header when there is no session', async () => {
    server.use(
      mswHttp.get(`${API}/things`, ({ request }) =>
        HttpResponse.json({ auth: request.headers.get('Authorization') }),
      ),
    );

    await expect(http.get('/things')).resolves.toEqual({ auth: null });
  });

  it('refreshes and replays the original request after a 401', async () => {
    seedSession();
    const refreshes = countRefreshes();
    let attempts = 0;
    server.use(
      acceptsRefreshedTokenOnly(() => {
        attempts += 1;
      }),
    );

    await expect(http.get('/things')).resolves.toEqual({ ok: true });
    expect(attempts).toBe(2);
    expect(refreshes()).toBe(1);
    expect(sessionStore.getSession()?.accessToken).toBe('access-token-2');
  });

  it('sends exactly one refresh for requests that fail concurrently', async () => {
    seedSession();
    const refreshes = countRefreshes();
    server.use(acceptsRefreshedTokenOnly());

    const results = await Promise.all([
      http.get('/things'),
      http.get('/things'),
      http.get('/things'),
    ]);

    expect(results).toEqual([{ ok: true }, { ok: true }, { ok: true }]);
    // A rotated refresh token that is replayed revokes the whole family, so this must stay 1.
    expect(refreshes()).toBe(1);
  });

  it('refreshes ahead of a request when the access token is about to expire', async () => {
    seedSession({ expiresAt: Date.now() + 5_000 });
    const refreshes = countRefreshes();
    let attempts = 0;
    server.use(
      acceptsRefreshedTokenOnly(() => {
        attempts += 1;
      }),
    );

    await expect(http.get('/things')).resolves.toEqual({ ok: true });
    expect(refreshes()).toBe(1);
    expect(attempts).toBe(1);
  });

  it('gives up and clears the session when the refresh token is rejected', async () => {
    seedSession();
    server.use(
      mswHttp.post(`${API}/auth/refresh`, () => unauthorized()),
      mswHttp.get(`${API}/things`, () => unauthorized()),
    );

    await expect(http.get('/things')).rejects.toBeInstanceOf(ApiError);
    expect(sessionStore.getSession()).toBeNull();
    expect(sessionStore.getState().expired).toBe(true);
  });

  it('retries a request only once, however often the server answers 401', async () => {
    seedSession();
    const refreshes = countRefreshes();
    let attempts = 0;
    server.use(
      mswHttp.get(`${API}/things`, () => {
        attempts += 1;
        return unauthorized();
      }),
    );

    await expect(http.get('/things')).rejects.toMatchObject({ status: 401 });
    expect(attempts).toBe(2);
    expect(refreshes()).toBe(1);
  });

  it('keeps the session when a refresh fails for transport reasons', async () => {
    seedSession({ expiresAt: Date.now() + 5_000 });
    server.use(
      mswHttp.post(`${API}/auth/refresh`, () => HttpResponse.error()),
      mswHttp.get(`${API}/things`, ({ request }) =>
        HttpResponse.json({ auth: request.headers.get('Authorization') }),
      ),
    );

    // A network blip is not proof the token is dead, so the stale one still gets its chance.
    await expect(http.get('/things')).resolves.toEqual({ auth: 'Bearer access-token-1' });
    expect(sessionStore.getSession()).not.toBeNull();
  });

  it('surfaces a problem detail as an ApiError carrying its status and message', async () => {
    seedSession();
    server.use(
      mswHttp.get(`${API}/things`, () =>
        HttpResponse.json(problemDetail(422, 'A project must be archived before deletion'), {
          status: 422,
        }),
      ),
    );

    await expect(http.get('/things')).rejects.toMatchObject({
      status: 422,
      message: 'A project must be archived before deletion',
    });
  });
});
