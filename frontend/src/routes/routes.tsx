import { Navigate, type RouteObject } from 'react-router';

import { RequireAuth } from '@/auth/RequireAuth';
import { RequireRole } from '@/auth/RequireRole';
import { AppLayout } from '@/components/layout/AppLayout';
import { AuthLayout } from '@/routes/AuthLayout';
import { LazyBoardPage, LazyDashboardPage } from '@/routes/lazyRoutes';
import { LoginPage } from '@/routes/LoginPage';
import { NotFoundPage } from '@/routes/NotFoundPage';
import { RegisterPage } from '@/routes/RegisterPage';
import { RouteErrorPage } from '@/routes/RouteErrorPage';
import { SettingsPage } from '@/routes/SettingsPage';
import { AuditPage } from '@/admin/AuditPage';
import { UsersPage } from '@/admin/UsersPage';
import { DeploymentsPage } from '@/deployments/DeploymentsPage';
import { EnvironmentsPage } from '@/deployments/EnvironmentsPage';
import { IssueDetailPage } from '@/issues/IssueDetailPage';
import { IssuesPage } from '@/issues/IssuesPage';
import { MembersPage } from '@/projects/MembersPage';
import { ProjectActivityPage } from '@/projects/ProjectActivityPage';
import { ProjectLayout } from '@/projects/ProjectLayout';
import { ProjectSettingsPage } from '@/projects/ProjectSettingsPage';
import { ProjectsPage } from '@/projects/ProjectsPage';

export const routes: RouteObject[] = [
  {
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
        ],
      },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { index: true, element: <LazyDashboardPage /> },
              { path: 'projects', element: <ProjectsPage /> },
              {
                path: 'projects/:projectId',
                element: <ProjectLayout />,
                children: [
                  { index: true, element: <Navigate to="board" replace /> },
                  { path: 'board', element: <LazyBoardPage /> },
                  { path: 'issues', element: <IssuesPage /> },
                  { path: 'issues/:issueId', element: <IssueDetailPage /> },
                  { path: 'deployments', element: <DeploymentsPage /> },
                  { path: 'environments', element: <EnvironmentsPage /> },
                  { path: 'members', element: <MembersPage /> },
                  { path: 'activity', element: <ProjectActivityPage /> },
                  { path: 'settings', element: <ProjectSettingsPage /> },
                ],
              },
              { path: 'settings', element: <SettingsPage /> },
              {
                element: <RequireRole role="ADMIN" />,
                children: [
                  { path: 'admin/users', element: <UsersPage /> },
                  { path: 'admin/audit', element: <AuditPage /> },
                ],
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
