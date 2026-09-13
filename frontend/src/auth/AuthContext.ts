import { createContext } from 'react';

import type { LoginRequest, RegisterRequest, Role, UserResponse } from '@/types/api';

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

export interface AuthContextValue {
  user: UserResponse | null;
  status: AuthStatus;
  /** True when the last sign-out was an expired or revoked session rather than a deliberate one. */
  sessionExpired: boolean;
  hasRole: (role: Role) => boolean;
  login: (body: LoginRequest) => Promise<void>;
  register: (body: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  logoutEverywhere: () => Promise<void>;
  acknowledgeExpiry: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
