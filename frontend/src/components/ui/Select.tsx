import type { SelectHTMLAttributes } from 'react';

import { cn } from '@/lib/cn';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

/**
 * A native select rather than a custom listbox. It gets keyboard support, screen reader support
 * and the platform's own picker on a phone for free, and none of the options here need markup.
 */
export function Select({ className, invalid, children, ...props }: SelectProps) {
  return (
    <select
      aria-invalid={invalid === true || undefined}
      className={cn(
        'border-line bg-surface text-ink hover:border-line-strong focus:border-accent aria-invalid:border-danger',
        'h-10 w-full cursor-pointer appearance-none rounded-md border pr-8 pl-3 text-sm transition-colors focus:outline-none',
        'bg-[length:1rem] bg-[right_0.5rem_center] bg-no-repeat',
        'disabled:cursor-not-allowed disabled:opacity-60',
        className,
      )}
      style={{
        backgroundImage:
          "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='none' stroke='%238b94a5' stroke-width='1.5'%3E%3Cpath d='M4 6l4 4 4-4'/%3E%3C/svg%3E\")",
      }}
      {...props}
    >
      {children}
    </select>
  );
}
