import { useQuery } from '@tanstack/react-query';
import { FolderKanban, Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';

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
import { useDebounced } from '@/lib/useDebounced';
import { formatRelative } from '@/lib/format';
import { PROJECT_STATUS_LABELS } from '@/lib/labels';
import { queryKeys } from '@/lib/queryKeys';
import { ProjectFormDialog } from '@/projects/ProjectFormDialog';
import { projectsApi } from '@/projects/projectsApi';
import type { ProjectResponse, ProjectStatus } from '@/types/api';

export function ProjectsPage() {
  const { hasRole } = useAuth();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ProjectStatus | ''>('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);

  const debouncedSearch = useDebounced(search);
  const params = {
    q: debouncedSearch || undefined,
    status: status || undefined,
    page,
    size: 12,
  };

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectList(params),
    queryFn: () => projectsApi.list(params),
    placeholderData: (previous) => previous,
  });

  return (
    <>
      <PageHeader
        title="Projects"
        description="Every project you own or belong to."
        actions={
          hasRole('DEVELOPER') && (
            <Button
              onClick={() => {
                setCreating(true);
              }}
            >
              <Plus className="size-4" aria-hidden="true" />
              New project
            </Button>
          )
        }
      />

      <div className="mb-5 flex flex-wrap gap-3">
        <div className="relative min-w-56 flex-1">
          <Search
            className="text-faint pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            aria-hidden="true"
          />
          <Input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search by name or key"
            aria-label="Search projects"
            className="pl-9"
          />
        </div>
        <Select
          value={status}
          aria-label="Filter by status"
          className="w-44"
          onChange={(event) => {
            setStatus(event.target.value as ProjectStatus | '');
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {Object.entries(PROJECT_STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </div>

      {error ? (
        <Alert variant="danger" title="Could not load projects">
          {errorMessage(error)}
        </Alert>
      ) : isPending ? (
        <div className="text-muted flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects here"
          description={
            debouncedSearch || status
              ? 'Nothing matches those filters.'
              : 'Projects you create or are added to will appear here.'
          }
        />
      ) : (
        <div className="flex flex-col gap-5">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {data.content.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
          <Pagination page={data} onChange={setPage} noun="projects" />
        </div>
      )}

      <ProjectFormDialog
        open={creating}
        onClose={() => {
          setCreating(false);
        }}
      />
    </>
  );
}

function ProjectCard({ project }: { project: ProjectResponse }) {
  return (
    <Link
      to={`/projects/${project.id}`}
      className="border-line bg-surface shadow-card hover:border-line-strong flex flex-col gap-3 rounded-lg border p-4 transition-colors"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-ink truncate text-sm font-semibold">{project.name}</p>
          <p className="text-faint mt-0.5 font-mono text-xs">{project.projectKey}</p>
        </div>
        {project.status === 'ARCHIVED' && <Badge tone="warning">Archived</Badge>}
      </div>

      <p className="text-muted line-clamp-2 min-h-10 text-sm">
        {project.description ?? 'No description.'}
      </p>

      <div className="border-line flex items-center justify-between gap-3 border-t pt-3">
        <div className="flex items-center gap-2">
          <Avatar name={project.owner.fullName} color={project.owner.avatarColor} size="sm" />
          <span className="text-muted truncate text-xs">{project.owner.fullName}</span>
        </div>
        <span className="text-faint text-xs whitespace-nowrap">
          {project.memberCount} {project.memberCount === 1 ? 'member' : 'members'} ·{' '}
          {formatRelative(project.updatedAt)}
        </span>
      </div>
    </Link>
  );
}
