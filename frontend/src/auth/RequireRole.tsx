import { Outlet } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { ForbiddenPage } from '@/routes/ForbiddenPage';
import type { Role } from '@/types/api';

/**
 * A client-side guard is a courtesy, not a control: the API enforces the same rule again on
 * every request. This only spares the user a page that would fail.
 */
export function RequireRole({ role }: { role: Role }) {
  const { hasRole } = useAuth();
  return hasRole(role) ? <Outlet /> : <ForbiddenPage />;
}
