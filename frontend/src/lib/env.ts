const demoMode = import.meta.env.VITE_DEMO_MODE === 'true';

export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '/api',
  // Set only on the public demo, where the seeded credentials are published anyway. A real
  // installation leaves it unset and never renders them.
  demoMode,
} as const;

// Behind the same compile-time constant rather than exported unconditionally and hidden at
// render time: Vite substitutes a literal for import.meta.env, so an ordinary build folds this
// to null and the password is minified away rather than shipping in every bundle. Hiding the
// panel at render time is not enough, and a build was checked to confirm it.
export const DEMO_CREDENTIALS = demoMode
  ? { identifier: 'demo', password: 'devflow-demo-1' }
  : null;
