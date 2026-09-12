import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';
import type { ReactNode } from 'react';

import { cn } from '@/lib/cn';

const VARIANTS = {
  info: { wrapper: 'bg-info-soft text-info border-info/30', Icon: Info },
  success: { wrapper: 'bg-success-soft text-success border-success/30', Icon: CheckCircle2 },
  warning: { wrapper: 'bg-warning-soft text-warning border-warning/30', Icon: AlertTriangle },
  danger: { wrapper: 'bg-danger-soft text-danger border-danger/30', Icon: XCircle },
} as const;

interface AlertProps {
  variant?: keyof typeof VARIANTS;
  title?: string;
  className?: string;
  children: ReactNode;
}

export function Alert({ variant = 'info', title, className, children }: AlertProps) {
  const { wrapper, Icon } = VARIANTS[variant];
  return (
    <div
      role={variant === 'danger' ? 'alert' : 'status'}
      className={cn('flex gap-3 rounded-md border px-3 py-2.5 text-sm', wrapper, className)}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <div className="min-w-0 flex-1">
        {title && <p className="font-medium">{title}</p>}
        <div className={cn(title && 'mt-0.5 opacity-90')}>{children}</div>
      </div>
    </div>
  );
}
