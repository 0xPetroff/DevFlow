import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Field, FieldSet } from '@/components/ui/Field';
import { Input, Textarea } from '@/components/ui/Input';
import { LabelChip } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Select } from '@/components/ui/Select';
import { cn } from '@/lib/cn';
import { applyApiError } from '@/lib/formErrors';
import {
  ISSUE_PRIORITY_LABELS,
  ISSUE_STATUS_LABELS,
  ISSUE_TYPE_LABELS,
} from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { issuesApi } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import {
  ISSUE_PRIORITIES,
  ISSUE_STATUSES,
  ISSUE_TYPES,
  type IssueResponse,
  type IssueStatus,
} from '@/types/api';

const schema = z.object({
  title: z.string().trim().min(1, 'Give the issue a title').max(200, 'Title is too long'),
  description: z.string().trim(),
  status: z.enum(ISSUE_STATUSES),
  priority: z.enum(ISSUE_PRIORITIES),
  type: z.enum(ISSUE_TYPES),
  assigneeId: z.string(),
  dueDate: z.string(),
  estimatePoints: z
    .string()
    .refine((value) => value === '' || (Number(value) >= 0 && Number(value) <= 100), {
      message: 'Between 0 and 100',
    }),
});

type Values = z.infer<typeof schema>;

const FIELDS = [
  'title',
  'description',
  'status',
  'priority',
  'type',
  'assigneeId',
  'dueDate',
  'estimatePoints',
] as const;

/**
 * Mounted only while it is open, so the form is constructed with the right defaults instead of
 * being reset into them afterwards. Opening the dialog for a second issue therefore cannot show
 * the first one's values.
 */
interface IssueFormDialogProps {
  onClose: () => void;
  /** Absent for a create; present for an edit of that issue. */
  issue?: IssueResponse | undefined;
  initialStatus?: IssueStatus | undefined;
}

export function IssueFormDialog({ onClose, issue, initialStatus }: IssueFormDialogProps) {
  const { project, members } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [formError, setFormError] = useState<string | null>(null);
  const [labelIds, setLabelIds] = useState<string[]>(
    () => issue?.labels.map((label) => label.id) ?? [],
  );

  const { data: labels = [] } = useQuery({
    queryKey: queryKeys.projectLabels(project.id),
    queryFn: () => issuesApi.labels(project.id),
  });

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: defaults(issue, initialStatus),
  });

  const { mutateAsync } = useMutation({
    mutationFn: (values: Values) => {
      const body = {
        title: values.title,
        description: values.description || null,
        status: values.status,
        priority: values.priority,
        type: values.type,
        assigneeId: values.assigneeId || null,
        dueDate: values.dueDate || null,
        estimatePoints: values.estimatePoints === '' ? null : Number(values.estimatePoints),
        labelIds,
      };
      return issue ? issuesApi.update(issue.id, body) : issuesApi.create(project.id, body);
    },
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
      await queryClient.invalidateQueries({ queryKey: queryKeys.issue(saved.id) });
      toast.success(issue ? `Updated ${saved.key}` : `Created ${saved.key}`);
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
      size="lg"
      title={issue ? `Edit ${issue.key}` : 'New issue'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="issue-form" loading={isSubmitting}>
            {issue ? 'Save changes' : 'Create issue'}
          </Button>
        </>
      }
    >
      <form
        id="issue-form"
        onSubmit={(event) => void onSubmit(event)}
        className="flex flex-col gap-4"
        noValidate
      >
        {formError && <Alert variant="danger">{formError}</Alert>}

        <Field label="Title" error={errors.title?.message}>
          {(control) => <Input {...control} {...register('title')} />}
        </Field>

        <Field label="Description" error={errors.description?.message}>
          {(control) => <Textarea {...control} {...register('description')} rows={5} />}
        </Field>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Status" error={errors.status?.message}>
            {(control) => (
              <Select {...control} {...register('status')}>
                {ISSUE_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {ISSUE_STATUS_LABELS[status]}
                  </option>
                ))}
              </Select>
            )}
          </Field>

          <Field label="Priority" error={errors.priority?.message}>
            {(control) => (
              <Select {...control} {...register('priority')}>
                {ISSUE_PRIORITIES.map((priority) => (
                  <option key={priority} value={priority}>
                    {ISSUE_PRIORITY_LABELS[priority]}
                  </option>
                ))}
              </Select>
            )}
          </Field>

          <Field label="Type" error={errors.type?.message}>
            {(control) => (
              <Select {...control} {...register('type')}>
                {ISSUE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {ISSUE_TYPE_LABELS[type]}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field
            label="Assignee"
            error={errors.assigneeId?.message}
            hint="Project members only."
          >
            {(control) => (
              <Select {...control} {...register('assigneeId')}>
                <option value="">Unassigned</option>
                {members.map((member) => (
                  <option key={member.user.id} value={member.user.id}>
                    {member.user.fullName}
                  </option>
                ))}
              </Select>
            )}
          </Field>

          <Field label="Due date" error={errors.dueDate?.message}>
            {(control) => <Input {...control} {...register('dueDate')} type="date" />}
          </Field>

          <Field label="Estimate" error={errors.estimatePoints?.message} hint="Story points.">
            {(control) => (
              <Input {...control} {...register('estimatePoints')} type="number" min={0} max={100} />
            )}
          </Field>
        </div>

        {labels.length > 0 && (
          <FieldSet legend="Labels">
            <div className="flex flex-wrap gap-2">
              {labels.map((label) => {
                const checked = labelIds.includes(label.id);
                return (
                  <label
                    key={label.id}
                    className={cn(
                      'cursor-pointer rounded-full transition-opacity',
                      !checked && 'opacity-45 hover:opacity-80',
                    )}
                  >
                    <input
                      type="checkbox"
                      className="sr-only"
                      checked={checked}
                      onChange={() => {
                        setLabelIds((current) =>
                          checked
                            ? current.filter((id) => id !== label.id)
                            : [...current, label.id],
                        );
                      }}
                    />
                    <LabelChip name={label.name} color={label.color} />
                  </label>
                );
              })}
            </div>
          </FieldSet>
        )}
      </form>
    </Modal>
  );
}

function defaults(issue: IssueResponse | undefined, initialStatus: IssueStatus | undefined): Values {
  return {
    title: issue?.title ?? '',
    description: issue?.description ?? '',
    status: issue?.status ?? initialStatus ?? 'TODO',
    priority: issue?.priority ?? 'MEDIUM',
    type: issue?.type ?? 'TASK',
    assigneeId: issue?.assignee?.id ?? '',
    dueDate: issue?.dueDate ?? '',
    estimatePoints: issue?.estimatePoints == null ? '' : String(issue.estimatePoints),
  };
}
