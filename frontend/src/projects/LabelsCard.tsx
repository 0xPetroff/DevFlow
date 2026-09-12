import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { Input } from '@/components/ui/Input';
import { LabelChip } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { queryKeys } from '@/lib/queryKeys';
import { issuesApi } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { LabelResponse } from '@/types/api';

const PALETTE = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#a855f7', '#64748b'];

export function LabelsCard() {
  const { project, writer, admin } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [name, setName] = useState('');
  const [color, setColor] = useState(PALETTE[0] ?? '#6366f1');
  const [formError, setFormError] = useState<string | null>(null);

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectLabels(project.id),
    queryFn: () => issuesApi.labels(project.id),
  });

  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: queryKeys.projectLabels(project.id) });

  const create = useMutation({
    mutationFn: () => issuesApi.createLabel(project.id, { name: name.trim(), color }),
    onSuccess: async () => {
      setName('');
      setFormError(null);
      await refresh();
    },
    onError: (createError) => {
      setFormError(errorMessage(createError));
    },
  });

  const remove = useMutation({
    mutationFn: (label: LabelResponse) => issuesApi.removeLabel(project.id, label.id),
    onSuccess: async () => {
      toast.success('Label deleted');
      await refresh();
      await queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
    },
    onError: (removeError) => {
      toast.failure(removeError, 'Could not delete that label');
    },
  });

  return (
    <Card>
      <CardHeader
        title="Labels"
        description="Shared across the project's issues. Deleting one removes it from every issue that carries it."
      />
      <CardBody className="flex flex-col gap-4">
        {error ? (
          <Alert variant="danger">{errorMessage(error)}</Alert>
        ) : isPending ? (
          <div className="text-muted flex justify-center py-4">
            <Spinner />
          </div>
        ) : data.length === 0 ? (
          <p className="text-muted text-sm">No labels yet.</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {data.map((label) => (
              <li key={label.id} className="flex items-center gap-1">
                <LabelChip name={label.name} color={label.color} />
                {admin && (
                  <button
                    type="button"
                    aria-label={`Delete ${label.name}`}
                    disabled={remove.isPending}
                    onClick={() => {
                      remove.mutate(label);
                    }}
                    className="text-faint hover:text-danger inline-flex size-5 cursor-pointer items-center justify-center rounded"
                  >
                    <Trash2 className="size-3" aria-hidden="true" />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        {writer && (
          <form
            className="border-line flex flex-wrap items-end gap-3 border-t pt-4"
            onSubmit={(event) => {
              event.preventDefault();
              create.mutate();
            }}
          >
            <Field label="New label" className="min-w-48 flex-1">
              {(control) => (
                <Input
                  {...control}
                  value={name}
                  maxLength={40}
                  placeholder="needs-review"
                  onChange={(event) => {
                    setName(event.target.value);
                  }}
                />
              )}
            </Field>

            <div className="flex items-center gap-1.5 pb-1">
              {PALETTE.map((swatch) => (
                <button
                  key={swatch}
                  type="button"
                  aria-label={`Use ${swatch}`}
                  aria-pressed={color === swatch}
                  onClick={() => {
                    setColor(swatch);
                  }}
                  style={{ backgroundColor: swatch }}
                  className={
                    color === swatch
                      ? 'ring-accent size-6 cursor-pointer rounded-full ring-2 ring-offset-2 ring-offset-[var(--surface)]'
                      : 'size-6 cursor-pointer rounded-full opacity-70 hover:opacity-100'
                  }
                />
              ))}
            </div>

            <Button type="submit" loading={create.isPending} disabled={name.trim() === ''}>
              <Plus className="size-4" aria-hidden="true" />
              Add
            </Button>

            {formError && (
              <Alert variant="danger" className="w-full">
                {formError}
              </Alert>
            )}
          </form>
        )}
      </CardBody>
    </Card>
  );
}
