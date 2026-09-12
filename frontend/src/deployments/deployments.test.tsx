import { screen, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';

import {
  deployment,
  page,
  problemDetail,
  developerMembers,
  productionEnvironment,
  projectOwnedByAnother,
  testProject,
} from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

const deploymentsPath = `/projects/${testProject.id}/deployments`;

/** A release queued against an environment that gates its start on a project administrator. */
function pendingProductionRelease() {
  return page([
    deployment({ status: 'PENDING', environment: productionEnvironment, startedAt: null }),
  ]);
}

describe('the deployment list', () => {
  it('names the pipeline that triggered a run when no person did', async () => {
    seedSession();
    renderApp(deploymentsPath);

    const row = (await screen.findByText('1.4.0')).closest('li');
    expect(row).not.toBeNull();
    expect(within(row!).getByText('Triggered by github-actions')).toBeInTheDocument();
    expect(within(row!).getByText('Success')).toBeInTheDocument();
    expect(within(row!).getByText('3m 0s')).toBeInTheDocument();
  });

  it('shortens the commit hash rather than spilling forty characters into the row', async () => {
    seedSession();
    renderApp(deploymentsPath);

    expect(await screen.findByText('9f2c1ab3')).toBeInTheDocument();
  });
});

describe('the approval gate', () => {
  it('offers a project administrator the approval that starts a gated release', async () => {
    server.use(
      http.get(`${API}/projects/:projectId/deployments`, () =>
        HttpResponse.json(pendingProductionRelease()),
      ),
    );
    seedSession();
    renderApp(deploymentsPath);

    expect(await screen.findByText('Awaiting approval')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Approve and start' })).toBeInTheDocument();
  });

  /**
   * The whole point of Environment.requiresApproval: a pipeline or a developer may queue the
   * production release, but only a project administrator may let it run.
   */
  it('withholds it from a developer, who can still cancel', async () => {
    server.use(
      http.get(`${API}/projects/:projectId`, () => HttpResponse.json(projectOwnedByAnother)),
      http.get(`${API}/projects/:projectId/members`, () => HttpResponse.json(developerMembers)),
      http.get(`${API}/projects/:projectId/deployments`, () =>
        HttpResponse.json(pendingProductionRelease()),
      ),
    );
    seedSession();
    renderApp(deploymentsPath);

    expect(await screen.findByText('Awaiting approval')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve and start' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('starts the release and says so', async () => {
    server.use(
      http.get(`${API}/projects/:projectId/deployments`, () =>
        HttpResponse.json(pendingProductionRelease()),
      ),
    );
    seedSession();
    const { user } = renderApp(deploymentsPath);

    await user.click(await screen.findByRole('button', { name: 'Approve and start' }));

    expect(await screen.findByText('1.4.0 is now running')).toBeInTheDocument();
  });

  it('surfaces a refused transition instead of leaving the row looking changed', async () => {
    server.use(
      http.get(`${API}/projects/:projectId/deployments`, () =>
        HttpResponse.json(pendingProductionRelease()),
      ),
      http.put(`${API}/deployments/:deploymentId/status`, () =>
        HttpResponse.json(
          problemDetail(403, 'Starting a release to production is reserved for administrators'),
          { status: 403 },
        ),
      ),
    );
    seedSession();
    const { user } = renderApp(deploymentsPath);

    await user.click(await screen.findByRole('button', { name: 'Approve and start' }));

    expect(
      await screen.findByText('Starting a release to production is reserved for administrators'),
    ).toBeInTheDocument();
  });
});

describe('environments and keys', () => {
  it('marks which environments gate a release behind approval', async () => {
    server.use(
      http.get(`${API}/projects/:projectId/environments`, () =>
        HttpResponse.json([productionEnvironment]),
      ),
    );
    seedSession();
    renderApp(`/projects/${testProject.id}/environments`);

    expect(await screen.findByText('production')).toBeInTheDocument();
    expect(screen.getByText('Approval required')).toBeInTheDocument();
  });

  it('shows an issued key once, with the warning that it cannot be shown again', async () => {
    seedSession();
    const { user } = renderApp(`/projects/${testProject.id}/environments`);

    await user.click(await screen.findByRole('button', { name: 'Issue a key' }));
    const dialog = await screen.findByRole('dialog');
    await user.type(within(dialog).getByLabelText('Name'), 'github-actions');
    await user.click(within(dialog).getByRole('button', { name: 'Issue key' }));

    expect(await screen.findByText('a1b2c3d4e5f6_deadbeef')).toBeInTheDocument();
    expect(screen.getByText('Copy it now')).toBeInTheDocument();
  });

  it('lists an existing key by its prefix only', async () => {
    seedSession();
    renderApp(`/projects/${testProject.id}/environments`);

    expect(await screen.findByText('a1b2c3d4e5f6...')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Active')).toBeInTheDocument();
    });
  });
});
