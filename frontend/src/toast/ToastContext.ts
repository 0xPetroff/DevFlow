import { createContext } from 'react';

export type ToastTone = 'success' | 'danger' | 'info';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

export interface ToastContextValue {
  toasts: Toast[];
  show: (tone: ToastTone, message: string) => void;
  dismiss: (id: number) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);
