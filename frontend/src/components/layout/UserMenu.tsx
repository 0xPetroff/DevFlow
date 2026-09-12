import { ChevronDown, LogOut, Settings } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';

import { useAuth } from '@/auth/useAuth';
import { Avatar } from '@/components/ui/Avatar';
import { ROLE_LABELS } from '@/lib/roles';

export function UserMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onPointerDown = (event: PointerEvent) => {
      if (!container.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  if (!user) {
    return null;
  }

  return (
    <div className="relative" ref={container}>
      <button
        type="button"
        onClick={() => {
          setOpen((value) => !value);
        }}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Account menu"
        className="hover:bg-elevated flex cursor-pointer items-center gap-2 rounded-md py-1 pr-2 pl-1 transition-colors"
      >
        <Avatar name={user.fullName} color={user.avatarColor} size="sm" />
        <span className="text-ink hidden text-sm font-medium sm:inline">{user.fullName}</span>
        <ChevronDown className="text-faint size-3.5" aria-hidden="true" />
      </button>

      {open && (
        <div
          role="menu"
          className="border-line bg-elevated shadow-pop absolute right-0 z-50 mt-2 w-60 overflow-hidden rounded-lg border"
        >
          <div className="border-line border-b px-4 py-3">
            <p className="text-ink truncate text-sm font-medium">{user.fullName}</p>
            <p className="text-muted truncate text-xs">{user.email}</p>
            <p className="bg-accent-soft text-accent mt-2 inline-block rounded-sm px-1.5 py-0.5 text-[11px] font-medium">
              {ROLE_LABELS[user.role]}
            </p>
          </div>

          <Link
            to="/settings"
            role="menuitem"
            onClick={() => {
              setOpen(false);
            }}
            className="text-muted hover:bg-surface hover:text-ink flex items-center gap-2.5 px-4 py-2.5 text-sm transition-colors"
          >
            <Settings className="size-4" aria-hidden="true" />
            Settings
          </Link>

          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              void logout();
            }}
            className="text-muted hover:bg-surface hover:text-danger flex w-full cursor-pointer items-center gap-2.5 px-4 py-2.5 text-sm transition-colors"
          >
            <LogOut className="size-4" aria-hidden="true" />
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
