import { useQueryClient } from '@tanstack/react-query';
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from 'react';

import { AuthContext, type AuthContextValue, type AuthStatus } from '@/auth/AuthContext';
import { authApi } from '@/auth/authApi';
import { ApiError } from '@/lib/apiError';
import { isAtLeast } from '@/lib/roles';
import { sessionStore } from '@/lib/sessionStore';
import type { LoginRequest, RegisterRequest, Role } from '@/types/api';

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const state = useSyncExternalStore(sessionStore.subscribe, sessionStore.getState);

  // A stored session proves only that this browser once had one; the account may since have been
  // deactivated or its tokens revoked, so it is re-validated before the app renders behind it.
  const [validated, setValidated] = useState(() => sessionStore.getSession() === null);

  useEffect(() => {
    if (validated) {
      return;
    }
    let cancelled = false;
    void authApi
      .me()
      .then((user) => {
        if (!cancelled) {
          sessionStore.updateUser(user);
        }
      })
      .catch((error: unknown) => {
        if (!cancelled && ApiError.from(error).isUnauthorized) {
          sessionStore.clear('expired');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setValidated(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [validated]);

  // Cached data belongs to the identity that fetched it, never to the next one.
  const activeUserId = state.session?.user.id ?? null;
  const previousUserId = useRef(activeUserId);
  useEffect(() => {
    if (previousUserId.current !== activeUserId) {
      previousUserId.current = activeUserId;
      queryClient.clear();
    }
  }, [activeUserId, queryClient]);

  const login = useCallback(async (body: LoginRequest) => {
    sessionStore.fromAuthResponse(await authApi.login(body));
  }, []);

  const register = useCallback(async (body: RegisterRequest) => {
    sessionStore.fromAuthResponse(await authApi.register(body));
  }, []);

  const logout = useCallback(async () => {
    const session = sessionStore.getSession();
    try {
      if (session) {
        await authApi.logout(session.refreshToken);
      }
    } catch {
      // The credentials are being discarded locally whether or not the server acknowledged it.
    } finally {
      sessionStore.clear('signed-out');
    }
  }, []);

  const logoutEverywhere = useCallback(async () => {
    try {
      await authApi.logoutEverywhere();
    } finally {
      sessionStore.clear('signed-out');
    }
  }, []);

  const user = state.session?.user ?? null;
  const status: AuthStatus = !validated ? 'loading' : user ? 'authenticated' : 'anonymous';

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      sessionExpired: state.expired,
      hasRole: (role: Role) => isAtLeast(user?.role, role),
      login,
      register,
      logout,
      logoutEverywhere,
      acknowledgeExpiry: sessionStore.acknowledgeExpiry,
    }),
    [user, status, state.expired, login, register, logout, logoutEverywhere],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}
