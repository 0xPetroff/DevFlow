import { createContext, use } from 'react';

import type { ProjectMemberResponse, ProjectResponse, Role } from '@/types/api';

export interface ProjectContextValue {
  project: ProjectResponse;
  members: ProjectMemberResponse[];
  /** The caller's effective role here: their project role, or ADMIN for an account-wide admin. */
  role: Role;
  /** May change content right now. Every write path on the API refuses an archived project. */
  writer: boolean;
  admin: boolean;
  /**
   * May administer the project record itself. Unlike `admin` this survives archiving, because
   * un-archiving and deleting are exactly the things an administrator does to an archived project.
   */
  projectAdmin: boolean;
}

export const ProjectContext = createContext<ProjectContextValue | null>(null);

export function useProject(): ProjectContextValue {
  const context = use(ProjectContext);
  if (!context) {
    throw new Error('useProject must be used inside a project route');
  }
  return context;
}
