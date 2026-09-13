import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { authApi } from '@/auth/authApi';
import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { PageHeader } from '@/components/ui/PageHeader';
import { formatDateTime, formatRelative } from '@/lib/format';
import { applyApiError } from '@/lib/formErrors';
import { ROLE_LABELS } from '@/lib/roles';
import { sessionStore } from '@/lib/sessionStore';

const profileSchema = z.object({
  fullName: z.string().trim().min(1, 'Enter your name').max(120, 'Name is too long'),
});

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Enter your current password'),
    newPassword: z
      .string()
      .min(10, 'At least 10 characters')
      .max(100, 'At most 100 characters')
      .regex(/[A-Za-z]/, 'Must contain a letter')
      .regex(/\d/, 'Must contain a digit'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });

type ProfileValues = z.infer<typeof profileSchema>;
type PasswordValues = z.infer<typeof passwordSchema>;

export function SettingsPage() {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <>
      <PageHeader title="Settings" description="Your profile, password and active sessions." />

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader title="Account" />
          <CardBody className="flex flex-wrap items-center gap-5">
            <Avatar name={user.fullName} color={user.avatarColor} size="lg" />
            <dl className="grid flex-1 grid-cols-[auto_1fr] gap-x-6 gap-y-1.5 text-sm">
              <dt className="text-muted">Username</dt>
              <dd className="text-ink font-mono">{user.username}</dd>
              <dt className="text-muted">Email</dt>
              <dd className="text-ink">{user.email}</dd>
              <dt className="text-muted">Role</dt>
              <dd className="text-ink">{ROLE_LABELS[user.role]}</dd>
              <dt className="text-muted">Member since</dt>
              <dd className="text-ink">{formatDateTime(user.createdAt)}</dd>
              <dt className="text-muted">Last sign-in</dt>
              <dd className="text-ink">{formatRelative(user.lastLoginAt)}</dd>
            </dl>
          </CardBody>
        </Card>

        <ProfileCard fullName={user.fullName} />
        <PasswordCard />
        <SessionsCard />
      </div>
    </>
  );
}

function ProfileCard({ fullName }: { fullName: string }) {
  const [status, setStatus] = useState<{ kind: 'saved' | 'error'; message: string } | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName },
  });

  const onSubmit = handleSubmit(async (values) => {
    setStatus(null);
    try {
      const updated = await authApi.updateProfile(values);
      sessionStore.updateUser(updated);
      reset({ fullName: updated.fullName });
      setStatus({ kind: 'saved', message: 'Profile updated.' });
    } catch (error) {
      const message = applyApiError(error, setError, ['fullName']);
      if (message) {
        setStatus({ kind: 'error', message });
      }
    }
  });

  return (
    <Card>
      <CardHeader title="Profile" description="The name shown next to your activity." />
      <CardBody>
        <form
          onSubmit={(event) => void onSubmit(event)}
          className="flex max-w-md flex-col gap-4"
          noValidate
        >
          {status && (
            <Alert variant={status.kind === 'saved' ? 'success' : 'danger'}>{status.message}</Alert>
          )}

          <Field label="Full name" error={errors.fullName?.message}>
            {(control) => <Input {...control} {...register('fullName')} autoComplete="name" />}
          </Field>

          <div>
            <Button type="submit" loading={isSubmitting} disabled={!isDirty}>
              Save changes
            </Button>
          </div>
        </form>
      </CardBody>
    </Card>
  );
}

function PasswordCard() {
  const [status, setStatus] = useState<{ kind: 'saved' | 'error'; message: string } | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setStatus(null);
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      reset();
      setStatus({ kind: 'saved', message: 'Password changed.' });
    } catch (error) {
      const message = applyApiError(error, setError, ['currentPassword', 'newPassword']);
      if (message) {
        setStatus({ kind: 'error', message });
      }
    }
  });

  return (
    <Card>
      <CardHeader title="Password" />
      <CardBody>
        <form
          onSubmit={(event) => void onSubmit(event)}
          className="flex max-w-md flex-col gap-4"
          noValidate
        >
          {status && (
            <Alert variant={status.kind === 'saved' ? 'success' : 'danger'}>{status.message}</Alert>
          )}

          <Field label="Current password" error={errors.currentPassword?.message}>
            {(control) => (
              <Input
                {...control}
                {...register('currentPassword')}
                type="password"
                autoComplete="current-password"
              />
            )}
          </Field>

          <Field
            label="New password"
            error={errors.newPassword?.message}
            hint="At least 10 characters, with a letter and a digit."
          >
            {(control) => (
              <Input
                {...control}
                {...register('newPassword')}
                type="password"
                autoComplete="new-password"
              />
            )}
          </Field>

          <Field label="Confirm new password" error={errors.confirmPassword?.message}>
            {(control) => (
              <Input
                {...control}
                {...register('confirmPassword')}
                type="password"
                autoComplete="new-password"
              />
            )}
          </Field>

          <div>
            <Button type="submit" loading={isSubmitting}>
              Change password
            </Button>
          </div>
        </form>
      </CardBody>
    </Card>
  );
}

function SessionsCard() {
  const { logoutEverywhere } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  return (
    <Card>
      <CardHeader
        title="Active sessions"
        description="Revokes every refresh token, including the one in this browser."
      />
      <CardBody>
        <Button
          variant="danger"
          loading={signingOut}
          onClick={() => {
            setSigningOut(true);
            void logoutEverywhere();
          }}
        >
          Sign out everywhere
        </Button>
      </CardBody>
    </Card>
  );
}
