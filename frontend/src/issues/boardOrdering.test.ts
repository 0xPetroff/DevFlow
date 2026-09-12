import { describe, expect, it } from 'vitest';

import {
  columnOf,
  issuesIn,
  moveAcross,
  neighbours,
  samePosition,
  withIssues,
} from '@/issues/boardOrdering';
import { issueSummary } from '@/test/fixtures';
import type { BoardColumn } from '@/types/api';

function board(): BoardColumn[] {
  return [
    {
      status: 'TODO',
      total: 3,
      issues: [
        issueSummary({ id: 'a', key: 'DEVF-1' }),
        issueSummary({ id: 'b', key: 'DEVF-2' }),
        issueSummary({ id: 'c', key: 'DEVF-3' }),
      ],
    },
    { status: 'IN_PROGRESS', total: 0, issues: [] },
    { status: 'IN_REVIEW', total: 1, issues: [issueSummary({ id: 'd', key: 'DEVF-4' })] },
    { status: 'DONE', total: 0, issues: [] },
  ];
}

describe('columnOf', () => {
  it('resolves a card to the column holding it', () => {
    expect(columnOf(board(), 'b')).toBe('TODO');
    expect(columnOf(board(), 'd')).toBe('IN_REVIEW');
  });

  it('treats a status as its own column, which is how an empty column is dropped into', () => {
    expect(columnOf(board(), 'DONE')).toBe('DONE');
  });
});

describe('neighbours', () => {
  it('reports the cards on either side', () => {
    expect(neighbours(issuesIn(board(), 'TODO'), 'b')).toEqual({
      previousIssueId: 'a',
      nextIssueId: 'c',
    });
  });

  it('reports null at each end rather than wrapping', () => {
    const issues = issuesIn(board(), 'TODO');
    expect(neighbours(issues, 'a')).toEqual({ previousIssueId: null, nextIssueId: 'b' });
    expect(neighbours(issues, 'c')).toEqual({ previousIssueId: 'b', nextIssueId: null });
  });

  it('reports both neighbours as null for the only card in a column', () => {
    expect(neighbours(issuesIn(board(), 'IN_REVIEW'), 'd')).toEqual({
      previousIssueId: null,
      nextIssueId: null,
    });
  });

  it('reports both as null for a card that is not there at all', () => {
    // findIndex returns -1, and issues[-2] must not be read as the last element.
    expect(neighbours(issuesIn(board(), 'TODO'), 'missing')).toEqual({
      previousIssueId: null,
      nextIssueId: null,
    });
  });
});

describe('samePosition', () => {
  it('recognises a drop that changed nothing, so no request is sent', () => {
    const before = neighbours(issuesIn(board(), 'TODO'), 'b');
    const after = neighbours(issuesIn(board(), 'TODO'), 'b');
    expect(samePosition(before, after)).toBe(true);
  });

  it('recognises a real move', () => {
    const before = neighbours(issuesIn(board(), 'TODO'), 'a');
    const after = neighbours(issuesIn(board(), 'TODO'), 'c');
    expect(samePosition(before, after)).toBe(false);
  });
});

describe('moveAcross', () => {
  it('moves the card and its total to the column it was dropped on', () => {
    const next = moveAcross(board(), 'b', 'TODO', 'IN_REVIEW', 'd');

    expect(issuesIn(next, 'TODO').map((issue) => issue.id)).toEqual(['a', 'c']);
    expect(issuesIn(next, 'IN_REVIEW').map((issue) => issue.id)).toEqual(['b', 'd']);
    expect(next.find((column) => column.status === 'TODO')?.total).toBe(2);
    expect(next.find((column) => column.status === 'IN_REVIEW')?.total).toBe(2);
  });

  it('rewrites the moved card status so the card renders in its new column', () => {
    const next = moveAcross(board(), 'b', 'TODO', 'DONE', 'DONE');
    expect(issuesIn(next, 'DONE')[0]?.status).toBe('DONE');
  });

  it('appends to an empty column, where there is no card to insert before', () => {
    const next = moveAcross(board(), 'a', 'TODO', 'DONE', 'DONE');
    expect(issuesIn(next, 'DONE').map((issue) => issue.id)).toEqual(['a']);
    expect(next.find((column) => column.status === 'DONE')?.total).toBe(1);
  });

  it('leaves the board alone when the source and target are the same column', () => {
    const before = board();
    expect(moveAcross(before, 'a', 'TODO', 'TODO', 'c')).toBe(before);
  });

  it('leaves the board alone for an id it cannot find', () => {
    const before = board();
    expect(moveAcross(before, 'missing', 'TODO', 'DONE', 'DONE')).toBe(before);
  });

  /**
   * A column reports the true number of issues in that status while its array only holds the page
   * the board endpoint returned, so totals have to move by a delta rather than be recounted.
   */
  it('adjusts totals by one even when the column is showing fewer cards than it counts', () => {
    const capped: BoardColumn[] = [
      { status: 'TODO', total: 120, issues: [issueSummary({ id: 'a' })] },
      { status: 'IN_PROGRESS', total: 80, issues: [] },
      { status: 'IN_REVIEW', total: 0, issues: [] },
      { status: 'DONE', total: 0, issues: [] },
    ];

    const next = moveAcross(capped, 'a', 'TODO', 'IN_PROGRESS', 'IN_PROGRESS');

    expect(next.find((column) => column.status === 'TODO')?.total).toBe(119);
    expect(next.find((column) => column.status === 'IN_PROGRESS')?.total).toBe(81);
  });
});

describe('withIssues', () => {
  it('replaces one column and leaves the rest untouched', () => {
    const before = board();
    const next = withIssues(before, 'TODO', []);

    expect(issuesIn(next, 'TODO')).toEqual([]);
    expect(next.find((column) => column.status === 'IN_REVIEW')).toBe(
      before.find((column) => column.status === 'IN_REVIEW'),
    );
  });
});
