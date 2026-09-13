import { X } from 'lucide-react';
import { NavLink } from 'react-router';

import { NAV_ITEMS } from '@/components/layout/navigation';
import { Logo } from '@/components/ui/Logo';
import { useAuth } from '@/auth/useAuth';
import { cn } from '@/lib/cn';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const { hasRole } = useAuth();
  const items = NAV_ITEMS.filter((item) => !item.requiresRole || hasRole(item.requiresRole));

  return (
    <>
      {open && (
        <button
          type="button"
          aria-label="Close navigation"
          onClick={onClose}
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
        />
      )}

      <nav
        aria-label="Main"
        className={cn(
          'border-line bg-surface fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r transition-transform duration-200',
          'lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="border-line flex h-14 items-center justify-between border-b px-4">
          <Logo />
          <button
            type="button"
            onClick={onClose}
            aria-label="Close navigation"
            className="text-muted hover:bg-elevated hover:text-ink inline-flex size-8 cursor-pointer items-center justify-center rounded-md lg:hidden"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>

        <ul className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-3">
          {items.map(({ to, label, icon: Icon, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end ?? false}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-accent-soft text-accent'
                      : 'text-muted hover:bg-elevated hover:text-ink',
                  )
                }
              >
                <Icon className="size-4 shrink-0" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>

        <p className="border-line text-faint border-t px-4 py-3 font-mono text-[11px]">
          DevFlow v1.0.0
        </p>
      </nav>
    </>
  );
}
