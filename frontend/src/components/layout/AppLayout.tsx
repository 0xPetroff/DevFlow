import { Menu } from 'lucide-react';
import { useState } from 'react';
import { Outlet } from 'react-router';

import { Sidebar } from '@/components/layout/Sidebar';
import { UserMenu } from '@/components/layout/UserMenu';
import { Logo } from '@/components/ui/Logo';
import { ThemeToggle } from '@/components/ui/ThemeToggle';

export function AppLayout() {
  // The drawer closes from the links and the overlay themselves rather than from a route effect,
  // which would be a second render after every navigation for the sake of one mobile case.
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="bg-canvas min-h-screen">
      <Sidebar
        open={sidebarOpen}
        onClose={() => {
          setSidebarOpen(false);
        }}
      />

      <div className="lg:pl-60">
        <header className="border-line bg-canvas/90 sticky top-0 z-20 flex h-14 items-center gap-3 border-b px-4 backdrop-blur-sm sm:px-6">
          <button
            type="button"
            onClick={() => {
              setSidebarOpen(true);
            }}
            aria-label="Open navigation"
            className="text-muted hover:bg-elevated hover:text-ink inline-flex size-9 cursor-pointer items-center justify-center rounded-md lg:hidden"
          >
            <Menu className="size-4" aria-hidden="true" />
          </button>

          <Logo className="lg:hidden" showWordmark={false} />

          <div className="flex-1" />
          <ThemeToggle />
          <UserMenu />
        </header>

        <main className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 sm:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
