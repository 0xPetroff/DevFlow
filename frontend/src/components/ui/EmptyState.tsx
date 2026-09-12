import type { ComponentType, ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ComponentType<{ className?: string }>;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="border-line flex flex-col items-center gap-3 rounded-lg border border-dashed px-6 py-14 text-center">
      {Icon && <Icon className="text-faint size-6" />}
      <div>
        <p className="text-ink text-sm font-medium">{title}</p>
        {description && <p className="text-muted mt-1 text-sm">{description}</p>}
      </div>
      {action}
    </div>
  );
}
