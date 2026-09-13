import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ExternalLink, Pencil, Plus, Server, Trash2 } from 'lucide-react';
import { useState } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { ENVIRONMENT_TYPE_LABELS, ENVIRONMENT_TYPE_TONES } from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { ApiKeysCard } from '@/deployments/ApiKeysCard';
import { EnvironmentFormDialog } from '@/deployments/EnvironmentFormDialog';
import { deploymentsApi } from '@/deployments/deploymentsApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { EnvironmentResponse } from '@/types/api';

export function EnvironmentsPage() {
  const { project, admin, projectAdmin } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [editing, setEditing] = useState<EnvironmentResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<EnvironmentResponse | null>(null);

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectEnvironments(project.id),
    queryFn: () => deploymentsApi.environments(project.id),
  });

  const remove = useMutation({
    mutationFn: (environment: EnvironmentResponse) =>
      deploymentsApi.removeEnvironment(project.id, environment.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.projectEnvironments(project.id) });
      toast.success('Environment deleted');
      setDeleting(null);
    },
    onError: (removeError) => {
      // The API refuses while any deployment still points at it, which is the usual case here.
      toast.failure(removeError, 'Could not delete that environment');
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader
          title="Environments"
          description="The deployment targets a pipeline can name. Administrators own them: they are release configuration."
          actions={
            admin && (
              <Button
                size="sm"
                onClick={() => {
                  setCreating(true);
                }}
              >
                <Plus className="size-4" aria-hidden="true" />
                New environment
              </Button>
            )
          }
        />
        <CardBody>
          {error ? (
            <Alert variant="danger">{errorMessage(error)}</Alert>
          ) : isPending ? (
            <div className="text-muted flex justify-center py-6">
              <Spinner />
            </div>
          ) : data.length === 0 ? (
            <EmptyState
              icon={Server}
              title="No environments yet"
              description="Add staging and production so pipelines have somewhere to deploy to."
            />
          ) : (
            <ul className="divide-line divide-y">
              {data.map((environment) => (
                <li key={environment.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0">
                  <div className="min-w-40 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-ink text-sm font-medium">{environment.name}</span>
                      <Badge tone={ENVIRONMENT_TYPE_TONES[environment.type]}>
                        {ENVIRONMENT_TYPE_LABELS[environment.type]}
                      </Badge>
                      {environment.requiresApproval && (
                        <Badge tone="warning">Approval required</Badge>
                      )}
                    </div>
                    {environment.url && (
                      <a
                        href={environment.url}
                        target="_blank"
                        rel="noreferrer noopener"
                        className="text-accent mt-1 inline-flex items-center gap-1 text-xs hover:underline"
                      >
                        {environment.url}
                        <ExternalLink className="size-3" aria-hidden="true" />
                      </a>
                    )}
                  </div>

                  {admin && (
                    <div className="flex gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={`Edit ${environment.name}`}
                        onClick={() => {
                          setEditing(environment);
                        }}
                      >
                        <Pencil className="size-3.5" aria-hidden="true" />
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={`Delete ${environment.name}`}
                        onClick={() => {
                          setDeleting(environment);
                        }}
                      >
                        <Trash2 className="size-3.5" aria-hidden="true" />
                      </Button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>

      {projectAdmin && <ApiKeysCard />}

      {(creating || editing !== null) && (
        <EnvironmentFormDialog
          environment={editing ?? undefined}
          onClose={() => {
            setCreating(false);
            setEditing(null);
          }}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="Delete this environment"
        confirmLabel="Delete"
        loading={remove.isPending}
        onCancel={() => {
          setDeleting(null);
        }}
        onConfirm={() => {
          if (deleting) {
            remove.mutate(deleting);
          }
        }}
      >
        An environment with deployment history cannot be deleted, so that the record of what was
        released stays intact.
      </ConfirmDialog>
    </div>
  );
}
