import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search, Users } from 'lucide-react';
import { useState } from 'react';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/EmptyState';
import { Input } from '@/components/ui/Input';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pagination } from '@/components/ui/Pagination';
import { Select } from '@/components/ui/Select';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatRelative } from '@/lib/format';
import { queryKeys } from '@/lib/queryKeys';
import { ROLE_LABELS } from '@/lib/roles';
import { useDebounced } from '@/lib/useDebounced';
import { usersApi, type UserListParams } from '@/admin/adminApi';
import { useToast } from '@/toast/useToast';
import { ROLES, type Role, type UserResponse } from '@/types/api';

export function UsersPage() {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [search, setSearch] = useState('');
  const [role, setRole] = useState<Role | ''>('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebounced(search);
  const params: UserListParams = {
    q: debouncedSearch || undefined,
    role: role || undefined,
    active: active === '' ? undefined : active === 'true',
    page,
    size: 20,
  };

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.users(params),
    queryFn: () => usersApi.list(params),
    placeholderData: (previous) => previous,
  });

  const update = useMutation({
    mutationFn: ({ user, changes }: { user: UserResponse; changes: Partial<UserResponse> }) =>
      usersApi.update(user.id, {
        fullName: changes.fullName ?? user.fullName,
        role: changes.role ?? user.role,
        active: changes.active ?? user.active,
      }),
    onSuccess: async (updated) => {
      toast.success(`Updated ${updated.username}`);
      await queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (updateError) => {
      toast.failure(updateError, 'Could not update that account');
    },
  });

  return (
    <>
      <PageHeader
        title="Users"
        description="The account directory. An account-wide admin can reach every project."
      />

      <div className="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div className="relative xl:col-span-2">
          <Search
            className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            aria-hidden="true"
          />
          <Input
            value={search}
            aria-label="Search accounts"
            placeholder="Search name, username or email"
            className="pl-9"
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
          />
        </div>

        <Select
          value={role}
          aria-label="Filter by role"
          onChange={(event) => {
            setRole(event.target.value as Role | '');
            setPage(0);
          }}
        >
          <option value="">Every role</option>
          {ROLES.map((value) => (
            <option key={value} value={value}>
              {ROLE_LABELS[value]}
            </option>
          ))}
        </Select>

        <Select
          value={active}
          aria-label="Filter by state"
          onChange={(event) => {
            setActive(event.target.value);
            setPage(0);
          }}
        >
          <option value="">Active and disabled</option>
          <option value="true">Active only</option>
          <option value="false">Disabled only</option>
        </Select>
      </div>

      {error ? (
        <Alert variant="danger" title="Could not load accounts">
          {errorMessage(error)}
        </Alert>
      ) : isPending ? (
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState icon={Users} title="No accounts match" description="Try different filters." />
      ) : (
        <div className="flex flex-col gap-4">
          <ul
            aria-label="Accounts"
            className="border-line bg-surface divide-line divide-y overflow-hidden rounded-lg border"
          >
            {data.content.map((user) => {
              const isSelf = user.id === currentUser?.id;
              return (
                <li key={user.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <Avatar name={user.fullName} color={user.avatarColor} />
                  <div className="min-w-40 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-ink text-sm font-medium">{user.fullName}</span>
                      {isSelf && <Badge tone="accent">You</Badge>}
                      {!user.active && <Badge tone="danger">Disabled</Badge>}
                    </div>
                    <p className="text-faint text-xs">
                      <span className="font-mono">{user.username}</span> · {user.email}
                    </p>
                  </div>

                  <span className="text-faint hidden w-36 text-xs lg:inline">
                    Last seen {formatRelative(user.lastLoginAt)}
                  </span>

                  {/* An admin cannot change their own role or disable themselves; the API refuses
                      it too, so the last administrator cannot lock everyone out by accident. */}
                  {isSelf ? (
                    <Badge>{ROLE_LABELS[user.role]}</Badge>
                  ) : (
                    <>
                      <Select
                        value={user.role}
                        aria-label={`Role for ${user.username}`}
                        className="w-36"
                        disabled={update.isPending}
                        onChange={(event) => {
                          update.mutate({
                            user,
                            changes: { role: event.target.value as Role },
                          });
                        }}
                      >
                        {ROLES.map((value) => (
                          <option key={value} value={value}>
                            {ROLE_LABELS[value]}
                          </option>
                        ))}
                      </Select>

                      <Button
                        size="sm"
                        variant="secondary"
                        loading={update.isPending}
                        onClick={() => {
                          update.mutate({ user, changes: { active: !user.active } });
                        }}
                      >
                        {user.active ? 'Disable' : 'Enable'}
                      </Button>
                    </>
                  )}
                </li>
              );
            })}
          </ul>
          <Pagination page={data} onChange={setPage} noun="accounts" />
        </div>
      )}
    </>
  );
}
