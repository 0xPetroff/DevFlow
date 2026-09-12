import { ChevronLeft, ChevronRight } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import type { PageResponse } from '@/types/api';

interface PaginationProps {
  page: Pick<PageResponse<unknown>, 'page' | 'size' | 'totalElements' | 'totalPages' | 'first' | 'last'>;
  onChange: (page: number) => void;
  noun: string;
}

export function Pagination({ page, onChange, noun }: PaginationProps) {
  if (page.totalElements === 0) {
    return null;
  }

  const from = page.page * page.size + 1;
  const to = Math.min(from + page.size - 1, page.totalElements);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <p className="text-muted text-xs tabular-nums">
        {from}-{to} of {page.totalElements} {noun}
      </p>

      {page.totalPages > 1 && (
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="secondary"
            disabled={page.first}
            onClick={() => {
              onChange(page.page - 1);
            }}
          >
            <ChevronLeft className="size-3.5" aria-hidden="true" />
            Previous
          </Button>
          <span className="text-muted text-xs tabular-nums">
            Page {page.page + 1} of {page.totalPages}
          </span>
          <Button
            size="sm"
            variant="secondary"
            disabled={page.last}
            onClick={() => {
              onChange(page.page + 1);
            }}
          >
            Next
            <ChevronRight className="size-3.5" aria-hidden="true" />
          </Button>
        </div>
      )}
    </div>
  );
}
