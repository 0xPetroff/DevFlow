import { describe, expect, it, vi } from 'vitest';

import { sessionStore } from '@/lib/sessionStore';
import { authResponse, testAdmin } from '@/test/fixtures';
import { seedSession } from '@/test/session';

describe('sessionStore', () => {
  it('derives an absolute expiry from the relative one the API returns', () => {
    const before = Date.now();
    const session = sessionStore.fromAuthResponse(authResponse({ expiresInSeconds: 900 }));

    expect(session.expiresAt).toBeGreaterThanOrEqual(before + 900_000);
    expect(session.expiresAt).toBeLessThan(before + 901_000);
  });

  it('persists the session so a reload stays signed in', () => {
    seedSession();
    expect(window.localStorage.getItem('devflow.session')).toContain('refresh-token-1');

    sessionStore.clear();
    expect(window.localStorage.getItem('devflow.session')).toBeNull();
  });

  it('notifies subscribers on every transition', () => {
    const listener = vi.fn();
    const unsubscribe = sessionStore.subscribe(listener);

    seedSession();
    sessionStore.clear();
    unsubscribe();
    seedSession();

    expect(listener).toHaveBeenCalledTimes(2);
  });

  it('records whether a sign-out was deliberate', () => {
    seedSession();
    sessionStore.clear('expired');
    expect(sessionStore.getState().expired).toBe(true);

    sessionStore.acknowledgeExpiry();
    expect(sessionStore.getState().expired).toBe(false);

    seedSession();
    sessionStore.clear('signed-out');
    expect(sessionStore.getState().expired).toBe(false);
  });

  it('replaces the user without disturbing the tokens', () => {
    seedSession();
    sessionStore.updateUser(testAdmin);

    expect(sessionStore.getSession()?.user).toEqual(testAdmin);
    expect(sessionStore.getSession()?.accessToken).toBe('access-token-1');
  });

  it('ignores a stored payload it cannot trust', () => {
    // A half-written or older-format entry must not be read back as a session.
    writeFromAnotherTab('{"accessToken":"only-this"}');
    expect(sessionStore.getSession()).toBeNull();

    writeFromAnotherTab('not json at all');
    expect(sessionStore.getSession()).toBeNull();
  });

  it('follows a sign-in performed in another tab', () => {
    writeFromAnotherTab(
      JSON.stringify({
        accessToken: 'from-other-tab',
        refreshToken: 'refresh-other',
        expiresAt: Date.now() + 600_000,
        user: testAdmin,
      }),
    );

    expect(sessionStore.getSession()?.accessToken).toBe('from-other-tab');
    expect(sessionStore.getState().expired).toBe(false);
  });

  it('follows a sign-out performed in another tab without calling it an expiry', () => {
    seedSession();
    writeFromAnotherTab(null);

    expect(sessionStore.getSession()).toBeNull();
    expect(sessionStore.getState().expired).toBe(false);
  });
});

/** The storage event only ever fires for changes made by a different tab. */
function writeFromAnotherTab(value: string | null): void {
  if (value === null) {
    window.localStorage.removeItem('devflow.session');
  } else {
    window.localStorage.setItem('devflow.session', value);
  }
  window.dispatchEvent(new StorageEvent('storage', { key: 'devflow.session' }));
}
