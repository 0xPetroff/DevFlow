import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, FolderKanban, Rocket, UserCheck } from 'lucide-react';
import { Link } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { CountBarChart, type CountDatum } from '@/components/charts/CountBarChart';
import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Spinner } from '@/components/ui/Spinner';
import { StatTile } from '@/components/ui/StatTile';
import { http } from '@/lib/apiClient';
import { errorMessage } from '@/lib/apiError';
import { formatRelative } from '@/lib/format';
import {
  DEPLOYMENT_STATUS_LABELS,
  DEPLOYMENT_STATUS_TONES,
  ENVIRONMENT_TYPE_TONES,
  ISSUE_STATUS_LABELS,
} from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import {
  DEPLOYMENT_STATUSES,
  ISSUE_STATUSES,
  type DashboardResponse,
  type DeploymentStatus,
} from '@/types/api';

const WINDOW_DAYS = 30;

/**
 * Issue status is ordinal: the stages run in an order, so the bars take one hue whose lightness
 * steps with that order. Deployment status is not a sequence but a state, so it takes the app's
 * reserved status colours, the same ones the badges use.
 */
const STAGE_COLORS = ['var(--stage-1)', 'var(--stage-2)', 'var(--stage-3)', 'var(--stage-4)'];

const DEPLOYMENT_COLORS: Record<DeploymentStatus, string> = {
  PENDING: 'var(--muted)',
  RUNNING: 'var(--info)',
  SUCCESS: 'var(--success)',
  FAILED: 'var(--danger)',
  CANCELLED: 'var(--warning)',
};

export function DashboardPage() {
  const { user } = useAuth();
  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.dashboard(WINDOW_DAYS),
    queryFn: () =>
      http.get<DashboardResponse>('/dashboard', { params: { windowDays: WINDOW_DAYS } }),
  });

  if (error) {
    return (
      <>
        <DashboardHeader name={user?.fullName} />
        <Alert variant="danger" title="Could not load the dashboard">
          {errorMessage(error)}
        </Alert>
      </>
    );
  }

  if (isPending) {
    return (
      <>
        <DashboardHeader name={user?.fullName} />
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      </>
    );
  }

  const issueData: CountDatum[] = ISSUE_STATUSES.map((status, index) => ({
    label: ISSUE_STATUS_LABELS[status],
    value: data.issues.byStatus[status] ?? 0,
    color: STAGE_COLORS[index] ?? 'var(--stage-4)',
  }));

  const deploymentData: CountDatum[] = DEPLOYMENT_STATUSES.map((status) => ({
    label: DEPLOYMENT_STATUS_LABELS[status],
    value: data.deployments.byStatus[status] ?? 0,
    color: DEPLOYMENT_COLORS[status],
  }));

  return (
    <>
      <DashboardHeader name={user?.fullName} />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatTile
          label="Projects"
          value={data.projects.total}
          hint={`${data.projects.active} active, ${data.projects.archived} archived`}
          icon={FolderKanban}
        />
        <StatTile
          label="Assigned to me"
          value={data.issues.assignedToMe}
          hint={`${data.issues.total} issues in total`}
          icon={UserCheck}
        />
        <StatTile
          label="Overdue"
          value={data.issues.overdue}
          tone={data.issues.overdue > 0 ? 'warning' : 'default'}
          hint="Past their due date and not done"
          icon={AlertTriangle}
        />
        <StatTile
          label="Deployment success"
          value={formatSuccessRate(data.deployments.successRate)}
          tone={successTone(data.deployments.successRate)}
          hint={`${data.deployments.total} runs in the window`}
          icon={Rocket}
        />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="Issues by status" description="Where the work is sitting." />
          <CardBody>
            <CountBarChart data={issueData} caption="Issues by status" />
          </CardBody>
        </Card>

        <Card>
          <CardHeader
            title="Deployments by status"
            description={`Runs recorded in the last ${WINDOW_DAYS} days.`}
          />
          <CardBody>
            <CountBarChart data={deploymentData} caption="Deployments by status" />
          </CardBody>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader title="Recent deployments" description="The latest releases you can see." />
        <CardBody>
          {data.deployments.recent.length === 0 ? (
            <p className="text-muted text-sm">Nothing has been deployed yet.</p>
          ) : (
            <ul className="divide-line divide-y">
              {data.deployments.recent.map((deployment) => (
                <li
                  key={deployment.id}
                  className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 first:pt-0 last:pb-0"
                >
                  <Link
                    to={`/projects/${deployment.project.id}/deployments`}
                    className="text-faint hover:text-accent font-mono text-xs"
                  >
                    {deployment.project.projectKey}
                  </Link>
                  <span className="text-ink min-w-24 flex-1 text-sm font-medium">
                    {deployment.releaseVersion}
                  </span>
                  <Badge tone={ENVIRONMENT_TYPE_TONES[deployment.environment.type]}>
                    {deployment.environment.name}
                  </Badge>
                  <Badge tone={DEPLOYMENT_STATUS_TONES[deployment.status]}>
                    {DEPLOYMENT_STATUS_LABELS[deployment.status]}
                  </Badge>
                  <span className="text-faint text-xs">{formatRelative(deployment.queuedAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </>
  );
}

function DashboardHeader({ name }: { name?: string | undefined }) {
  return (
    <PageHeader
      title={`Welcome back, ${name?.split(' ')[0] ?? 'there'}`}
      description={`Activity across every project you can see, over the last ${WINDOW_DAYS} days.`}
    />
  );
}

/**
 * No rate means nothing finished in the window, which is not the same as a zero percent rate.
 * It arrives as an absent field rather than a null, so this has to be a nullish check.
 */
function formatSuccessRate(rate: number | null | undefined): string {
  return rate == null ? '--' : `${Math.round(rate * 100)}%`;
}

function successTone(rate: number | null | undefined): 'default' | 'success' | 'danger' {
  if (rate == null) return 'default';
  return rate >= 0.9 ? 'success' : rate < 0.6 ? 'danger' : 'default';
}
