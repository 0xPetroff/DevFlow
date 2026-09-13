import { cn } from '@/lib/cn';

export function Logo({
  className,
  showWordmark = true,
}: {
  className?: string;
  showWordmark?: boolean;
}) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <svg viewBox="0 0 32 32" className="size-7 shrink-0" aria-hidden="true">
        <rect width="32" height="32" rx="7" className="fill-accent" />
        <path
          d="M10 9.5 L10 22.5 M10 9.5 L18 9.5 A4.2 4.2 0 0 1 18 18 L10 18"
          fill="none"
          stroke="white"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="22.5" cy="22" r="2.6" fill="white" />
      </svg>
      {showWordmark && (
        <span className="text-ink text-base font-semibold tracking-tight">DevFlow</span>
      )}
    </span>
  );
}
