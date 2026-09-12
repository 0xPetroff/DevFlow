import { Monitor, Moon, Sun } from 'lucide-react';

import { cn } from '@/lib/cn';
import { useTheme } from '@/theme/useTheme';
import type { ThemePreference } from '@/theme/ThemeContext';

const ORDER: ThemePreference[] = ['system', 'light', 'dark'];

const ICONS = {
  system: Monitor,
  light: Sun,
  dark: Moon,
} as const;

const LABELS: Record<ThemePreference, string> = {
  system: 'Theme: follows your system',
  light: 'Theme: light',
  dark: 'Theme: dark',
};

export function ThemeToggle({ className }: { className?: string }) {
  const { preference, setPreference } = useTheme();
  const Icon = ICONS[preference];
  const next = ORDER[(ORDER.indexOf(preference) + 1) % ORDER.length] ?? 'system';

  return (
    <button
      type="button"
      onClick={() => {
        setPreference(next);
      }}
      title={LABELS[preference]}
      aria-label={LABELS[preference]}
      className={cn(
        'text-muted hover:bg-elevated hover:text-ink inline-flex size-9 cursor-pointer items-center justify-center rounded-md transition-colors',
        className,
      )}
    >
      <Icon className="size-4" aria-hidden="true" />
    </button>
  );
}
