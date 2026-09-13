import { screen, within } from '@testing-library/react';
import { HttpResponse, http as mswHttp } from 'msw';
import { describe, expect, it } from 'vitest';

import { deployment, problemDetail, testDashboard } from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

/**
 * Copied verbatim from a real response on a fresh instance. The backend serialises with
 * non_null inclusion, so successRate is absent rather than null when nothing has finished.
 */
const emptyDashboard = {
  projects: { total: 0, active: 0, archived: 0 },
  issues: {
    total: 0,
    byStatus: { TODO: 0, IN_PROGRESS: 0, IN_REVIEW: 0, DONE: 0 },
    assignedToMe: 0,
    overdue: 0,
  },
  deployments: {
    windowDays: 30,
    total: 0,
    byStatus: { PENDING: 0, RUNNING: 0, SUCCESS: 0, FAILED: 0, CANCELLED: 0 },
    recent: [],
  },
};

describe('DashboardPage', () => {
  it('shows the figures the API returned', async () => {
    seedSession();
    renderApp('/');

    expect(await screen.findByRole('heading', { name: /Welcome back, Ada/ })).toBeInTheDocument();
    expect(await screen.findByText('90%')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('2 active, 1 archived')).toBeInTheDocument();
  });

  it('reports an absent success rate as unknown rather than as a number', async () => {
    seedSession();
    server.use(mswHttp.get(`${API}/dashboard`, () => HttpResponse.json(emptyDashboard)));

    renderApp('/');

    expect(await screen.findByText('--')).toBeInTheDocument();
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  /**
   * The chart itself is hidden from assistive technology, so the numbers behind it are published
   * as a table. That is also the only reading of the chart that survives in jsdom, where nothing
   * has a size and the SVG never lays out.
   */
  it('publishes the charted numbers as a table as well as a chart', async () => {
    seedSession();
    renderApp('/');

    const issues = await screen.findByRole('table', { name: 'Issues by status' });
    expect(within(issues).getByRole('rowheader', { name: 'To do' })).toBeInTheDocument();
    expect(within(issues).getByRole('rowheader', { name: 'In progress' })).toBeInTheDocument();

    const deployments = screen.getByRole('table', { name: 'Deployments by status' });
    expect(within(deployments).getByRole('rowheader', { name: 'Success' })).toBeInTheDocument();
  });

  it('lists the recent deployments the API returned', async () => {
    seedSession();
    server.use(
      mswHttp.get(`${API}/dashboard`, () =>
        HttpResponse.json({
          ...testDashboard,
          deployments: { ...testDashboard.deployments, recent: [deployment()] },
        }),
      ),
    );

    renderApp('/');

    expect(await screen.findByText('1.4.0')).toBeInTheDocument();
    expect(screen.getByText('staging')).toBeInTheDocument();
  });

  it('explains itself when the dashboard cannot be loaded', async () => {
    seedSession();
    server.use(
      mswHttp.get(`${API}/dashboard`, () =>
        HttpResponse.json(problemDetail(500, 'An unexpected error occurred', 'internal-error'), {
          status: 500,
        }),
      ),
    );

    renderApp('/');

    expect(await screen.findByText('Could not load the dashboard')).toBeInTheDocument();
  });
});
