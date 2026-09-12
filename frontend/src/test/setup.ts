import '@testing-library/jest-dom/vitest';

import { afterAll, afterEach, beforeAll } from 'vitest';

import { resetRefreshState } from '@/lib/apiClient';
import { sessionStore } from '@/lib/sessionStore';
import { server } from '@/test/server';

// jsdom has no matchMedia, and the theme provider reads it on first render.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string): MediaQueryList =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }) as unknown as MediaQueryList,
});

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  server.resetHandlers();
  sessionStore.clear();
  window.localStorage.clear();
  resetRefreshState();
});

afterAll(() => {
  server.close();
});
