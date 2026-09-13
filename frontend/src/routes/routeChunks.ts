/**
 * The dynamic imports behind the lazily loaded routes, kept apart from the components that
 * render them so that both this and lazyRoutes.tsx stay fast-refresh friendly: a module that
 * exports components alongside plain functions loses the refresh boundary for the whole file.
 *
 * Sharing one factory per route is what makes prefetching safe. The bundler keys a dynamic
 * import on its specifier and hands back the promise already in flight, so a prefetch followed
 * by the real render resolves the same chunk once rather than fetching it twice.
 */
export const importDashboardPage = () => import('@/routes/DashboardPage');

export const importBoardPage = () => import('@/issues/BoardPage');

/**
 * The dashboard is the index route, so signing in always lands on it, and its charting chunk is
 * by far the largest thing the app fetches. Left to itself the download only starts once the
 * credentials come back, which puts it behind a password hash the demo's shared-CPU instance
 * takes a good half second over, and one spinner covers the sum of the two.
 *
 * Calling this while the login form is on screen moves that fetch alongside the typing and the
 * request instead of after them. It is a hint and nothing waits on it, so a failure here is not
 * an error: the route still renders through the same lazy component, which will surface a real
 * failure in its own right. A visitor who never signs in has spent one cached request.
 */
export function prefetchDashboard(): void {
  void importDashboardPage().catch(() => {
    // Offline, or the chunk moved under a new deployment. The route import decides what that
    // means; a speculative fetch has no business turning it into an unhandled rejection.
  });
}
