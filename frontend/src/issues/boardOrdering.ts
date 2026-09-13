import { ISSUE_STATUSES, type BoardColumn, type IssueStatus, type IssueSummary } from '@/types/api';

/**
 * The board's reordering rules, kept apart from the component because this is where the subtle
 * cases live: a drop into an empty column, a card that ends up back where it started, and the
 * neighbours the server needs in order to compute a position itself.
 */

export interface Neighbours {
  previousIssueId: string | null;
  nextIssueId: string | null;
}

export function issuesIn(columns: BoardColumn[], status: IssueStatus): IssueSummary[] {
  return columns.find((column) => column.status === status)?.issues ?? [];
}

export function withIssues(
  columns: BoardColumn[],
  status: IssueStatus,
  issues: IssueSummary[],
): BoardColumn[] {
  return columns.map((column) => (column.status === status ? { ...column, issues } : column));
}

export function findIssue(columns: BoardColumn[], id: string): IssueSummary | null {
  for (const column of columns) {
    const issue = column.issues.find((candidate) => candidate.id === id);
    if (issue) {
      return issue;
    }
  }
  return null;
}

/** An id is either a card's or, when a card hovers over empty space, the column's own status. */
export function columnOf(columns: BoardColumn[], id: string): IssueStatus {
  const asStatus = ISSUE_STATUSES.find((status) => status === id);
  if (asStatus) {
    return asStatus;
  }
  return columns.find((column) => column.issues.some((issue) => issue.id === id))?.status ?? 'TODO';
}

/**
 * What the server is told about a drop. It is deliberately the card's two neighbours rather than
 * a number: the server owns the ordering, so two people dragging at once cannot invent
 * conflicting positions, and a drag stays a single update.
 */
export function neighbours(issues: IssueSummary[], issueId: string): Neighbours {
  const index = issues.findIndex((issue) => issue.id === issueId);
  // A card that is not in the list has no neighbours. Without this the -1 from findIndex would
  // make index + 1 read the first card and report it as the one to sit before.
  if (index === -1) {
    return { previousIssueId: null, nextIssueId: null };
  }
  return {
    previousIssueId: issues[index - 1]?.id ?? null,
    nextIssueId: issues[index + 1]?.id ?? null,
  };
}

export function samePosition(a: Neighbours, b: Neighbours): boolean {
  return a.previousIssueId === b.previousIssueId && a.nextIssueId === b.nextIssueId;
}

/**
 * Relocates a card between columns mid-drag. The totals move with it, because a column's total
 * counts every issue in that status while the array only holds the page the board returned.
 */
export function moveAcross(
  columns: BoardColumn[],
  activeId: string,
  from: IssueStatus,
  to: IssueStatus,
  overId: string,
): BoardColumn[] {
  const issue = findIssue(columns, activeId);
  if (!issue || from === to) {
    return columns;
  }

  const target = issuesIn(columns, to);
  const overIndex = target.findIndex((candidate) => candidate.id === overId);
  const insertAt = overIndex === -1 ? target.length : overIndex;

  return columns.map((column) => {
    if (column.status === from) {
      return {
        ...column,
        total: column.total - 1,
        issues: column.issues.filter((candidate) => candidate.id !== activeId),
      };
    }
    if (column.status === to) {
      const issues = [...column.issues];
      issues.splice(insertAt, 0, { ...issue, status: to });
      return { ...column, total: column.total + 1, issues };
    }
    return column;
  });
}
