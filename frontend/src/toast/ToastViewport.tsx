import { CheckCircle2, Info, X, XCircle } from 'lucide-react';

import { cn } from '@/lib/cn';
import type { Toast, ToastTone } from '@/toast/ToastContext';

const TONES: Record<ToastTone, { wrapper: string; Icon: typeof Info }> = {
  success: { wrapper: 'border-success/40 text-success', Icon: CheckCircle2 },
  danger: { wrapper: 'border-danger/40 text-danger', Icon: XCircle },
  info: { wrapper: 'border-info/40 text-info', Icon: Info },
};

interface ToastViewportProps {
  toasts: Toast[];
  onDismiss: (id: number) => void;
}

export function ToastViewport({ toasts, onDismiss }: ToastViewportProps) {
  return (
    <div
      // Polite rather than assertive: these confirm an action the user just took, and an
      // assertive region would interrupt whatever a screen reader is in the middle of.
      aria-live="polite"
      className="pointer-events-none fixed inset-x-0 bottom-0 z-60 flex flex-col items-center gap-2 p-4 sm:items-end"
    >
      {toasts.map((toast) => {
        const { wrapper, Icon } = TONES[toast.tone];
        return (
          <div
            key={toast.id}
            className={cn(
              'bg-surface shadow-pop pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-lg border px-3.5 py-3',
              wrapper,
            )}
          >
            <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <p className="text-ink min-w-0 flex-1 text-sm">{toast.message}</p>
            <button
              type="button"
              aria-label="Dismiss notification"
              onClick={() => {
                onDismiss(toast.id);
              }}
              className="text-faint hover:text-ink -mt-0.5 -mr-1 inline-flex size-6 shrink-0 cursor-pointer items-center justify-center rounded"
            >
              <X className="size-3.5" aria-hidden="true" />
            </button>
          </div>
        );
      })}
    </div>
  );
}
