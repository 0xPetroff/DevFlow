import { FolderKanban, History, LayoutDashboard, Settings, Users } from 'lucide-react';
import type { ComponentType } from 'react';

import type { Role } from '@/types/api';

export interface NavItem {
  to: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  /** Match the path exactly rather than as a prefix. */
  end?: boolean;
  requiresRole?: Role;
}

/**
 * Only the account-wide destinations. Boards, issues, deployments and environments are
 * project-scoped on the API and live inside a project rather than in the global sidebar.
 */
export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/projects', label: 'Projects', icon: FolderKanban },
  { to: '/admin/users', label: 'Users', icon: Users, requiresRole: 'ADMIN' },
  { to: '/admin/audit', label: 'Audit log', icon: History, requiresRole: 'ADMIN' },
  { to: '/settings', label: 'Settings', icon: Settings },
];
