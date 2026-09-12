import type { ReactNode } from 'react';

import { cn } from '@/lib/cn';
import type { Tone } from '@/lib/labels';

const TONES: Record<Tone, string> = {
  neutral: 'bg-elevated text-muted border-line',
  accent: 'bg-accent-soft text-accent border-accent/30',
  success: 'bg-success-soft text-success border-success/30',
  warning: 'bg-warning-soft text-warning border-warning/30',
  danger: 'bg-danger-soft text-danger border-danger/30',
  info: 'bg-info-soft text-info border-info/30',
};

interface BadgeProps {
  tone?: Tone;
  className?: string;
  children: ReactNode;
}

export function Badge({ tone = 'neutral', className, children }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium whitespace-nowrap',
        TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

interface LabelChipProps {
  name: string;
  color: string;
  className?: string;
}

/**
 * A label's colour comes from the database, so the chip is tinted at runtime rather than through
 * a class. The text keeps the theme's ink colour and the fill stays faint, which is the only way
 * one arbitrary hex stays readable in both themes.
 */
export function LabelChip({ name, color, className }: LabelChipProps) {
  return (
    <span
      style={{ backgroundColor: `${color}22`, borderColor: `${color}66` }}
      className={cn(
        'text-ink inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-xs whitespace-nowrap',
        className,
      )}
    >
      <span style={{ backgroundColor: color }} className="size-2 shrink-0 rounded-full" />
      {name}
    </span>
  );
}
