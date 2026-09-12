import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Copy, KeyRound, Plus } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatDateTime, formatRelative } from '@/lib/format';
import { applyApiError } from '@/lib/formErrors';
import { queryKeys } from '@/lib/queryKeys';
import { deploymentsApi } from '@/deployments/deploymentsApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { ApiKeyResponse } from '@/types/api';

const schema = z.object({
  name: z.string().trim().min(1, 'Name the key').max(120, 'Name is too long'),
  expiresInDays: z
    .string()
    .refine((value) => value === '' || (Number(value) >= 1 && Number(value) <= 3650), {
      message: 'Between 1 and 3650 days',
    }),
});

type Values = z.infer<typeof schema>;

export function ApiKeysCard() {
  const { project } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [creating, setCreating] = useState(false);
  const [issued, setIssued] = useState<string | null>(null);
  const [revoking, setRevoking] = useState<ApiKeyResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const { data: keys, isPending, error } = useQuery({
    queryKey: queryKeys.projectApiKeys(project.id),
    queryFn: () => deploymentsApi.apiKeys(project.id),
  });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', expiresInDays: '' },
  });

  const create = useMutation({
    mutationFn: (values: Values) =>
      deploymentsApi.createApiKey(project.id, {
        name: values.name,
        expiresInDays: values.expiresInDays === '' ? null : Number(values.expiresInDays),
      }),
    onSuccess: async (response) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projectApiKeys(project.id) });
      setCreating(false);
      reset();
      setIssued(response.key);
    },
  });

  const revoke = useMutation({
    mutationFn: (key: ApiKeyResponse) => deploymentsApi.revokeApiKey(project.id, key.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projectApiKeys(project.id) });
      toast.success('Key revoked');
      setRevoking(null);
    },
    onError: (revokeError) => {
      toast.failure(revokeError, 'Could not revoke that key');
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await create.mutateAsync(values);
    } catch (submitError) {
      setFormError(applyApiError(submitError, setError, ['name', 'expiresInDays']));
    }
  });

  return (
    <>
      <Card>
        <CardHeader
          title="API keys"
          description="Scoped to deployment:write on this project, so a leaked key cannot read the issue tracker."
          actions={
            <Button
              size="sm"
              onClick={() => {
                setFormError(null);
                setCreating(true);
              }}
            >
              <Plus className="size-4" aria-hidden="true" />
              Issue a key
            </Button>
          }
        />
        <CardBody>
          {error ? (
            <Alert variant="danger">{errorMessage(error)}</Alert>
          ) : isPending ? (
            <div className="text-muted flex justify-center py-6">
              <Spinner />
            </div>
          ) : keys.length === 0 ? (
            <p className="text-muted text-sm">
              No keys yet. A build agent needs one to record deployments against this project.
            </p>
          ) : (
            <ul className="divide-line divide-y">
              {keys.map((key) => (
                <li key={key.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0">
                  <KeyRound className="text-faint size-4 shrink-0" aria-hidden="true" />
                  <div className="min-w-40 flex-1">
                    <p className="text-ink text-sm font-medium">{key.name}</p>
                    <p className="text-faint font-mono text-xs">{key.keyPrefix}...</p>
                  </div>

                  <Badge tone={key.active ? 'success' : 'neutral'}>
                    {key.active ? 'Active' : 'Revoked'}
                  </Badge>

                  <div className="text-muted min-w-40 text-xs">
                    <p>Last used {formatRelative(key.lastUsedAt)}</p>
                    <p className="text-faint">
                      {key.expiresAt ? `Expires ${formatDateTime(key.expiresAt)}` : 'No expiry'}
                    </p>
                  </div>

                  {key.active && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setRevoking(key);
                      }}
                    >
                      Revoke
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>

      <Modal
        open={creating}
        onClose={() => {
          setCreating(false);
        }}
        title="Issue an API key"
        description="The key is shown once and stored only as a hash."
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                setCreating(false);
              }}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" form="new-api-key" loading={isSubmitting}>
              Issue key
            </Button>
          </>
        }
      >
        <form
          id="new-api-key"
          onSubmit={(event) => void onSubmit(event)}
          className="flex flex-col gap-4"
          noValidate
        >
          {formError && <Alert variant="danger">{formError}</Alert>}

          <Field
            label="Name"
            error={errors.name?.message}
            hint="Whatever will use it, such as GitHub Actions."
          >
            {(control) => <Input {...control} {...register('name')} placeholder="github-actions" />}
          </Field>

          <Field
            label="Expires in days"
            error={errors.expiresInDays?.message}
            hint="Leave empty for a key that never expires."
          >
            {(control) => (
              <Input {...control} {...register('expiresInDays')} type="number" min={1} max={3650} />
            )}
          </Field>
        </form>
      </Modal>

      <IssuedKeyDialog
        issued={issued}
        onClose={() => {
          setIssued(null);
        }}
      />

      <ConfirmDialog
        open={revoking !== null}
        title="Revoke this key"
        confirmLabel="Revoke"
        loading={revoke.isPending}
        onCancel={() => {
          setRevoking(null);
        }}
        onConfirm={() => {
          if (revoking) {
            revoke.mutate(revoking);
          }
        }}
      >
        Anything using {revoking?.name} stops being able to record deployments immediately. The
        record is kept so the audit trail can still name it.
      </ConfirmDialog>
    </>
  );
}

function IssuedKeyDialog({ issued, onClose }: { issued: string | null; onClose: () => void }) {
  const toast = useToast();

  return (
    <Modal
      open={issued !== null}
      onClose={onClose}
      title="Your new API key"
      footer={<Button onClick={onClose}>Done</Button>}
    >
      <div className="flex flex-col gap-3">
        <Alert variant="warning" title="Copy it now">
          Only a hash of this key is stored, so it cannot be shown again. Issue a new one if it is
          lost.
        </Alert>

        <div className="border-line bg-elevated flex items-center gap-2 rounded-md border p-3">
          <code className="text-ink min-w-0 flex-1 font-mono text-xs break-all">{issued}</code>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => {
              void navigator.clipboard.writeText(issued ?? '').then(
                () => {
                  toast.success('Key copied');
                },
                () => {
                  toast.failure(null, 'Could not copy. Select it and copy manually.');
                },
              );
            }}
          >
            <Copy className="size-3.5" aria-hidden="true" />
            Copy
          </Button>
        </div>

        <p className="text-muted text-xs">
          Send it as the <code className="font-mono">X-DevFlow-Api-Key</code> header.
        </p>
      </div>
    </Modal>
  );
}
