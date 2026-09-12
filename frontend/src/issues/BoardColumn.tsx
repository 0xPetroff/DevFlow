import { useDroppable } from '@dnd-kit/core';
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Plus } from 'lucide-react';

import { cn } from '@/lib/cn';
import { ISSUE_STATUS_LABELS } from '@/lib/labels';
import { IssueCard } from '@/issues/IssueCard';
import type { BoardColumn as BoardColumnData, IssueSummary, IssueStatus } from '@/types/api';

interface BoardColumnProps {
  column: BoardColumnData;
  projectId: string;
  draggable: boolean;
  onAdd?: ((status: IssueStatus) => void) | undefined;
}

export function BoardColumn({ column, projectId, draggable, onAdd }: BoardColumnProps) {
  // The column itself is a drop target as well as its cards, which is what lets a card be
  // dropped into a column that has none to sort against.
  const { setNodeRef, isOver } = useDroppable({ id: column.status });
  const hidden = column.total - column.issues.length;

  return (
    <section className="flex min-w-72 flex-1 flex-col">
      <header className="mb-2 flex items-center justify-between gap-2 px-1">
        <h2 className="text-ink flex items-center gap-2 text-sm font-semibold">
          {ISSUE_STATUS_LABELS[column.status]}
          <span className="text-muted bg-elevated rounded-full px-2 py-0.5 text-xs tabular-nums">
            {column.total}
          </span>
        </h2>
        {onAdd && (
          <button
            type="button"
            aria-label={`Add an issue to ${ISSUE_STATUS_LABELS[column.status]}`}
            onClick={() => {
              onAdd(column.status);
            }}
            className="text-faint hover:bg-elevated hover:text-ink inline-flex size-6 cursor-pointer items-center justify-center rounded"
          >
            <Plus className="size-4" aria-hidden="true" />
          </button>
        )}
      </header>

      <div
        ref={setNodeRef}
        className={cn(
          'flex min-h-32 flex-1 flex-col gap-2 rounded-lg p-2 transition-colors',
          isOver ? 'bg-accent-soft' : 'bg-elevated/50',
        )}
      >
        <SortableContext
          items={column.issues.map((issue) => issue.id)}
          strategy={verticalListSortingStrategy}
        >
          {column.issues.map((issue) => (
            <SortableIssueCard
              key={issue.id}
              issue={issue}
              projectId={projectId}
              draggable={draggable}
            />
          ))}
        </SortableContext>

        {column.issues.length === 0 && (
          <p className="text-faint px-2 py-6 text-center text-xs">Nothing here</p>
        )}

        {hidden > 0 && (
          <p className="text-faint px-2 py-1 text-center text-xs">
            {hidden} more not shown. Use the issues list to see them all.
          </p>
        )}
      </div>
    </section>
  );
}

interface SortableIssueCardProps {
  issue: IssueSummary;
  projectId: string;
  draggable: boolean;
}

function SortableIssueCard({ issue, projectId, draggable }: SortableIssueCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: issue.id,
    disabled: !draggable,
  });

  return (
    <IssueCard
      issue={issue}
      projectId={projectId}
      cardRef={setNodeRef}
      dragging={isDragging}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      // The pointer listeners go on the whole card so a drag can start anywhere on it, and the
      // same listeners go on the grip so the keyboard sensor has a focusable control to act on.
      // The title stays an ordinary link, which would otherwise fight the keyboard sensor for Enter.
      bodyProps={draggable ? listeners : undefined}
      handleProps={draggable ? { ...attributes, ...listeners } : undefined}
    />
  );
}
