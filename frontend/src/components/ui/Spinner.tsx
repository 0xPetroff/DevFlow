import { cn } from '@/lib/cn';

const SIZES = {
  sm: 'size-4 border-2',
  md: 'size-5 border-2',
  lg: 'size-8 border-[3px]',
} as const;

interface SpinnerProps {
  size?: keyof typeof SIZES;
  className?: string;
  label?: string;
}

export function Spinner({ size = 'md', className, label = 'Loading' }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label={label}
      className={cn(
        'inline-block animate-spin rounded-full border-current border-t-transparent opacity-70',
        SIZES[size],
        className,
      )}
    />
  );
}

export function FullPageSpinner() {
  return (
    <div className="bg-canvas text-muted flex min-h-screen items-center justify-center">
      <Spinner size="lg" label="Loading DevFlow" />
    </div>
  );
}
