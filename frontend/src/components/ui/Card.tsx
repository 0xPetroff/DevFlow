import type { ReactNode } from 'react';

import { cn } from '@/lib/cn';

interface CardProps {
  className?: string;
  children: ReactNode;
}

export function Card({ className, children }: CardProps) {
  return (
    <section className={cn('border-line bg-surface shadow-card rounded-lg border', className)}>
      {children}
    </section>
  );
}

interface CardHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function CardHeader({ title, description, actions }: CardHeaderProps) {
  return (
    <header className="border-line flex items-start justify-between gap-4 border-b px-5 py-4">
      <div className="min-w-0">
        <h2 className="text-ink text-sm font-semibold">{title}</h2>
        {description && <p className="text-muted mt-1 text-xs">{description}</p>}
      </div>
      {actions}
    </header>
  );
}

export function CardBody({ className, children }: CardProps) {
  return <div className={cn('px-5 py-4', className)}>{children}</div>;
}
