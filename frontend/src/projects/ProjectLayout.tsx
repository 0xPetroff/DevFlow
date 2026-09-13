import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { NavLink, Outlet, useParams } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { cn } from '@/lib/cn';
import { errorMessage } from '@/lib/apiError';
import { queryKeys } from '@/lib/queryKeys';
import { ProjectContext, type ProjectContextValue } from '@/projects/ProjectContext';
import { projectsApi } from '@/projects/projectsApi';
import type { ProjectMemberResponse, ProjectResponse, Role } from '@/types/api';

const TABS = [
  { to: 'board', label: 'Board' },
  { to: 'issues', label: 'Issues' },
  { to: 'deployments', label: 'Deployments' },
  { to: 'environments', label: 'Environments' },
  { to: 'members', label: 'Members' },
  { to: 'activity', label: 'Activity' },
  { to: 'settings', label: 'Settings', adminOnly: true },
];

export function ProjectLayout() {
  const { projectId = '' } = useParams();
  const { user } = useAuth();

  const projectQuery = useQuery({
    queryKey: queryKeys.project(projectId),
    queryFn: () => projectsApi.get(projectId),
  });

  const membersQuery = useQuery({
    queryKey: queryKeys.projectMembers(projectId),
    queryFn: () => projectsApi.members(projectId),
  });

  const project = projectQuery.data;
  const members = useMemo(() => membersQuery.data ?? [], [membersQuery.data]);

  const value = useMemo<ProjectContextValue | null>(() => {
    if (!project || !user) {
      return null;
    }
    const role = effectiveRole(project, members, user.id, user.role);
    const active = project.status === 'ACTIVE';
    return {
      project,
      members,
      role,
      writer: active && (role === 'ADMIN' || role === 'DEVELOPER'),
      admin: active && role === 'ADMIN',
      projectAdmin: role === 'ADMIN',
    };
  }, [project, members, user]);

  const error = projectQuery.error ?? membersQuery.error;
  if (error) {
    return (
      <Alert variant="danger" title="Could not open this project">
        {errorMessage(error)}
      </Alert>
    );
  }

  if (!value) {
    return (
      <div className="text-muted flex justify-center py-16">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <ProjectContext value={value}>
      <div className="mb-6">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-ink text-xl font-semibold tracking-tight">{value.project.name}</h1>
          <Badge tone="accent">{value.project.projectKey}</Badge>
          {value.project.status === 'ARCHIVED' && <Badge tone="warning">Archived</Badge>}
        </div>
        {value.project.description && (
          <p className="text-muted mt-1 max-w-3xl text-sm">{value.project.description}</p>
        )}
      </div>

      <nav
        aria-label="Project sections"
        className="border-line no-scrollbar mb-6 flex gap-1 overflow-x-auto border-b"
      >
        {TABS.filter((tab) => !tab.adminOnly || value.projectAdmin).map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              cn(
                '-mb-px border-b-2 px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors',
                isActive
                  ? 'border-accent text-ink'
                  : 'text-muted hover:text-ink border-transparent',
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>

      <Outlet />
    </ProjectContext>
  );
}

/**
 * Mirrors the backend's two tiers: an account-wide ADMIN outranks everything, and otherwise the
 * project role decides on its own. A global VIEWER who is a project DEVELOPER writes here.
 *
 * The owner is always kept in the member list as an ADMIN, but ownership is checked too so this
 * never depends on that invariant holding, exactly as ProjectAccessService does server-side.
 */
function effectiveRole(
  project: ProjectResponse,
  members: ProjectMemberResponse[],
  userId: string,
  accountRole: Role,
): Role {
  if (accountRole === 'ADMIN' || project.owner.id === userId) {
    return 'ADMIN';
  }
  return members.find((member) => member.user.id === userId)?.role ?? 'VIEWER';
}
