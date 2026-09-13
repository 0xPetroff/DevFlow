import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { DEMO_CREDENTIALS } from '@/lib/env';
import { applyApiError } from '@/lib/formErrors';
import { redirectTarget } from '@/lib/redirect';
import { prefetchDashboard } from '@/routes/routeChunks';

const schema = z.object({
  identifier: z.string().trim().min(1, 'Enter your email or username'),
  password: z.string().min(1, 'Enter your password'),
});

type LoginValues = z.infer<typeof schema>;

export function LoginPage() {
  // A local binding so the null check narrows inside the click handler too.
  const demo = DEMO_CREDENTIALS;
  const { login, sessionExpired, acknowledgeExpiry } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState<string | null>(null);
  const [expiredNotice] = useState(sessionExpired);

  useEffect(() => {
    acknowledgeExpiry();
  }, [acknowledgeExpiry]);

  // Signing in always lands on the dashboard, so its chunk is worth fetching while the form is
  // still being filled in rather than after the credentials come back. See prefetchDashboard.
  useEffect(() => {
    prefetchDashboard();
  }, []);

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { identifier: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await login(values);
      await navigate(redirectTarget(location.state), { replace: true });
    } catch (error) {
      setFormError(applyApiError(error, setError, ['identifier', 'password']));
    }
  });

  return (
    <div className="border-line bg-surface shadow-card rounded-lg border p-6">
      <h1 className="text-ink text-lg font-semibold tracking-tight">Sign in</h1>
      <p className="text-muted mt-1 mb-6 text-sm">Welcome back. Pick up where you left off.</p>

      {expiredNotice && (
        <Alert variant="warning" className="mb-4">
          Your session expired. Sign in again to continue.
        </Alert>
      )}

      {formError && (
        <Alert variant="danger" className="mb-4">
          {formError}
        </Alert>
      )}

      {demo && (
        <Alert variant="info" title="Demo instance" className="mb-4">
          <p>
            Sign in as <code className="font-mono">{demo.identifier}</code> with the password{' '}
            <code className="font-mono">{demo.password}</code>. The account administers the
            installation, so every screen is reachable. Data resets when the database is recreated.
          </p>
          <button
            type="button"
            onClick={() => {
              setValue('identifier', demo.identifier);
              setValue('password', demo.password);
            }}
            className="mt-2 font-medium underline underline-offset-2"
          >
            Fill the form
          </button>
        </Alert>
      )}

      <form onSubmit={(event) => void onSubmit(event)} className="flex flex-col gap-4" noValidate>
        <Field label="Email or username" error={errors.identifier?.message}>
          {(control) => (
            <Input
              {...control}
              {...register('identifier')}
              type="text"
              autoComplete="username"
              autoFocus
              placeholder="you@example.com"
            />
          )}
        </Field>

        <Field label="Password" error={errors.password?.message}>
          {(control) => (
            <Input
              {...control}
              {...register('password')}
              type="password"
              autoComplete="current-password"
              placeholder="••••••••••"
            />
          )}
        </Field>

        <Button type="submit" loading={isSubmitting} fullWidth className="mt-2">
          Sign in
        </Button>
      </form>

      <p className="text-muted mt-6 text-center text-sm">
        No account yet?{' '}
        <Link to="/register" className="text-accent font-medium hover:underline">
          Create one
        </Link>
      </p>
    </div>
  );
}
