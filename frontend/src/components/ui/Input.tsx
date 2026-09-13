import type { InputHTMLAttributes, TextareaHTMLAttributes } from 'react';

import { cn } from '@/lib/cn';

const CONTROL =
  'w-full rounded-md border bg-surface px-3 text-sm text-ink placeholder:text-faint transition-colors ' +
  'border-line hover:border-line-strong focus:border-accent focus:outline-none ' +
  'disabled:cursor-not-allowed disabled:opacity-60 aria-invalid:border-danger';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export function Input({ className, invalid, ...props }: InputProps) {
  return (
    <input
      aria-invalid={invalid === true || undefined}
      className={cn(CONTROL, 'h-10', className)}
      {...props}
    />
  );
}

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export function Textarea({ className, invalid, rows = 4, ...props }: TextareaProps) {
  return (
    <textarea
      rows={rows}
      aria-invalid={invalid === true || undefined}
      className={cn(CONTROL, 'resize-y py-2 leading-relaxed', className)}
      {...props}
    />
  );
}
