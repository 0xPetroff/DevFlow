import type { ButtonHTMLAttributes, ReactNode } from 'react';

import { Spinner } from '@/components/ui/Spinner';
import { cn } from '@/lib/cn';

const VARIANTS = {
  primary: 'bg-accent text-accent-ink hover:bg-accent-hover shadow-card',
  secondary: 'bg-elevated text-ink border border-line hover:border-line-strong',
  ghost: 'text-muted hover:bg-elevated hover:text-ink',
  danger: 'bg-danger text-white hover:bg-danger-hover shadow-card',
} as const;

const SIZES = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
} as const;

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: keyof typeof VARIANTS;
  size?: keyof typeof SIZES;
  loading?: boolean;
  fullWidth?: boolean;
  children?: ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  className,
  disabled,
  type = 'button',
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled === true || loading}
      aria-busy={loading}
      className={cn(
        'inline-flex cursor-pointer items-center justify-center rounded-md font-medium whitespace-nowrap transition-colors',
        'disabled:pointer-events-none disabled:opacity-55',
        VARIANTS[variant],
        SIZES[size],
        fullWidth && 'w-full',
        className,
      )}
      {...props}
    >
      {loading && <Spinner size="sm" label="" />}
      {children}
    </button>
  );
}
