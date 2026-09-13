import { Construction } from 'lucide-react';

import { PageHeader } from '@/components/ui/PageHeader';

interface PlaceholderPageProps {
  title: string;
  description: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <>
      <PageHeader title={title} description={description} />
      <div className="border-line-strong flex flex-col items-center gap-3 rounded-lg border border-dashed px-6 py-16 text-center">
        <Construction className="text-faint size-6" aria-hidden="true" />
        <p className="text-muted text-sm">This view is built in the next stage.</p>
      </div>
    </>
  );
}
