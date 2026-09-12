import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { auditApi, type AuditListParams } from '@/admin/adminApi';
import { AuditTable } from '@/admin/AuditTable';
import { queryKeys } from '@/lib/queryKeys';
import { useProject } from '@/projects/ProjectContext';

export function ProjectActivityPage() {
  const { project } = useProject();
  const [params, setParams] = useState<AuditListParams>({ page: 0, size: 25 });

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.projectAudit(project.id, params),
    queryFn: () => auditApi.forProject(project.id, params),
    placeholderData: (previous) => previous,
  });

  return (
    <>
      <div className="mb-4">
        <h2 className="text-ink text-base font-semibold">Activity</h2>
        <p className="text-muted mt-1 text-sm">
          Every recorded change to this project, including the deployments its pipelines report.
        </p>
      </div>

      <AuditTable
        data={data}
        isPending={isPending}
        error={error}
        params={params}
        onParamsChange={setParams}
      />
    </>
  );
}
