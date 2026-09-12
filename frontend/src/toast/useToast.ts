import { use, useMemo } from 'react';

import { errorMessage } from '@/lib/apiError';
import { ToastContext } from '@/toast/ToastContext';

export interface ToastApi {
  success: (message: string) => void;
  info: (message: string) => void;
  /** Takes the thrown value rather than a string, so no caller has to unwrap an ApiError itself. */
  failure: (error: unknown, fallback?: string) => void;
}

export function useToast(): ToastApi {
  const context = use(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside a ToastProvider');
  }

  const { show } = context;
  return useMemo(
    () => ({
      success: (message: string) => {
        show('success', message);
      },
      info: (message: string) => {
        show('info', message);
      },
      failure: (error: unknown, fallback?: string) => {
        show('danger', errorMessage(error) || (fallback ?? 'Something went wrong'));
      },
    }),
    [show],
  );
}
