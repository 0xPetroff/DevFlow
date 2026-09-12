import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { Input, Textarea } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { applyApiError } from '@/lib/formErrors';
import { queryKeys } from '@/lib/queryKeys';
import { projectsApi } from '@/projects/projectsApi';
import { useToast } from '@/toast/useToast';

const schema = z.object({
  projectKey: z
    .string()
    .trim()
    .regex(/^[A-Za-z][A-Za-z0-9]{1,9}$/, '2 to 10 letters or digits, starting with a letter'),
  name: z.string().trim().min(1, 'Name the project').max(120, 'Name is too long'),
  description: z.string().trim().max(5000, 'Description is too long'),
  repositoryUrl: z.string().trim().max(500, 'URL is too long'),
});

type Values = z.infer<typeof schema>;

const FIELDS = ['projectKey', 'name', 'description', 'repositoryUrl'] as const;

interface ProjectFormDialogProps {
  open: boolean;
  onClose: () => void;
}

export function ProjectFormDialog({ open, onClose }: ProjectFormDialogProps) {
  const navigate = useNavigate();
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
    defaultValues: { projectKey: '', name: '', description: '', repositoryUrl: '' },
  });

  const { mutateAsync } = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: async (project) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projects });
      toast.success(`Created ${project.projectKey}`);
      close();
      await navigate(`/projects/${project.id}`);
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
      await mutateAsync({
        projectKey: values.projectKey.toUpperCase(),
        name: values.name,
        description: values.description || null,
        repositoryUrl: values.repositoryUrl || null,
      });
    } catch (error) {
      setFormError(applyApiError(error, setError, FIELDS));
    }
  });

  return (
    <Modal
      open={open}
      onClose={close}
      title="New project"
      description="The key prefixes every issue in the project and cannot be changed later."
      footer={
        <>
          <Button variant="secondary" onClick={close} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="new-project" loading={isSubmitting}>
            Create project
          </Button>
        </>
      }
    >
      <form
        id="new-project"
        onSubmit={(event) => void onSubmit(event)}
        className="flex flex-col gap-4"
        noValidate
      >
        {formError && <Alert variant="danger">{formError}</Alert>}

        <Field label="Name" error={errors.name?.message}>
          {(control) => <Input {...control} {...register('name')} placeholder="Payments service" />}
        </Field>

        <Field
          label="Project key"
          error={errors.projectKey?.message}
          hint="Issues will be numbered PAY-1, PAY-2 and so on."
        >
          {(control) => (
            <Input
              {...control}
              {...register('projectKey')}
              placeholder="PAY"
              className="font-mono uppercase"
            />
          )}
        </Field>

        <Field label="Description" error={errors.description?.message}>
          {(control) => <Textarea {...control} {...register('description')} rows={3} />}
        </Field>

        <Field label="Repository URL" error={errors.repositoryUrl?.message}>
          {(control) => (
            <Input
              {...control}
              {...register('repositoryUrl')}
              placeholder="https://github.com/acme/payments"
            />
          )}
        </Field>
      </form>
    </Modal>
  );
}
