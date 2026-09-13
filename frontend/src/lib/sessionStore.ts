import type { AuthResponse, UserResponse } from '@/types/api';

const STORAGE_KEY = 'devflow.session';

export interface Session {
  accessToken: string;
  refreshToken: string;
  /** Epoch milliseconds at which the access token stops being accepted. */
  expiresAt: number;
  user: UserResponse;
}

export interface SessionState {
  session: Session | null;
  /** True when the last sign-out was involuntary, so the login page can explain itself. */
  expired: boolean;
}

type Listener = () => void;

const listeners = new Set<Listener>();
let state: SessionState = { session: readStoredSession(), expired: false };

/**
 * The single source of truth for credentials. It lives outside React because the axios
 * interceptors need it too, and a context would make them depend on render order.
 *
 * Arrow properties rather than methods: these are handed to useSyncExternalStore and to
 * effects as bare references, so none of them may depend on their receiver.
 */
export const sessionStore = {
  getState: (): SessionState => state,

  getSession: (): Session | null => state.session,

  subscribe: (listener: Listener): (() => void) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  set: (session: Session): void => {
    write(session);
    publish({ session, expired: false });
  },

  fromAuthResponse: (response: AuthResponse): Session => {
    const session: Session = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
      user: response.user,
    };
    sessionStore.set(session);
    return session;
  },

  updateUser: (user: UserResponse): void => {
    if (!state.session) {
      return;
    }
    sessionStore.set({ ...state.session, user });
  },

  clear: (reason: 'signed-out' | 'expired' = 'signed-out'): void => {
    write(null);
    publish({ session: null, expired: reason === 'expired' });
  },

  acknowledgeExpiry: (): void => {
    if (state.expired) {
      publish({ session: state.session, expired: false });
    }
  },
};

/**
 * A tab that signs out or in must not leave its siblings holding the opposite belief. The
 * expiry notice stays off here: whatever happened, the person did it themselves next door.
 */
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key !== null && event.key !== STORAGE_KEY) {
      return;
    }
    publish({ session: readStoredSession(), expired: false });
  });
}

function publish(next: SessionState): void {
  state = next;
  for (const listener of listeners) {
    listener();
  }
}

function write(session: Session | null): void {
  try {
    if (session) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Private browsing denies storage. The in-memory state still carries the session for this tab.
  }
}

function readStoredSession(): Session | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw === null ? null : parseSession(raw);
  } catch {
    return null;
  }
}

function parseSession(raw: string): Session | null {
  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== 'object' || parsed === null) {
      return null;
    }
    // Deliberately not typed as Partial<Session>: this came from storage, so nothing is known yet.
    const candidate = parsed as Record<string, unknown>;
    const valid =
      typeof candidate.accessToken === 'string' &&
      typeof candidate.refreshToken === 'string' &&
      typeof candidate.expiresAt === 'number' &&
      typeof candidate.user === 'object' &&
      candidate.user !== null;
    return valid ? (parsed as Session) : null;
  } catch {
    // A payload written by an older build is not worth rescuing.
    return null;
  }
}
