import { screen, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';

import {
  page,
  problemDetail,
  projectOwnedByAnother,
  testComment,
  testIssue,
  testProject,
  viewerMembers,
} from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

const issuePath = `/projects/${testProject.id}/issues/${testIssue.id}`;

describe('the board', () => {
  it('renders every column with its own total, not the number of cards it is showing', async () => {
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByText('Wire the deploy hook')).toBeInTheDocument();
    expect(screen.getByText('Add metrics')).toBeInTheDocument();
    expect(screen.getByText('Ship the schema')).toBeInTheDocument();

    const todo = screen.getByRole('heading', { name: /To do/ });
    expect(within(todo).getByText('2')).toBeInTheDocument();
  });

  it('offers a grip to reorder each card, and withholds it from a reader', async () => {
    seedSession();
    renderApp(`/projects/${testProject.id}/board`);

    expect(await screen.findByRole('button', { name: 'Reorder DEVF-1' })).toBeInTheDocument();

    server.use(
      http.get(`${API}/projects/:projectId`, () => HttpResponse.json(projectOwnedByAnother)),
      http.get(`${API}/projects/:projectId/members`, () => HttpResponse.json(viewerMembers)),
    );
    const second = renderApp(`/projects/${testProject.id}/board`);
    await waitFor(() => {
      expect(
        within(second.container).queryByRole('button', { name: 'Reorder DEVF-1' }),
      ).not.toBeInTheDocument();
    });
  });
});

describe('an issue', () => {
  it('shows its detail, its reporter and its comments', async () => {
    seedSession();
    renderApp(issuePath);

    expect(await screen.findByRole('heading', { name: 'Wire the deploy hook' })).toBeInTheDocument();
    expect(screen.getByText('GitHub Actions should post to /api/deployments.')).toBeInTheDocument();
    expect(await screen.findByText('The key needs deployment:write.')).toBeInTheDocument();
    expect(screen.getByText('Reporter')).toBeInTheDocument();
  });

  it('posts a comment and clears the box', async () => {
    seedSession();
    const { user } = renderApp(issuePath);

    const box = await screen.findByLabelText('Add a comment');
    await user.type(box, 'Looks right to me');
    await user.click(screen.getByRole('button', { name: 'Comment' }));

    await waitFor(() => {
      expect(box).toHaveValue('');
    });
  });

  it('offers editing only on your own comment, and deletion to a project administrator', async () => {
    seedSession();
    renderApp(issuePath);

    // The signed-in user wrote this one and owns the project, so both are available.
    expect(await screen.findByRole('button', { name: 'Edit your comment' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Delete comment from Ada Lovelace' }),
    ).toBeInTheDocument();
  });

  it('does not offer to edit somebody else’s comment', async () => {
    server.use(
      http.get(`${API}/issues/:issueId/comments`, () =>
        HttpResponse.json(
          page([
            {
              ...testComment,
              author: {
                id: 'someone-else',
                username: 'grace',
                fullName: 'Grace Hopper',
                avatarColor: '#059669',
              },
            },
          ]),
        ),
      ),
    );
    seedSession();
    renderApp(issuePath);

    expect(await screen.findByText('The key needs deployment:write.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit your comment' })).not.toBeInTheDocument();
    // Clearing up someone else's comment is still an administrator's job.
    expect(
      screen.getByRole('button', { name: 'Delete comment from Grace Hopper' }),
    ).toBeInTheDocument();
  });

  it('reports a refused comment through a toast rather than swallowing it', async () => {
    server.use(
      http.post(`${API}/issues/:issueId/comments`, () =>
        HttpResponse.json(problemDetail(403, 'This project is archived'), { status: 403 }),
      ),
    );
    seedSession();
    const { user } = renderApp(issuePath);

    await user.type(await screen.findByLabelText('Add a comment'), 'Anyone home');
    await user.click(screen.getByRole('button', { name: 'Comment' }));

    expect(await screen.findByText('This project is archived')).toBeInTheDocument();
  });
});

describe('the issue list', () => {
  it('lists issues with their status and priority', async () => {
    seedSession();
    renderApp(`/projects/${testProject.id}/issues`);

    const row = await screen.findByRole('link', { name: /Wire the deploy hook/ });
    expect(within(row).getByText('DEVF-1')).toBeInTheDocument();
    expect(within(row).getByText('To do')).toBeInTheDocument();
    expect(within(row).getByText('High')).toBeInTheDocument();
  });

  it('asks the API for one status at a time as a repeated parameter, not an indexed one', async () => {
    let requested = '';
    server.use(
      http.get(`${API}/projects/:projectId/issues`, ({ request }) => {
        requested = new URL(request.url).search;
        return HttpResponse.json(page([testIssue]));
      }),
    );
    seedSession();
    const { user } = renderApp(`/projects/${testProject.id}/issues`);

    await user.selectOptions(await screen.findByLabelText('Filter by status'), 'DONE');

    // Spring binds status=DONE to a List; axios's default status[]=DONE would be dropped.
    await waitFor(() => {
      expect(requested).toContain('status=DONE');
    });
    expect(requested).not.toContain('status%5B%5D');
  });
});
