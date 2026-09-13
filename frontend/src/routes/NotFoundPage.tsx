import { Link } from 'react-router';

import { Logo } from '@/components/ui/Logo';

export function NotFoundPage() {
  return (
    <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
      <Logo />
      <p className="text-faint font-mono text-5xl font-semibold">404</p>
      <h1 className="text-ink text-lg font-semibold">There is nothing at this address</h1>
      <Link to="/" className="text-accent text-sm font-medium hover:underline">
        Back to the dashboard
      </Link>
    </div>
  );
}
