import { screen, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';

import { auditEntry, page, testAdmin, testUser } from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

describe('the account directory', () => {
  it('is refused to an account that is not an administrator', async () => {
    seedSession();
    renderApp('/admin/users');

    expect(await screen.findByText('Not your permission level')).toBeInTheDocument();
  });

  it('lists accounts for an administrator', async () => {
    server.use(http.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)));
    seedSession({ user: testAdmin });
    renderApp('/admin/users');

    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument();
    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
  });

  it('changes a role through the API', async () => {
    let sent: unknown = null;
    server.use(
      http.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)),
      http.put(`${API}/users/:id`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ ...testUser, role: 'ADMIN' });
      }),
    );
    seedSession({ user: testAdmin });
    const { user } = renderApp('/admin/users');

    await user.selectOptions(await screen.findByLabelText('Role for ada'), 'ADMIN');

    await waitFor(() => {
      expect(sent).toMatchObject({ role: 'ADMIN', active: true });
    });
    expect(await screen.findByText('Updated ada')).toBeInTheDocument();
  });

  /** The API refuses it too; the client simply does not offer the foot-gun. */
  it('does not let an administrator change their own role', async () => {
    server.use(
      http.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)),
      http.get(`${API}/users`, () => HttpResponse.json(page([testAdmin]))),
    );
    seedSession({ user: testAdmin });
    renderApp('/admin/users');

    const accounts = await screen.findByRole('list', { name: 'Accounts' });
    const row = within(accounts).getByRole('listitem');
    expect(within(row).getByText('You')).toBeInTheDocument();
    expect(within(row).queryByLabelText('Role for grace')).not.toBeInTheDocument();
    expect(within(row).queryByRole('button', { name: 'Disable' })).not.toBeInTheDocument();
  });
});

describe('the audit log', () => {
  it('renders each entry with a readable action rather than the enum name', async () => {
    server.use(http.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)));
    seedSession({ user: testAdmin });
    renderApp('/admin/audit');

    const activity = await screen.findByRole('list', { name: 'Activity' });
    expect(within(activity).getByText('Created DEVF-1 Wire the deploy hook')).toBeInTheDocument();
    expect(within(activity).getByText('Issue created')).toBeInTheDocument();
    expect(within(activity).queryByText('ISSUE_CREATED')).not.toBeInTheDocument();
  });

  it('sends the chosen action to the API as a filter', async () => {
    let requested = '';
    server.use(
      http.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)),
      http.get(`${API}/audit-logs`, ({ request }) => {
        requested = new URL(request.url).search;
        return HttpResponse.json(page([auditEntry]));
      }),
    );
    seedSession({ user: testAdmin });
    const { user } = renderApp('/admin/audit');

    await user.selectOptions(await screen.findByLabelText('Filter by action'), 'DEPLOYMENT_FAILED');

    await waitFor(() => {
      expect(requested).toContain('action=DEPLOYMENT_FAILED');
    });
  });
});
