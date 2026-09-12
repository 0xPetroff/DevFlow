import { useQuery } from '@tanstack/react-query';
import { ListChecks, Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';

import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Badge, LabelChip } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { Pagination } from '@/components/ui/Pagination';
import { Select } from '@/components/ui/Select';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatDate } from '@/lib/format';
import {
  ISSUE_PRIORITY_LABELS,
  ISSUE_PRIORITY_TONES,
  ISSUE_STATUS_LABELS,
  ISSUE_STATUS_TONES,
  ISSUE_TYPE_LABELS,
} from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { useDebounced } from '@/lib/useDebounced';
import { IssueFormDialog } from '@/issues/IssueFormDialog';
import { issuesApi, type IssueListParams } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import {
  ISSUE_PRIORITIES,
  ISSUE_STATUSES,
  type IssuePriority,
  type IssueStatus,
} from '@/types/api';

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'priority,desc', label: 'Priority' },
  { value: 'dueDate,asc', label: 'Due date' },
  { value: 'issueNumber,asc', label: 'Issue number' },
];

export function IssuesPage() {
  const { project, members, writer } = useProject();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<IssueStatus | ''>('');
  const [priority, setPriority] = useState<IssuePriority | ''>('');
  const [assigneeId, setAssigneeId] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);

  const debouncedSearch = useDebounced(search);
  const params: IssueListParams = {
    q: debouncedSearch || undefined,
    status: status ? [status] : undefined,
    priority: priority ? [priority] : undefined,
    assigneeId: assigneeId && assigneeId !== 'none' ? assigneeId : undefined,
    unassigned: assigneeId === 'none' ? true : undefined,
    sort,
    page,
    size: 25,
  };

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectIssues(project.id, params),
    queryFn: () => issuesApi.list(project.id, params),
    placeholderData: (previous) => previous,
  });

  function resetPage<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value);
      setPage(0);
    };
  }

  return (
    <>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-ink text-base font-semibold">Issues</h2>
        {writer && (
          <Button
            size="sm"
            onClick={() => {
              setCreating(true);
            }}
          >
            <Plus className="size-4" aria-hidden="true" />
            New issue
          </Button>
        )}
      </div>

      <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <div className="relative xl:col-span-2">
          <Search
            className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            aria-hidden="true"
          />
          <Input
            value={search}
            onChange={(event) => {
              resetPage(setSearch)(event.target.value);
            }}
            placeholder="Search title or description"
            aria-label="Search issues"
            className="pl-9"
          />
        </div>

        <Select
          value={status}
          aria-label="Filter by status"
          onChange={(event) => {
            resetPage(setStatus)(event.target.value as IssueStatus | '');
          }}
        >
          <option value="">All statuses</option>
          {ISSUE_STATUSES.map((value) => (
            <option key={value} value={value}>
              {ISSUE_STATUS_LABELS[value]}
            </option>
          ))}
        </Select>

        <Select
          value={priority}
          aria-label="Filter by priority"
          onChange={(event) => {
            resetPage(setPriority)(event.target.value as IssuePriority | '');
          }}
        >
          <option value="">All priorities</option>
          {ISSUE_PRIORITIES.map((value) => (
            <option key={value} value={value}>
              {ISSUE_PRIORITY_LABELS[value]}
            </option>
          ))}
        </Select>

        <Select
          value={assigneeId}
          aria-label="Filter by assignee"
          onChange={(event) => {
            resetPage(setAssigneeId)(event.target.value);
          }}
        >
          <option value="">Anyone</option>
          <option value="none">Unassigned</option>
          {members.map((member) => (
            <option key={member.user.id} value={member.user.id}>
              {member.user.fullName}
            </option>
          ))}
        </Select>

        <Select
          value={sort}
          aria-label="Sort issues"
          onChange={(event) => {
            resetPage(setSort)(event.target.value);
          }}
        >
          {SORTS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
      </div>

      {error ? (
        <Alert variant="danger" title="Could not load issues">
          {errorMessage(error)}
        </Alert>
      ) : isPending ? (
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={ListChecks}
          title="No issues match"
          description="Adjust the filters, or create the first one."
        />
      ) : (
        <div className="flex flex-col gap-4">
          <ul className="border-line bg-surface divide-line divide-y overflow-hidden rounded-lg border">
            {data.content.map((issue) => (
              <li key={issue.id}>
                <Link
                  to={`/projects/${project.id}/issues/${issue.id}`}
                  className="hover:bg-elevated flex flex-wrap items-center gap-x-3 gap-y-2 px-4 py-3 transition-colors"
                >
                  <span className="text-faint w-20 shrink-0 font-mono text-xs">{issue.key}</span>
                  <span className="text-ink min-w-48 flex-1 text-sm font-medium">
                    {issue.title}
                  </span>

                  {issue.labels.map((label) => (
                    <LabelChip key={label.id} name={label.name} color={label.color} />
                  ))}

                  <Badge tone={ISSUE_STATUS_TONES[issue.status]}>
                    {ISSUE_STATUS_LABELS[issue.status]}
                  </Badge>
                  <Badge tone={ISSUE_PRIORITY_TONES[issue.priority]}>
                    {ISSUE_PRIORITY_LABELS[issue.priority]}
                  </Badge>
                  <span className="text-muted hidden w-24 text-xs sm:inline">
                    {ISSUE_TYPE_LABELS[issue.type]}
                  </span>
                  <span className="text-faint hidden w-24 text-xs sm:inline">
                    {issue.dueDate ? formatDate(issue.dueDate) : ''}
                  </span>

                  {issue.assignee ? (
                    <Avatar
                      name={issue.assignee.fullName}
                      color={issue.assignee.avatarColor}
                      size="sm"
                    />
                  ) : (
                    <span className="size-7" />
                  )}
                </Link>
              </li>
            ))}
          </ul>
          <Pagination page={data} onChange={setPage} noun="issues" />
        </div>
      )}

      {creating && (
        <IssueFormDialog
          onClose={() => {
            setCreating(false);
          }}
        />
      )}
    </>
  );
}
