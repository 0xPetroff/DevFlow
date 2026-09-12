import { QueryClient } from '@tanstack/react-query';

import { ApiError } from '@/lib/apiError';

/** A 4xx is a verdict, not a hiccup; only transport and server faults are worth retrying. */
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (failureCount >= 2) {
    return false;
  }
  const apiError = ApiError.from(error);
  // The timeout is long enough to cover a cold start, so one that expires has already waited
  // out the slowest case. Repeating it only makes the page hang for a multiple of that.
  if (apiError.timedOut) {
    return false;
  }
  return apiError.status === 0 || apiError.status >= 500;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        gcTime: 5 * 60_000,
        retry: shouldRetry,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
