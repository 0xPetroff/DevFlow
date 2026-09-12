import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Select } from '@/components/ui/Select';
import { applyApiError } from '@/lib/formErrors';
import { ENVIRONMENT_TYPE_LABELS } from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { deploymentsApi } from '@/deployments/deploymentsApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import { ENVIRONMENT_TYPES, type EnvironmentResponse } from '@/types/api';

const schema = z.object({
  name: z.string().trim().min(1, 'Name the environment').max(60, 'Name is too long'),
  type: z.enum(ENVIRONMENT_TYPES),
  url: z.string().trim().max(500, 'URL is too long'),
  requiresApproval: z.boolean(),
});

type Values = z.infer<typeof schema>;

const FIELDS = ['name', 'type', 'url', 'requiresApproval'] as const;

/** Mounted only while open, so its defaults are the environment being edited. */
interface EnvironmentFormDialogProps {
  onClose: () => void;
  environment?: EnvironmentResponse | undefined;
}

export function EnvironmentFormDialog({ onClose, environment }: EnvironmentFormDialogProps) {
  const { project } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: defaults(environment),
  });

  const { mutateAsync } = useMutation({
    mutationFn: (values: Values) => {
      const body = {
        name: values.name,
        type: values.type,
        url: values.url || null,
        requiresApproval: values.requiresApproval,
      };
      return environment
        ? deploymentsApi.updateEnvironment(project.id, environment.id, body)
        : deploymentsApi.createEnvironment(project.id, body);
    },
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projectEnvironments(project.id) });
      toast.success(environment ? `Updated ${saved.name}` : `Added ${saved.name}`);
      onClose();
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await mutateAsync(values);
    } catch (error) {
      setFormError(applyApiError(error, setError, FIELDS));
    }
  });

  return (
    <Modal
      open
      onClose={onClose}
      title={environment ? `Edit ${environment.name}` : 'New environment'}
      description="Pipelines name the environment when they record a deployment."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="environment-form" loading={isSubmitting}>
            {environment ? 'Save changes' : 'Add environment'}
          </Button>
        </>
      }
    >
      <form
        id="environment-form"
        onSubmit={(event) => void onSubmit(event)}
        className="flex flex-col gap-4"
        noValidate
      >
        {formError && <Alert variant="danger">{formError}</Alert>}

        <Field label="Name" error={errors.name?.message} hint="Unique within the project.">
          {(control) => <Input {...control} {...register('name')} placeholder="staging" />}
        </Field>

        <Field label="Type" error={errors.type?.message}>
          {(control) => (
            <Select {...control} {...register('type')}>
              {ENVIRONMENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {ENVIRONMENT_TYPE_LABELS[type]}
                </option>
              ))}
            </Select>
          )}
        </Field>

        <Field label="URL" error={errors.url?.message}>
          {(control) => (
            <Input {...control} {...register('url')} placeholder="https://staging.example.com" />
          )}
        </Field>

        <label className="flex items-start gap-3">
          <input
            type="checkbox"
            {...register('requiresApproval')}
            className="accent-accent mt-0.5 size-4 cursor-pointer"
          />
          <span>
            <span className="text-ink block text-sm font-medium">Require approval to start</span>
            <span className="text-muted block text-xs">
              A pipeline can queue a release here, but only a project administrator can start it.
            </span>
          </span>
        </label>
      </form>
    </Modal>
  );
}

function defaults(environment: EnvironmentResponse | undefined): Values {
  return {
    name: environment?.name ?? '',
    type: environment?.type ?? 'STAGING',
    url: environment?.url ?? '',
    requiresApproval: environment?.requiresApproval ?? false,
  };
}
