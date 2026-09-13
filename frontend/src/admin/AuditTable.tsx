import { History, Search } from 'lucide-react';
import { useState } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { Pagination } from '@/components/ui/Pagination';
import { Select } from '@/components/ui/Select';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatDateTime, formatRelative } from '@/lib/format';
import { AUDIT_ACTION_LABELS, type Tone } from '@/lib/labels';
import type { AuditListParams } from '@/admin/adminApi';
import type { AuditAction, AuditLogResponse, PageResponse } from '@/types/api';

interface AuditTableProps {
  data: PageResponse<AuditLogResponse> | undefined;
  isPending: boolean;
  error: unknown;
  params: AuditListParams;
  onParamsChange: (params: AuditListParams) => void;
}

export function AuditTable({ data, isPending, error, params, onParamsChange }: AuditTableProps) {
  const [search, setSearch] = useState(params.q ?? '');

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <div className="relative">
          <Search
            className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            aria-hidden="true"
          />
          <Input
            value={search}
            aria-label="Search the activity"
            placeholder="Search summaries and actors"
            className="pl-9"
            onChange={(event) => {
              setSearch(event.target.value);
              onParamsChange({ ...params, q: event.target.value || undefined, page: 0 });
            }}
          />
        </div>

        <Select
          value={params.action ?? ''}
          aria-label="Filter by action"
          onChange={(event) => {
            onParamsChange({
              ...params,
              action: (event.target.value || undefined) as AuditAction | undefined,
              page: 0,
            });
          }}
        >
          <option value="">Every action</option>
          {Object.entries(AUDIT_ACTION_LABELS).map(([action, label]) => (
            <option key={action} value={action}>
              {label}
            </option>
          ))}
        </Select>
      </div>

      {error ? (
        <Alert variant="danger" title="Could not load the activity">
          {errorMessage(error)}
        </Alert>
      ) : isPending || !data ? (
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={History}
          title="Nothing recorded"
          description="Actions worth keeping a record of will show up here."
        />
      ) : (
        <>
          <ul
            aria-label="Activity"
            className="border-line bg-surface divide-line divide-y overflow-hidden rounded-lg border"
          >
            {data.content.map((entry) => (
              <li key={entry.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-3">
                <Badge tone={toneFor(entry.action)}>{AUDIT_ACTION_LABELS[entry.action]}</Badge>
                <span className="text-ink min-w-48 flex-1 text-sm">{entry.summary}</span>
                <span className="text-muted text-xs">{entry.actorLabel ?? 'system'}</span>
                <span className="text-faint text-xs" title={formatDateTime(entry.createdAt)}>
                  {formatRelative(entry.createdAt)}
                </span>
              </li>
            ))}
          </ul>
          <Pagination
            page={data}
            noun="entries"
            onChange={(page) => {
              onParamsChange({ ...params, page });
            }}
          />
        </>
      )}
    </div>
  );
}

function toneFor(action: AuditAction): Tone {
  if (action.endsWith('_DELETED') || action.endsWith('_REMOVED') || action === 'DEPLOYMENT_FAILED') {
    return 'danger';
  }
  if (action.endsWith('_CREATED') || action === 'DEPLOYMENT_SUCCEEDED') {
    return 'success';
  }
  if (action.startsWith('DEPLOYMENT') || action.startsWith('API_KEY')) {
    return 'info';
  }
  return 'neutral';
}
