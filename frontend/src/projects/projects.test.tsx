import { screen, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';

import {
  archivedProject,
  page,
  problemDetail,
  projectOwnedByAnother,
  testProject,
  viewerMembers,
} from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

/** The sidebar carries a Settings link of its own, so tab queries are scoped to the tab bar. */
function projectTabs(): HTMLElement {
  return screen.getByRole('navigation', { name: 'Project sections' });
}

describe('the projects list', () => {
  it('lists the projects the caller can see', async () => {
    seedSession();
    renderApp('/projects');

    const card = await screen.findByRole('link', { name: /DevFlow/ });
    expect(within(card).getByText('DEVF')).toBeInTheDocument();
    expect(within(card).getByText(/2 members/)).toBeInTheDocument();
  });

  it('marks an archived project rather than hiding it', async () => {
    server.use(
      http.get(`${API}/projects`, () => HttpResponse.json(page([testProject, archivedProject]))),
    );
    seedSession();
    renderApp('/projects');

    const card = await screen.findByRole('link', { name: /Retired service/ });
    expect(within(card).getByText('Archived')).toBeInTheDocument();
  });

  it('says so when nothing matches instead of showing an empty grid', async () => {
    server.use(http.get(`${API}/projects`, () => HttpResponse.json(page([]))));
    seedSession();
    renderApp('/projects');

    expect(await screen.findByText('No projects here')).toBeInTheDocument();
  });

  it('creates a project and opens it', async () => {
    seedSession();
    const { user, router } = renderApp('/projects');

    await user.click(await screen.findByRole('button', { name: 'New project' }));

    const dialog = await screen.findByRole('dialog');
    await user.type(within(dialog).getByLabelText('Name'), 'DevFlow');
    await user.type(within(dialog).getByLabelText('Project key'), 'devf');
    await user.click(within(dialog).getByRole('button', { name: 'Create project' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe(`/projects/${testProject.id}`);
    });
  });

  it('reports a rejected key on the field that caused it', async () => {
    server.use(
      http.post(`${API}/projects`, () =>
        HttpResponse.json(
          {
            ...problemDetail(400, 'Validation failed', 'validation-failed'),
            errors: [{ field: 'projectKey', message: 'That key is already taken' }],
          },
          { status: 400 },
        ),
      ),
    );
    seedSession();
    const { user } = renderApp('/projects');

    await user.click(await screen.findByRole('button', { name: 'New project' }));
    const dialog = await screen.findByRole('dialog');
    await user.type(within(dialog).getByLabelText('Name'), 'DevFlow');
    await user.type(within(dialog).getByLabelText('Project key'), 'DEVF');
    await user.click(within(dialog).getByRole('button', { name: 'Create project' }));

    expect(await screen.findByText('That key is already taken')).toBeInTheDocument();
  });
});

describe('the two-tier permission model, as the client sees it', () => {
  it('gives the owner the settings tab and the write affordances', async () => {
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByRole('button', { name: 'New issue' })).toBeInTheDocument();
    expect(within(projectTabs()).getByRole('link', { name: 'Settings' })).toBeInTheDocument();
  });

  /**
   * The signed-in account is a global DEVELOPER but only a VIEWER on this project, which is the
   * case the backend's two tiers exist for: the project role decides on its own.
   */
  it('hides them from a project viewer who is a developer account-wide', async () => {
    server.use(
      http.get(`${API}/projects/:projectId`, () => HttpResponse.json(projectOwnedByAnother)),
      http.get(`${API}/projects/:projectId/members`, () => HttpResponse.json(viewerMembers)),
    );
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByText(/read-only access/)).toBeInTheDocument();
    expect(within(projectTabs()).queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'New issue' })).not.toBeInTheDocument();
  });

  /** Every write path on the API refuses an archived project, so the UI must not offer one. */
  it('withdraws the write affordances once a project is archived', async () => {
    server.use(http.get(`${API}/projects/:projectId`, () => HttpResponse.json(archivedProject)));
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByText(/read-only access/)).toBeInTheDocument();
    expect(screen.getByText('Archived')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'New issue' })).not.toBeInTheDocument();
    // The owner keeps the settings tab, because un-archiving and deleting happen there.
    expect(within(projectTabs()).getByRole('link', { name: 'Settings' })).toBeInTheDocument();
  });

  it('shows the API error rather than an empty shell when the project cannot be opened', async () => {
    server.use(
      http.get(`${API}/projects/:projectId`, () =>
        HttpResponse.json(problemDetail(403, 'You do not have access to this project'), {
          status: 403,
        }),
      ),
    );
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByText('You do not have access to this project')).toBeInTheDocument();
  });
});
