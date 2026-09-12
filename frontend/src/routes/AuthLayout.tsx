import { Navigate, Outlet, useLocation } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { Logo } from '@/components/ui/Logo';
import { FullPageSpinner } from '@/components/ui/Spinner';
import { ThemeToggle } from '@/components/ui/ThemeToggle';
import { redirectTarget } from '@/lib/redirect';

export function AuthLayout() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <FullPageSpinner />;
  }
  if (status === 'authenticated') {
    return <Navigate to={redirectTarget(location.state)} replace />;
  }

  return (
    <div className="bg-canvas flex min-h-screen flex-col">
      <header className="flex h-14 items-center justify-between px-4 sm:px-6">
        <Logo />
        <ThemeToggle />
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-8">
        <div className="w-full max-w-sm">
          <Outlet />
        </div>
      </main>

      <footer className="text-faint px-4 py-6 text-center text-xs">
        Project management and deployment tracking
      </footer>
    </div>
  );
}
