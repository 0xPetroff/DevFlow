import { screen, waitFor } from '@testing-library/react';
import { HttpResponse, http as mswHttp } from 'msw';
import { describe, expect, it } from 'vitest';

import { DEMO_CREDENTIALS } from '@/lib/env';
import { sessionStore } from '@/lib/sessionStore';
import { problemDetail, testAdmin } from '@/test/fixtures';
import { API } from '@/test/handlers';
import { renderApp } from '@/test/renderApp';
import { server } from '@/test/server';
import { seedSession } from '@/test/session';

describe('authentication flow', () => {
  it('sends an anonymous visitor to the login page', async () => {
    renderApp('/');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('never publishes demo credentials on an ordinary build', async () => {
    // The export itself is null, not merely hidden: that is what lets the bundler drop the
    // password rather than ship it in every build with the panel switched off.
    expect(DEMO_CREDENTIALS).toBeNull();

    renderApp('/login');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByText('Demo instance')).not.toBeInTheDocument();
    expect(screen.queryByText('devflow-demo-1')).not.toBeInTheDocument();
  });

  it('signs in and lands on the dashboard', async () => {
    const { user } = renderApp('/login');

    await user.type(await screen.findByLabelText('Email or username'), 'ada');
    await user.type(screen.getByLabelText('Password'), 'correct-horse-1');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: /Welcome back, Ada/ })).toBeInTheDocument();
    expect(sessionStore.getSession()?.user.username).toBe('ada');
  });

  it('remembers where the visitor was headed before the login', async () => {
    const { user } = renderApp('/settings');

    await user.type(await screen.findByLabelText('Email or username'), 'ada');
    await user.type(screen.getByLabelText('Password'), 'correct-horse-1');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Settings' })).toBeInTheDocument();
  });

  it('shows the reason the server gave for rejecting the credentials', async () => {
    server.use(
      mswHttp.post(`${API}/auth/login`, () =>
        HttpResponse.json(problemDetail(401, 'Invalid credentials', 'authentication-failed'), {
          status: 401,
        }),
      ),
    );
    const { user } = renderApp('/login');

    await user.type(await screen.findByLabelText('Email or username'), 'ada');
    await user.type(screen.getByLabelText('Password'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Invalid credentials')).toBeInTheDocument();
    expect(sessionStore.getSession()).toBeNull();
  });

  it('validates the form before it reaches the server', async () => {
    const { user } = renderApp('/login');

    await user.click(await screen.findByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Enter your email or username')).toBeInTheDocument();
  });

  it('puts a validation failure on the field that caused it', async () => {
    server.use(
      mswHttp.post(`${API}/auth/register`, () =>
        HttpResponse.json(
          {
            ...problemDetail(400, 'One or more fields are invalid', 'validation'),
            errors: [{ field: 'username', message: 'is already taken' }],
          },
          { status: 400 },
        ),
      ),
    );
    const { user } = renderApp('/register');

    await user.type(await screen.findByLabelText('Full name'), 'Ada Lovelace');
    await user.type(screen.getByLabelText('Email'), 'ada@example.com');
    await user.type(screen.getByLabelText('Username'), 'ada');
    await user.type(screen.getByLabelText('Password'), 'correct-horse-1');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('is already taken')).toBeInTheDocument();
  });

  it('restores a stored session without asking to sign in again', async () => {
    seedSession();
    renderApp('/');

    expect(await screen.findByRole('heading', { name: /Welcome back, Ada/ })).toBeInTheDocument();
  });

  it('discards a stored session the server no longer honours', async () => {
    seedSession();
    server.use(
      mswHttp.get(`${API}/auth/me`, () =>
        HttpResponse.json(problemDetail(401, 'Invalid credentials', 'authentication-failed'), {
          status: 401,
        }),
      ),
      mswHttp.post(`${API}/auth/refresh`, () =>
        HttpResponse.json(problemDetail(401, 'Invalid credentials', 'authentication-failed'), {
          status: 401,
        }),
      ),
    );

    renderApp('/');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(await screen.findByText(/Your session expired/)).toBeInTheDocument();
  });

  it('signs out through the account menu', async () => {
    seedSession();
    const { user } = renderApp('/');

    await user.click(await screen.findByRole('button', { name: 'Account menu' }));
    await user.click(screen.getByRole('menuitem', { name: 'Sign out' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    await waitFor(() => {
      expect(sessionStore.getSession()).toBeNull();
    });
  });
});

describe('role guards', () => {
  it('keeps a developer out of the admin area', async () => {
    seedSession();
    renderApp('/admin/users');

    expect(
      await screen.findByRole('heading', { name: 'Not your permission level' }),
    ).toBeInTheDocument();
  });

  it('lets an admin in, and shows them the link', async () => {
    seedSession({ user: testAdmin });
    server.use(mswHttp.get(`${API}/auth/me`, () => HttpResponse.json(testAdmin)));

    renderApp('/admin/users');

    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Users' })).toBeInTheDocument();
  });

  it('hides the admin link from a developer', async () => {
    seedSession();
    renderApp('/');

    await screen.findByRole('heading', { name: /Welcome back, Ada/ });
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument();
  });
});
