import { lazy, Suspense } from 'react';

import { Spinner } from '@/components/ui/Spinner';

/**
 * The two heaviest routes, split out of the main bundle: the board carries the drag-and-drop
 * library and the dashboard carries the charting one, and neither belongs in the download that
 * renders a login form.
 */
const DashboardPage = lazy(() =>
  import('@/routes/DashboardPage').then((module) => ({ default: module.DashboardPage })),
);

const BoardPage = lazy(() =>
  import('@/issues/BoardPage').then((module) => ({ default: module.BoardPage })),
);

function RouteFallback() {
  return (
    <div className="text-muted flex justify-center py-16">
      <Spinner size="lg" />
    </div>
  );
}

export function LazyDashboardPage() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <DashboardPage />
    </Suspense>
  );
}

export function LazyBoardPage() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <BoardPage />
    </Suspense>
  );
}
