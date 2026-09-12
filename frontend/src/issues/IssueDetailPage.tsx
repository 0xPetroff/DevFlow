import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Badge, LabelChip } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card, CardBody } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatDate, formatDateTime } from '@/lib/format';
import {
  ISSUE_PRIORITY_LABELS,
  ISSUE_PRIORITY_TONES,
  ISSUE_STATUS_LABELS,
  ISSUE_STATUS_TONES,
  ISSUE_TYPE_LABELS,
} from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { CommentSection } from '@/issues/CommentSection';
import { IssueFormDialog } from '@/issues/IssueFormDialog';
import { issuesApi } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { UserSummary } from '@/types/api';

export function IssueDetailPage() {
  const { issueId = '' } = useParams();
  const { project, writer, admin } = useProject();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const { data: issue, isPending, error } = useQuery({
    queryKey: queryKeys.issue(issueId),
    queryFn: () => issuesApi.get(issueId),
  });

  const remove = useMutation({
    mutationFn: () => issuesApi.remove(issueId),
    onSuccess: async () => {
      toast.success(`Deleted ${issue?.key ?? 'the issue'}`);
      await queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
      await navigate(`/projects/${project.id}/board`);
    },
    onError: (deleteError) => {
      toast.failure(deleteError, 'Could not delete that issue');
      setDeleting(false);
    },
  });

  if (error) {
    return (
      <Alert variant="danger" title="Could not load this issue">
        {errorMessage(error)}
      </Alert>
    );
  }

  if (isPending) {
    return (
      <div className="text-muted flex justify-center py-16">
        <Spinner size="lg" />
      </div>
    );
  }

  // Deletion is the reporter's or an administrator's; the API checks it again either way.
  const mayDelete = admin || (writer && issue.creator?.id === user?.id);

  return (
    <>
      <Link
        to={`/projects/${project.id}/board`}
        className="text-muted hover:text-ink mb-4 inline-flex items-center gap-1.5 text-sm"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Back to the board
      </Link>

      <div className="grid gap-6 lg:grid-cols-[1fr_18rem]">
        <div className="flex flex-col gap-6">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-faint font-mono text-sm">{issue.key}</span>
              <Badge tone={ISSUE_STATUS_TONES[issue.status]}>
                {ISSUE_STATUS_LABELS[issue.status]}
              </Badge>
              <Badge tone={ISSUE_PRIORITY_TONES[issue.priority]}>
                {ISSUE_PRIORITY_LABELS[issue.priority]}
              </Badge>

              <div className="flex-1" />

              {writer && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    setEditing(true);
                  }}
                >
                  <Pencil className="size-3.5" aria-hidden="true" />
                  Edit
                </Button>
              )}
              {mayDelete && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setDeleting(true);
                  }}
                >
                  <Trash2 className="size-3.5" aria-hidden="true" />
                  Delete
                </Button>
              )}
            </div>

            <h1 className="text-ink mt-2 text-xl font-semibold tracking-tight">{issue.title}</h1>
          </div>

          <Card>
            <CardBody>
              {issue.description ? (
                <p className="text-muted text-sm leading-relaxed whitespace-pre-wrap">
                  {issue.description}
                </p>
              ) : (
                <p className="text-faint text-sm italic">No description.</p>
              )}
            </CardBody>
          </Card>

          <CommentSection issueId={issue.id} />
        </div>

        <aside className="border-line bg-surface h-fit rounded-lg border p-4">
          <dl className="flex flex-col gap-3 text-sm">
            <Detail label="Type">{ISSUE_TYPE_LABELS[issue.type]}</Detail>
            <Detail label="Assignee">
              <Person user={issue.assignee} fallback="Unassigned" />
            </Detail>
            <Detail label="Reporter">
              <Person user={issue.creator} fallback="Unknown" />
            </Detail>
            <Detail label="Due date">{issue.dueDate ? formatDate(issue.dueDate) : '--'}</Detail>
            <Detail label="Estimate">
              {issue.estimatePoints == null ? '--' : `${issue.estimatePoints} points`}
            </Detail>
            <Detail label="Labels">
              {issue.labels.length === 0 ? (
                '--'
              ) : (
                <span className="flex flex-wrap gap-1">
                  {issue.labels.map((label) => (
                    <LabelChip key={label.id} name={label.name} color={label.color} />
                  ))}
                </span>
              )}
            </Detail>
            <Detail label="Created">{formatDateTime(issue.createdAt)}</Detail>
            <Detail label="Updated">{formatDateTime(issue.updatedAt)}</Detail>
          </dl>
        </aside>
      </div>

      {editing && (
        <IssueFormDialog
          issue={issue}
          onClose={() => {
            setEditing(false);
          }}
        />
      )}

      <ConfirmDialog
        open={deleting}
        title={`Delete ${issue.key}`}
        confirmLabel="Delete issue"
        loading={remove.isPending}
        onCancel={() => {
          setDeleting(false);
        }}
        onConfirm={() => {
          remove.mutate();
        }}
      >
        The issue and its comments are removed permanently.
      </ConfirmDialog>
    </>
  );
}

function Detail({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-faint text-xs tracking-wide uppercase">{label}</dt>
      <dd className="text-ink mt-1">{children}</dd>
    </div>
  );
}

function Person({ user, fallback }: { user?: UserSummary | null | undefined; fallback: string }) {
  if (!user) {
    return <span className="text-muted">{fallback}</span>;
  }
  return (
    <span className="flex items-center gap-2">
      <Avatar name={user.fullName} color={user.avatarColor} size="sm" />
      {user.fullName}
    </span>
  );
}
