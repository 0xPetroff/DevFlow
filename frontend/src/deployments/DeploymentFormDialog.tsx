import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type ChangeEvent } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Input, Textarea } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Select } from '@/components/ui/Select';
import { applyApiError } from '@/lib/formErrors';
import { queryKeys } from '@/lib/queryKeys';
import { deploymentsApi } from '@/deployments/deploymentsApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { EnvironmentResponse } from '@/types/api';

const schema = z.object({
  environmentName: z.string().min(1, 'Choose an environment'),
  releaseVersion: z.string().trim().min(1, 'Name the release').max(60, 'Too long'),
  commitHash: z
    .string()
    .trim()
    .regex(/^[0-9a-fA-F]{7,40}$/, '7 to 40 hexadecimal characters'),
  commitMessage: z.string().trim().max(500, 'Too long'),
  branch: z.string().trim().min(1, 'Name the branch').max(200, 'Too long'),
  pipelineUrl: z.string().trim().max(500, 'Too long'),
});

type Values = z.infer<typeof schema>;

const FIELDS = [
  'environmentName',
  'releaseVersion',
  'commitHash',
  'commitMessage',
  'branch',
  'pipelineUrl',
] as const;

/** Mounted only while open, so the first environment is already loaded when the form is built. */
interface DeploymentFormDialogProps {
  onClose: () => void;
  environments: EnvironmentResponse[];
}

export function DeploymentFormDialog({ onClose, environments }: DeploymentFormDialogProps) {
  const { project } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      environmentName: environments[0]?.name ?? '',
      releaseVersion: '',
      commitHash: '',
      commitMessage: '',
      branch: 'main',
      pipelineUrl: '',
    },
  });

  // Tracked alongside the form rather than through watch(), whose returned function cannot be
  // memoized and so opts the whole component out of the React compiler.
  const [selectedName, setSelectedName] = useState(environments[0]?.name ?? '');
  const selected = environments.find((environment) => environment.name === selectedName);

  const { mutateAsync } = useMutation({
    mutationFn: (values: Values) =>
      deploymentsApi.create({
        projectId: project.id,
        environmentName: values.environmentName,
        releaseVersion: values.releaseVersion,
        commitHash: values.commitHash,
        commitMessage: values.commitMessage || null,
        branch: values.branch,
        pipelineUrl: values.pipelineUrl || null,
      }),
    onSuccess: async (deployment) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
      toast.success(`Queued ${deployment.releaseVersion} for ${deployment.environment.name}`);
      close();
    },
  });

  function close() {
    reset();
    setFormError(null);
    onClose();
  }

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
      onClose={close}
      title="Record a deployment"
      description="The same request a pipeline makes with its API key. It starts out pending."
      footer={
        <>
          <Button variant="secondary" onClick={close} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="new-deployment" loading={isSubmitting}>
            Queue deployment
          </Button>
        </>
      }
    >
      <form
        id="new-deployment"
        onSubmit={(event) => void onSubmit(event)}
        className="flex flex-col gap-4"
        noValidate
      >
        {formError && <Alert variant="danger">{formError}</Alert>}

        <Field label="Environment" error={errors.environmentName?.message}>
          {(control) => (
            <Select
              {...control}
              {...register('environmentName', {
                onChange: (event: ChangeEvent<HTMLSelectElement>) => {
                  setSelectedName(event.target.value);
                },
              })}
            >
              {environments.map((environment) => (
                <option key={environment.id} value={environment.name}>
                  {environment.name}
                </option>
              ))}
            </Select>
          )}
        </Field>

        {selected?.requiresApproval && (
          <Alert variant="info">
            Releases to {selected.name} wait for a project administrator before they start.
          </Alert>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Release version" error={errors.releaseVersion?.message}>
            {(control) => <Input {...control} {...register('releaseVersion')} placeholder="1.4.0" />}
          </Field>

          <Field label="Branch" error={errors.branch?.message}>
            {(control) => <Input {...control} {...register('branch')} className="font-mono" />}
          </Field>
        </div>

        <Field label="Commit hash" error={errors.commitHash?.message}>
          {(control) => (
            <Input {...control} {...register('commitHash')} className="font-mono" placeholder="9f2c1ab" />
          )}
        </Field>

        <Field label="Commit message" error={errors.commitMessage?.message}>
          {(control) => <Textarea {...control} {...register('commitMessage')} rows={2} />}
        </Field>

        <Field label="Pipeline URL" error={errors.pipelineUrl?.message}>
          {(control) => (
            <Input {...control} {...register('pipelineUrl')} placeholder="https://ci.example.com/devflow/actions/runs/42" />
          )}
        </Field>
      </form>
    </Modal>
  );
}
