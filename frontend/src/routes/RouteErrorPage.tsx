import { useRouteError } from 'react-router';

import { Button } from '@/components/ui/Button';
import { Logo } from '@/components/ui/Logo';
import { errorMessage } from '@/lib/apiError';

export function RouteErrorPage() {
  const error = useRouteError();

  return (
    <div className="bg-canvas flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
      <Logo />
      <h1 className="text-ink text-lg font-semibold">Something went wrong</h1>
      <p className="text-muted max-w-md text-sm">{errorMessage(error)}</p>
      <Button
        variant="secondary"
        onClick={() => {
          window.location.assign('/');
        }}
      >
        Reload DevFlow
      </Button>
    </div>
  );
}
