import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RouterProvider, createMemoryRouter } from 'react-router';

import { AuthProvider } from '@/auth/AuthProvider';
import { routes } from '@/routes/routes';
import { ThemeProvider } from '@/theme/ThemeProvider';
import { ToastProvider } from '@/toast/ToastProvider';

/**
 * Mounts the real router and the real providers, so a test exercises guards, layouts and
 * data fetching the way a browser would rather than a hand-assembled subset of them.
 */
export function renderApp(initialEntry = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const router = createMemoryRouter(routes, { initialEntries: [initialEntry] });

  const user = userEvent.setup();
  const result = render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </QueryClientProvider>,
  );

  return { ...result, user, router, queryClient };
}
