import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ExternalLink, Rocket, Search } from 'lucide-react';
import { useState, type ReactNode } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { Pagination } from '@/components/ui/Pagination';
import { Select } from '@/components/ui/Select';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatDateTime, formatRelative } from '@/lib/format';
import {
  DEPLOYMENT_STATUS_LABELS,
  DEPLOYMENT_STATUS_TONES,
  ENVIRONMENT_TYPE_TONES,
  formatDuration,
} from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { useDebounced } from '@/lib/useDebounced';
import { DeploymentFormDialog } from '@/deployments/DeploymentFormDialog';
import { deploymentsApi, type DeploymentListParams } from '@/deployments/deploymentsApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import {
  DEPLOYMENT_STATUSES,
  type DeploymentResponse,
  type DeploymentStatus,
} from '@/types/api';

export function DeploymentsPage() {
  const { project, writer } = useProject();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<DeploymentStatus | ''>('');
  const [environmentId, setEnvironmentId] = useState('');
  const [page, setPage] = useState(0);
  const [deploying, setDeploying] = useState(false);

  const debouncedSearch = useDebounced(search);
  const params: DeploymentListParams = {
    q: debouncedSearch || undefined,
    status: status ? [status] : undefined,
    environmentId: environmentId || undefined,
    page,
    size: 20,
  };

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectDeployments(project.id, params),
    queryFn: () => deploymentsApi.list(project.id, params),
    placeholderData: (previous) => previous,
  });

  const { data: environments = [] } = useQuery({
    queryKey: queryKeys.projectEnvironments(project.id),
    queryFn: () => deploymentsApi.environments(project.id),
  });

  return (
    <>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-ink text-base font-semibold">Deployments</h2>
          <p className="text-muted mt-1 text-sm">
            Every release recorded against this project, whether a pipeline or a person started it.
          </p>
        </div>
        {writer && environments.length > 0 && (
          <Button
            size="sm"
            onClick={() => {
              setDeploying(true);
            }}
          >
            <Rocket className="size-4" aria-hidden="true" />
            Record a deployment
          </Button>
        )}
      </div>

      <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div className="relative xl:col-span-2">
          <Search
            className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            aria-hidden="true"
          />
          <Input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search version, branch or commit"
            aria-label="Search deployments"
            className="pl-9"
          />
        </div>

        <Select
          value={status}
          aria-label="Filter by status"
          onChange={(event) => {
            setStatus(event.target.value as DeploymentStatus | '');
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {DEPLOYMENT_STATUSES.map((value) => (
            <option key={value} value={value}>
              {DEPLOYMENT_STATUS_LABELS[value]}
            </option>
          ))}
        </Select>

        <Select
          value={environmentId}
          aria-label="Filter by environment"
          onChange={(event) => {
            setEnvironmentId(event.target.value);
            setPage(0);
          }}
        >
          <option value="">All environments</option>
          {environments.map((environment) => (
            <option key={environment.id} value={environment.id}>
              {environment.name}
            </option>
          ))}
        </Select>
      </div>

      {error ? (
        <Alert variant="danger" title="Could not load deployments">
          {errorMessage(error)}
        </Alert>
      ) : isPending ? (
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={Rocket}
          title="No deployments yet"
          description={
            environments.length === 0
              ? 'Add an environment first, then a pipeline or a person can record a release against it.'
              : 'Releases recorded against this project will appear here.'
          }
        />
      ) : (
        <div className="flex flex-col gap-4">
          <ul className="flex flex-col gap-3">
            {data.content.map((deployment) => (
              <DeploymentRow key={deployment.id} deployment={deployment} />
            ))}
          </ul>
          <Pagination page={data} onChange={setPage} noun="deployments" />
        </div>
      )}

      {deploying && (
        <DeploymentFormDialog
          environments={environments}
          onClose={() => {
            setDeploying(false);
          }}
        />
      )}
    </>
  );
}

function DeploymentRow({ deployment }: { deployment: DeploymentResponse }) {
  const { project, writer, admin } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const transition = useMutation({
    mutationFn: (next: DeploymentStatus) =>
      deploymentsApi.transition(deployment.id, { status: next }),
    onSuccess: async (updated) => {
      toast.success(
        `${updated.releaseVersion} is now ${DEPLOYMENT_STATUS_LABELS[updated.status].toLowerCase()}`,
      );
      await queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (transitionError) => {
      toast.failure(transitionError, 'Could not change that deployment');
    },
  });

  // The gate a production environment can carry: queueing is open to any pipeline, but only a
  // project administrator may start the run. Everything after that is reported by the pipeline.
  const needsApproval = deployment.environment.requiresApproval;
  const mayStart = deployment.status === 'PENDING' && (needsApproval ? admin : writer);
  const mayFinish = deployment.status === 'RUNNING' && writer;
  const mayCancel = (deployment.status === 'PENDING' || deployment.status === 'RUNNING') && writer;

  return (
    <li className="border-line bg-surface shadow-card rounded-lg border p-4">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-ink text-sm font-semibold">{deployment.releaseVersion}</span>
        <Badge tone={ENVIRONMENT_TYPE_TONES[deployment.environment.type]}>
          {deployment.environment.name}
        </Badge>
        <Badge tone={DEPLOYMENT_STATUS_TONES[deployment.status]}>
          {DEPLOYMENT_STATUS_LABELS[deployment.status]}
        </Badge>
        {needsApproval && deployment.status === 'PENDING' && (
          <Badge tone="warning">Awaiting approval</Badge>
        )}

        <div className="flex-1" />

        {mayStart && (
          <Button
            size="sm"
            loading={transition.isPending}
            onClick={() => {
              transition.mutate('RUNNING');
            }}
          >
            {needsApproval ? 'Approve and start' : 'Start'}
          </Button>
        )}
        {mayFinish && (
          <>
            <Button
              size="sm"
              variant="secondary"
              loading={transition.isPending}
              onClick={() => {
                transition.mutate('SUCCESS');
              }}
            >
              Mark succeeded
            </Button>
            <Button
              size="sm"
              variant="secondary"
              loading={transition.isPending}
              onClick={() => {
                transition.mutate('FAILED');
              }}
            >
              Mark failed
            </Button>
          </>
        )}
        {mayCancel && (
          <Button
            size="sm"
            variant="ghost"
            loading={transition.isPending}
            onClick={() => {
              transition.mutate('CANCELLED');
            }}
          >
            Cancel
          </Button>
        )}
      </div>

      <dl className="text-muted mt-3 grid gap-x-6 gap-y-1 text-xs sm:grid-cols-2 lg:grid-cols-4">
        <Detail label="Branch">
          <span className="font-mono">{deployment.branch}</span>
        </Detail>
        <Detail label="Commit">
          <span className="font-mono">{deployment.commitHash.slice(0, 8)}</span>
        </Detail>
        <Detail label="Queued">{formatDateTime(deployment.queuedAt)}</Detail>
        <Detail label="Duration">{formatDuration(deployment.durationSeconds)}</Detail>
      </dl>

      {deployment.commitMessage && (
        <p className="text-muted mt-2 truncate text-xs italic">{deployment.commitMessage}</p>
      )}

      {deployment.failureReason && (
        <p className="text-danger mt-2 text-xs">{deployment.failureReason}</p>
      )}

      <div className="border-line mt-3 flex flex-wrap items-center gap-3 border-t pt-3">
        {deployment.triggeredBy ? (
          <span className="text-muted flex items-center gap-2 text-xs">
            <Avatar
              name={deployment.triggeredBy.fullName}
              color={deployment.triggeredBy.avatarColor}
              size="sm"
            />
            {deployment.triggeredBy.fullName}
          </span>
        ) : (
          // A pipeline has no user account, so the API names the key that authenticated instead.
          <span className="text-muted text-xs">
            Triggered by {deployment.triggeredByLabel ?? 'an automation'}
          </span>
        )}

        <span className="text-faint text-xs">{formatRelative(deployment.queuedAt)}</span>

        <div className="flex-1" />

        {deployment.pipelineUrl && (
          <a
            href={deployment.pipelineUrl}
            target="_blank"
            rel="noreferrer noopener"
            className="text-accent inline-flex items-center gap-1 text-xs hover:underline"
          >
            Pipeline
            <ExternalLink className="size-3" aria-hidden="true" />
          </a>
        )}
        {deployment.environment.url && (
          <a
            href={deployment.environment.url}
            target="_blank"
            rel="noreferrer noopener"
            className="text-accent inline-flex items-center gap-1 text-xs hover:underline"
          >
            Environment
            <ExternalLink className="size-3" aria-hidden="true" />
          </a>
        )}
      </div>
    </li>
  );
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex gap-1.5">
      <dt className="text-faint">{label}</dt>
      <dd className="text-ink truncate">{children}</dd>
    </div>
  );
}
