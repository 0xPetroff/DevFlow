import { sessionStore, type Session } from '@/lib/sessionStore';
import { testUser } from '@/test/fixtures';

export function seedSession(overrides: Partial<Session> = {}): void {
  sessionStore.set({
    accessToken: 'access-token-1',
    refreshToken: 'refresh-token-1',
    expiresAt: Date.now() + 600_000,
    user: testUser,
    ...overrides,
  });
}
