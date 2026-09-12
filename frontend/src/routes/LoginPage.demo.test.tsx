import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import type * as EnvModule from '@/lib/env';
import { renderApp } from '@/test/renderApp';

// DEMO_CREDENTIALS is resolved from import.meta.env when the module is first evaluated and is
// null in an ordinary build, which is what lets the bundler drop the password entirely. The
// demo build is simulated by mocking the module. The companion assertion, that the real module
// exports null, lives in auth.test.tsx.
vi.mock('@/lib/env', async (importOriginal) => {
  const actual = await importOriginal<typeof EnvModule>();
  return {
    ...actual,
    env: { ...actual.env, demoMode: true },
    DEMO_CREDENTIALS: { identifier: 'demo', password: 'devflow-demo-1' },
  };
});

describe('login page on a demo instance', () => {
  it('publishes the demo credentials', async () => {
    renderApp('/login');

    expect(await screen.findByText('Demo instance')).toBeInTheDocument();
    expect(screen.getByText('devflow-demo-1')).toBeInTheDocument();
  });

  it('fills the form so a visitor does not have to type', async () => {
    const { user } = renderApp('/login');

    await user.click(await screen.findByRole('button', { name: 'Fill the form' }));

    expect(screen.getByLabelText('Email or username')).toHaveValue('demo');
    expect(screen.getByLabelText('Password')).toHaveValue('devflow-demo-1');
  });
});
