import { cn } from '@/lib/cn';
import { initials } from '@/lib/format';

const SIZES = {
  sm: 'size-7 text-[11px]',
  md: 'size-9 text-xs',
  lg: 'size-14 text-lg',
} as const;

interface AvatarProps {
  name: string;
  color: string;
  size?: keyof typeof SIZES;
  className?: string;
}

export function Avatar({ name, color, size = 'md', className }: AvatarProps) {
  return (
    <span
      title={name}
      style={{ backgroundColor: color }}
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white select-none',
        SIZES[size],
        className,
      )}
    >
      {initials(name)}
    </span>
  );
}
