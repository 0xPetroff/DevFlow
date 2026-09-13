import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { z } from 'zod';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { applyApiError } from '@/lib/formErrors';

/** Mirrors the constraints on RegisterRequest so the server never has to reject a typo. */
const schema = z.object({
  fullName: z.string().trim().min(1, 'Enter your name').max(120, 'Name is too long'),
  email: z.string().trim().min(1, 'Enter your email').max(255).pipe(z.email('Enter a valid email')),
  username: z
    .string()
    .trim()
    .min(3, 'At least 3 characters')
    .max(50, 'At most 50 characters')
    .regex(/^[a-zA-Z0-9._-]+$/, 'Letters, digits, dot, underscore or hyphen only'),
  password: z
    .string()
    .min(10, 'At least 10 characters')
    .max(100, 'At most 100 characters')
    .regex(/[A-Za-z]/, 'Must contain a letter')
    .regex(/\d/, 'Must contain a digit'),
});

type RegisterValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: createAccount } = useAuth();
  const navigate = useNavigate();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', username: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await createAccount(values);
      await navigate('/', { replace: true });
    } catch (error) {
      setFormError(applyApiError(error, setError, ['fullName', 'email', 'username', 'password']));
    }
  });

  return (
    <div className="border-line bg-surface shadow-card rounded-lg border p-6">
      <h1 className="text-ink text-lg font-semibold tracking-tight">Create an account</h1>
      <p className="text-muted mt-1 mb-6 text-sm">
        The first account on a fresh instance becomes the administrator.
      </p>

      {formError && (
        <Alert variant="danger" className="mb-4">
          {formError}
        </Alert>
      )}

      <form onSubmit={(event) => void onSubmit(event)} className="flex flex-col gap-4" noValidate>
        <Field label="Full name" error={errors.fullName?.message}>
          {(control) => (
            <Input {...control} {...register('fullName')} autoComplete="name" autoFocus />
          )}
        </Field>

        <Field label="Email" error={errors.email?.message}>
          {(control) => (
            <Input
              {...control}
              {...register('email')}
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
            />
          )}
        </Field>

        <Field label="Username" error={errors.username?.message}>
          {(control) => <Input {...control} {...register('username')} autoComplete="username" />}
        </Field>

        <Field
          label="Password"
          error={errors.password?.message}
          hint="At least 10 characters, with a letter and a digit."
        >
          {(control) => (
            <Input
              {...control}
              {...register('password')}
              type="password"
              autoComplete="new-password"
            />
          )}
        </Field>

        <Button type="submit" loading={isSubmitting} fullWidth className="mt-2">
          Create account
        </Button>
      </form>

      <p className="text-muted mt-6 text-center text-sm">
        Already have an account?{' '}
        <Link to="/login" className="text-accent font-medium hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
