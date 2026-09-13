import { CalendarClock, GripVertical } from 'lucide-react';
import type { CSSProperties, HTMLAttributes, Ref } from 'react';
import { Link } from 'react-router';

import { Avatar } from '@/components/ui/Avatar';
import { Badge, LabelChip } from '@/components/ui/Badge';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/format';
import { ISSUE_PRIORITY_LABELS, ISSUE_PRIORITY_TONES, ISSUE_TYPE_LABELS } from '@/lib/labels';
import type { IssueSummary } from '@/types/api';

interface IssueCardProps {
  issue: IssueSummary;
  projectId: string;
  dragging?: boolean;
  overlay?: boolean;
  cardRef?: Ref<HTMLElement> | undefined;
  /** Spread onto the grip so a keyboard drag starts from a focusable control. */
  handleProps?: HTMLAttributes<HTMLElement> | undefined;
  /** Spread onto the card body so a pointer drag can start anywhere on it. */
  bodyProps?: HTMLAttributes<HTMLElement> | undefined;
  style?: CSSProperties | undefined;
}

export function IssueCard({
  issue,
  projectId,
  dragging = false,
  overlay = false,
  cardRef,
  handleProps,
  bodyProps,
  style,
}: IssueCardProps) {
  const overdue = issue.dueDate != null && issue.status !== 'DONE' && issue.dueDate < today();

  return (
    <article
      ref={cardRef}
      style={style}
      className={cn(
        'border-line bg-surface shadow-card touch-none rounded-lg border p-3',
        dragging && 'opacity-40',
        overlay && 'shadow-pop border-accent rotate-1 cursor-grabbing',
      )}
      {...bodyProps}
    >
      <div className="flex items-start justify-between gap-2">
        <Link
          to={`/projects/${projectId}/issues/${issue.id}`}
          className="text-ink hover:text-accent min-w-0 text-sm leading-snug font-medium"
        >
          {issue.title}
        </Link>
        {handleProps && (
          <button
            type="button"
            aria-label={`Reorder ${issue.key}`}
            className="text-faint hover:text-ink -mt-1 -mr-1 inline-flex size-6 shrink-0 cursor-grab items-center justify-center rounded"
            {...handleProps}
          >
            <GripVertical className="size-4" aria-hidden="true" />
          </button>
        )}
      </div>

      {issue.labels.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1">
          {issue.labels.map((label) => (
            <LabelChip key={label.id} name={label.name} color={label.color} />
          ))}
        </div>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <span className="text-faint font-mono text-xs">{issue.key}</span>
        <Badge tone={ISSUE_PRIORITY_TONES[issue.priority]}>
          {ISSUE_PRIORITY_LABELS[issue.priority]}
        </Badge>
        <span className="text-muted text-xs">{ISSUE_TYPE_LABELS[issue.type]}</span>

        <div className="flex-1" />

        {issue.estimatePoints != null && (
          <span className="text-muted bg-elevated rounded px-1.5 py-0.5 text-xs tabular-nums">
            {issue.estimatePoints}
          </span>
        )}
        {issue.assignee && (
          <Avatar
            name={issue.assignee.fullName}
            color={issue.assignee.avatarColor}
            size="sm"
            className="size-6 text-[10px]"
          />
        )}
      </div>

      {issue.dueDate && (
        <p
          className={cn(
            'mt-2 flex items-center gap-1.5 text-xs',
            overdue ? 'text-danger' : 'text-faint',
          )}
        >
          <CalendarClock className="size-3.5" aria-hidden="true" />
          {overdue ? 'Overdue ' : 'Due '}
          {formatDate(issue.dueDate)}
        </p>
      )}
    </article>
  );
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}
