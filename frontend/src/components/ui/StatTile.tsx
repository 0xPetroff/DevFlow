import type { ComponentType } from 'react';

import { cn } from '@/lib/cn';

interface StatTileProps {
  label: string;
  value: string | number;
  hint?: string;
  icon?: ComponentType<{ className?: string }>;
  tone?: 'default' | 'success' | 'warning' | 'danger';
}

const TONES = {
  default: 'text-ink',
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
} as const;

export function StatTile({ label, value, hint, icon: Icon, tone = 'default' }: StatTileProps) {
  return (
    <div className="border-line bg-surface shadow-card rounded-lg border p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-muted text-xs font-medium tracking-wide uppercase">{label}</p>
        {Icon && <Icon className="text-faint size-4" />}
      </div>
      <p className={cn('mt-2 text-2xl font-semibold tabular-nums', TONES[tone])}>{value}</p>
      {hint && <p className="text-faint mt-1 text-xs">{hint}</p>}
    </div>
  );
}
