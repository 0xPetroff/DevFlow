import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { PageHeader } from '@/components/ui/PageHeader';
import { queryKeys } from '@/lib/queryKeys';
import { auditApi, type AuditListParams } from '@/admin/adminApi';
import { AuditTable } from '@/admin/AuditTable';

export function AuditPage() {
  const [params, setParams] = useState<AuditListParams>({ page: 0, size: 25 });

  const { data, isPending, error } = useQuery({
    queryKey: queryKeys.auditLogs(params),
    queryFn: () => auditApi.list(params),
    placeholderData: (previous) => previous,
  });

  return (
    <>
      <PageHeader
        title="Audit log"
        description="Every recorded action across the installation, including the ones no project owns."
      />

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
