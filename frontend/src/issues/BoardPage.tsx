import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { arrayMove, sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { queryKeys } from '@/lib/queryKeys';
import { BoardColumn } from '@/issues/BoardColumn';
import {
  columnOf,
  findIssue,
  issuesIn,
  moveAcross,
  neighbours,
  samePosition,
  withIssues,
  type Neighbours,
} from '@/issues/boardOrdering';
import { IssueCard } from '@/issues/IssueCard';
import { IssueFormDialog } from '@/issues/IssueFormDialog';
import { issuesApi } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type {
  BoardColumn as BoardColumnData,
  BoardResponse,
  IssueStatus,
  IssueSummary,
  MoveIssueRequest,
} from '@/types/api';

interface DragOrigin extends Neighbours {
  status: IssueStatus;
}

export function BoardPage() {
  const { project, writer } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const boardKey = queryKeys.projectBoard(project.id);
  const { data, isPending, error } = useQuery({
    queryKey: boardKey,
    queryFn: () => issuesApi.board(project.id),
  });

  const [activeIssue, setActiveIssue] = useState<IssueSummary | null>(null);
  const [origin, setOrigin] = useState<DragOrigin | null>(null);
  const [creatingIn, setCreatingIn] = useState<IssueStatus | null>(null);

  const columns = data?.columns ?? [];

  /**
   * A drag reorders the cached board rather than a copy of it held in component state. One source
   * of truth means the optimistic order cannot be momentarily overwritten by a re-render, and the
   * server's answer simply replaces it when the refetch lands.
   */
  function writeColumns(next: BoardColumnData[]) {
    queryClient.setQueryData<BoardResponse>(boardKey, (board) =>
      board ? { ...board, columns: next } : board,
    );
  }

  const sensors = useSensors(
    // A few pixels of travel before a drag begins, so clicking a card's link is still a click.
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const { mutate: move } = useMutation({
    mutationFn: ({ issueId, body }: { issueId: string; body: MoveIssueRequest }) =>
      issuesApi.move(issueId, body),
    onError: (moveError) => {
      toast.failure(moveError, 'Could not move that issue');
      void queryClient.invalidateQueries({ queryKey: boardKey });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.project(project.id) });
    },
  });

  function onDragStart(event: DragStartEvent) {
    const activeId = String(event.active.id);
    const issue = findIssue(columns, activeId);
    if (!issue) {
      return;
    }
    const status = columnOf(columns, activeId);
    setActiveIssue(issue);
    setOrigin({ status, ...neighbours(issuesIn(columns, status), activeId) });
  }

  function onDragOver(event: DragOverEvent) {
    const { active, over } = event;
    if (!over) {
      return;
    }
    const activeId = String(active.id);
    const overId = String(over.id);
    const from = columnOf(columns, activeId);
    const to = columnOf(columns, overId);
    if (from === to) {
      return;
    }
    writeColumns(moveAcross(columns, activeId, from, to, overId));
  }

  function onDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    const dragged = activeIssue;
    const from = origin;
    setActiveIssue(null);
    setOrigin(null);

    if (!over || !dragged || !from) {
      return;
    }

    const activeId = String(active.id);
    const overId = String(over.id);
    const status = columnOf(columns, activeId);
    let next = columns;

    if (status === columnOf(columns, overId) && activeId !== overId) {
      const issues = issuesIn(columns, status);
      const fromIndex = issues.findIndex((issue) => issue.id === activeId);
      const toIndex = issues.findIndex((issue) => issue.id === overId);
      if (fromIndex !== -1 && toIndex !== -1) {
        next = withIssues(columns, status, arrayMove(issues, fromIndex, toIndex));
        writeColumns(next);
      }
    }

    const position = neighbours(issuesIn(next, status), activeId);

    // A drop that lands a card back between the same two neighbours is not a move.
    if (status === from.status && samePosition(position, from)) {
      return;
    }

    const body: MoveIssueRequest = { status, ...position };
    move({ issueId: activeId, body });
  }

  if (error) {
    return (
      <Alert variant="danger" title="Could not load the board">
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

  return (
    <>
      <div className="mb-4 flex items-center justify-between gap-3">
        <p className="text-muted text-sm">
          {writer
            ? 'Drag a card to reorder it or move it between columns.'
            : 'You have read-only access to this project.'}
        </p>
        {writer && (
          <Button
            size="sm"
            onClick={() => {
              setCreatingIn('TODO');
            }}
          >
            <Plus className="size-4" aria-hidden="true" />
            New issue
          </Button>
        )}
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={onDragStart}
        onDragOver={onDragOver}
        onDragEnd={onDragEnd}
        onDragCancel={() => {
          setActiveIssue(null);
          setOrigin(null);
          // The cache was already reordered as the card moved, so the server has to say what
          // the board actually looks like again.
          void queryClient.invalidateQueries({ queryKey: boardKey });
        }}
      >
        <div className="flex gap-4 overflow-x-auto pb-4">
          {columns.map((column) => (
            <BoardColumn
              key={column.status}
              column={column}
              projectId={project.id}
              draggable={writer}
              onAdd={writer ? setCreatingIn : undefined}
            />
          ))}
        </div>

        <DragOverlay>
          {activeIssue && (
            <IssueCard issue={activeIssue} projectId={project.id} overlay handleProps={{}} />
          )}
        </DragOverlay>
      </DndContext>

      {creatingIn !== null && (
        <IssueFormDialog
          initialStatus={creatingIn}
          onClose={() => {
            setCreatingIn(null);
          }}
        />
      )}
    </>
  );
}
