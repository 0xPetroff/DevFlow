import type { Role } from '@/types/api';

/** Mirrors the explicit ranks on the backend Role enum. */
const RANKS: Record<Role, number> = {
  ADMIN: 30,
  DEVELOPER: 20,
  VIEWER: 10,
};

export function isAtLeast(role: Role | null | undefined, required: Role): boolean {
  return role !== null && role !== undefined && RANKS[role] >= RANKS[required];
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  DEVELOPER: 'Developer',
  VIEWER: 'Viewer',
};
