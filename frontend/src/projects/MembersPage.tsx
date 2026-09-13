import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';

import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Select } from '@/components/ui/Select';
import { errorMessage } from '@/lib/apiError';
import { formatDate } from '@/lib/format';
import { queryKeys } from '@/lib/queryKeys';
import { ROLE_LABELS } from '@/lib/roles';
import { usersApi } from '@/admin/adminApi';
import { useProject } from '@/projects/ProjectContext';
import { projectsApi } from '@/projects/projectsApi';
import { useToast } from '@/toast/useToast';
import { ROLES, type ProjectMemberResponse, type Role } from '@/types/api';

export function MembersPage() {
  const { project, members, admin } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<ProjectMemberResponse | null>(null);

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.projectMembers(project.id) });
    await queryClient.invalidateQueries({ queryKey: queryKeys.projects });
  };

  const changeRole = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: Role }) =>
      projectsApi.changeMemberRole(project.id, userId, { role }),
    onSuccess: async () => {
      toast.success('Role updated');
      await refresh();
    },
    onError: (error) => {
      toast.failure(error, 'Could not change that role');
    },
  });

  const remove = useMutation({
    mutationFn: (member: ProjectMemberResponse) =>
      projectsApi.removeMember(project.id, member.user.id),
    onSuccess: async () => {
      toast.success('Member removed');
      setRemoving(null);
      await refresh();
    },
    onError: (error) => {
      toast.failure(error, 'Could not remove that member');
    },
  });

  return (
    <>
      <Card>
        <CardHeader
          title="Members"
          description="A project role governs access here on its own, whatever the account-wide role is."
          actions={
            admin && (
              <Button
                size="sm"
                onClick={() => {
                  setAdding(true);
                }}
              >
                <UserPlus className="size-4" aria-hidden="true" />
                Add member
              </Button>
            )
          }
        />
        <CardBody>
          <ul className="divide-line divide-y">
            {members.map((member) => (
              <li key={member.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0">
                <Avatar name={member.user.fullName} color={member.user.avatarColor} />
                <div className="min-w-40 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-ink text-sm font-medium">{member.user.fullName}</span>
                    {member.owner && <Badge tone="accent">Owner</Badge>}
                  </div>
                  <p className="text-faint font-mono text-xs">{member.user.username}</p>
                </div>

                <span className="text-faint hidden text-xs sm:inline">
                  Added {formatDate(member.createdAt)}
                </span>

                {admin && !member.owner ? (
                  <Select
                    value={member.role}
                    aria-label={`Role for ${member.user.fullName}`}
                    className="w-36"
                    disabled={changeRole.isPending}
                    onChange={(event) => {
                      changeRole.mutate({
                        userId: member.user.id,
                        role: event.target.value as Role,
                      });
                    }}
                  >
                    {ROLES.map((role) => (
                      <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                      </option>
                    ))}
                  </Select>
                ) : (
                  <Badge>{ROLE_LABELS[member.role]}</Badge>
                )}

                {admin && !member.owner && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      setRemoving(member);
                    }}
                  >
                    Remove
                  </Button>
                )}
              </li>
            ))}
          </ul>

          {/* The owner is kept in this list as an ADMIN, so a listing and a permission check
              never disagree. Transferring ownership is how an owner changes. */}
          <p className="text-faint mt-4 text-xs">
            The owner cannot be removed or demoted. Transfer ownership from the project settings
            instead.
          </p>
        </CardBody>
      </Card>

      <AddMemberDialog
        open={adding}
        onClose={() => {
          setAdding(false);
        }}
        onAdded={refresh}
      />

      <ConfirmDialog
        open={removing !== null}
        title="Remove this member"
        confirmLabel="Remove"
        loading={remove.isPending}
        onCancel={() => {
          setRemoving(null);
        }}
        onConfirm={() => {
          if (removing) {
            remove.mutate(removing);
          }
        }}
      >
        {removing?.user.fullName} loses access to this project. Anything they created stays.
      </ConfirmDialog>
    </>
  );
}

interface AddMemberDialogProps {
  open: boolean;
  onClose: () => void;
  onAdded: () => Promise<void>;
}

function AddMemberDialog({ open, onClose, onAdded }: AddMemberDialogProps) {
  const { project, members } = useProject();
  const toast = useToast();
  const [userId, setUserId] = useState('');
  const [role, setRole] = useState<Role>('DEVELOPER');
  const [formError, setFormError] = useState<string | null>(null);

  const { data } = useQuery({
    queryKey: queryKeys.users({ size: 200 }),
    queryFn: () => usersApi.list({ size: 200, active: true }),
    enabled: open,
  });

  const memberIds = new Set(members.map((member) => member.user.id));
  const candidates = (data?.content ?? []).filter((user) => !memberIds.has(user.id));

  const add = useMutation({
    mutationFn: () => projectsApi.addMember(project.id, { userId, role }),
    onSuccess: async () => {
      toast.success('Member added');
      setUserId('');
      onClose();
      await onAdded();
    },
    onError: (error) => {
      setFormError(errorMessage(error));
    },
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a member"
      description="Only active accounts that are not already members are listed."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={add.isPending}>
            Cancel
          </Button>
          <Button
            loading={add.isPending}
            disabled={userId === ''}
            onClick={() => {
              setFormError(null);
              add.mutate();
            }}
          >
            Add member
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {formError && <Alert variant="danger">{formError}</Alert>}

        <Field label="Account">
          {(control) => (
            <Select
              {...control}
              value={userId}
              onChange={(event) => {
                setUserId(event.target.value);
              }}
            >
              <option value="">Choose an account</option>
              {candidates.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.fullName} ({user.username})
                </option>
              ))}
            </Select>
          )}
        </Field>

        <Field label="Project role" hint="Developers write here; viewers only read.">
          {(control) => (
            <Select
              {...control}
              value={role}
              onChange={(event) => {
                setRole(event.target.value as Role);
              }}
            >
              {ROLES.map((value) => (
                <option key={value} value={value}>
                  {ROLE_LABELS[value]}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>
    </Modal>
  );
}
