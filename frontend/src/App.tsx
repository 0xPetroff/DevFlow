import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { RouterProvider, createBrowserRouter } from 'react-router';

import { AuthProvider } from '@/auth/AuthProvider';
import { createQueryClient } from '@/lib/queryClient';
import { routes } from '@/routes/routes';
import { ThemeProvider } from '@/theme/ThemeProvider';
import { ToastProvider } from '@/toast/ToastProvider';

const router = createBrowserRouter(routes);

export function App() {
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
