import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Field } from '@/components/ui/Field';
import { Input, Textarea } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { applyApiError } from '@/lib/formErrors';
import { PROJECT_STATUS_LABELS } from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { LabelsCard } from '@/projects/LabelsCard';
import { useProject } from '@/projects/ProjectContext';
import { projectsApi } from '@/projects/projectsApi';
import { useToast } from '@/toast/useToast';
import { PROJECT_STATUSES } from '@/types/api';

const schema = z.object({
  name: z.string().trim().min(1, 'Name the project').max(120, 'Name is too long'),
  description: z.string().trim().max(5000, 'Description is too long'),
  repositoryUrl: z.string().trim().max(500, 'URL is too long'),
  status: z.enum(PROJECT_STATUSES),
  ownerId: z.string(),
});

type Values = z.infer<typeof schema>;

const FIELDS = ['name', 'description', 'repositoryUrl', 'status', 'ownerId'] as const;

export function ProjectSettingsPage() {
  const { project, members } = useProject();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const toast = useToast();

  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: project.name,
      description: project.description ?? '',
      repositoryUrl: project.repositoryUrl ?? '',
      status: project.status,
      ownerId: project.owner.id,
    },
  });

  const { mutateAsync } = useMutation({
    mutationFn: (values: Values) =>
      projectsApi.update(project.id, {
        name: values.name,
        description: values.description || null,
        repositoryUrl: values.repositoryUrl || null,
        status: values.status,
        ownerId: values.ownerId === project.owner.id ? null : values.ownerId,
      }),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projects });
      reset({
        name: updated.name,
        description: updated.description ?? '',
        repositoryUrl: updated.repositoryUrl ?? '',
        status: updated.status,
        ownerId: updated.owner.id,
      });
      toast.success('Project updated');
    },
  });

  const remove = useMutation({
    mutationFn: () => projectsApi.remove(project.id),
    onSuccess: async () => {
      toast.success(`Deleted ${project.projectKey}`);
      await queryClient.invalidateQueries({ queryKey: queryKeys.projects });
      await navigate('/projects');
    },
    onError: (deleteError) => {
      toast.failure(deleteError, 'Could not delete the project');
      setDeleting(false);
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
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader
          title="Project details"
          description="The project key is fixed: every issue key derives from it."
        />
        <CardBody>
          <form
            onSubmit={(event) => void onSubmit(event)}
            className="flex max-w-xl flex-col gap-4"
            noValidate
          >
            {formError && <Alert variant="danger">{formError}</Alert>}

            <Field label="Name" error={errors.name?.message}>
              {(control) => <Input {...control} {...register('name')} />}
            </Field>

            <Field label="Description" error={errors.description?.message}>
              {(control) => <Textarea {...control} {...register('description')} rows={3} />}
            </Field>

            <Field label="Repository URL" error={errors.repositoryUrl?.message}>
              {(control) => <Input {...control} {...register('repositoryUrl')} />}
            </Field>

            <Field
              label="Status"
              error={errors.status?.message}
              hint="An archived project is read-only until it is made active again."
            >
              {(control) => (
                <Select {...control} {...register('status')}>
                  {PROJECT_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {PROJECT_STATUS_LABELS[status]}
                    </option>
                  ))}
                </Select>
              )}
            </Field>

            <Field
              label="Owner"
              error={errors.ownerId?.message}
              hint="Transferring ownership makes the new owner a project administrator."
            >
              {(control) => (
                <Select {...control} {...register('ownerId')}>
                  {members.map((member) => (
                    <option key={member.user.id} value={member.user.id}>
                      {member.user.fullName}
                    </option>
                  ))}
                </Select>
              )}
            </Field>

            <div>
              <Button type="submit" loading={isSubmitting} disabled={!isDirty}>
                Save changes
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>

      <LabelsCard />

      <Card className="border-danger/40">
        <CardHeader
          title="Delete this project"
          description="Everything inside it goes: issues, comments, environments and the deployment history."
        />
        <CardBody>
          {project.status === 'ACTIVE' ? (
            <Alert variant="info">
              Archive the project first. Deleting is deliberately two steps, because the cascade
              cannot be undone.
            </Alert>
          ) : (
            <Button
              variant="danger"
              onClick={() => {
                setDeleting(true);
              }}
            >
              Delete project
            </Button>
          )}
        </CardBody>
      </Card>

      <ConfirmDialog
        open={deleting}
        title={`Delete ${project.projectKey}`}
        confirmLabel="Delete permanently"
        loading={remove.isPending}
        onCancel={() => {
          setDeleting(false);
        }}
        onConfirm={() => {
          remove.mutate();
        }}
      >
        {project.name} and everything in it will be removed. This cannot be undone.
      </ConfirmDialog>
    </div>
  );
}
