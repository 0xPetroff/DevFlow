import { ShieldOff } from 'lucide-react';
import { Link } from 'react-router';

export function ForbiddenPage() {
  return (
    <div className="flex flex-col items-center gap-3 py-20 text-center">
      <ShieldOff className="text-faint size-8" aria-hidden="true" />
      <h1 className="text-ink text-lg font-semibold">Not your permission level</h1>
      <p className="text-muted max-w-sm text-sm">
        This area needs a role your account does not have. Ask an administrator if you think that is
        wrong.
      </p>
      <Link to="/" className="text-accent mt-2 text-sm font-medium hover:underline">
        Back to the dashboard
      </Link>
    </div>
  );
}
