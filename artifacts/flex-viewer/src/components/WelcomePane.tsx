import { FileText, Folder, Info } from 'lucide-react';
import type { FlexBackup } from '@/lib/flexcil-parser';
import { formatDate, formatFileSize } from '@/lib/flexcil-parser';

interface WelcomePaneProps {
  backup: FlexBackup;
}

export function WelcomePane({ backup }: WelcomePaneProps) {
  const totalSize = backup.folders.reduce(
    (sum, f) => sum + f.documents.reduce((s, d) => s + (d.flxData?.length ?? 0), 0),
    0
  );

  return (
    <div className="flex flex-col h-full overflow-auto p-8">
      <div className="max-w-2xl mx-auto w-full">
        <div className="mb-8">
          <h1 className="text-2xl font-bold mb-2">Flexcil Backup</h1>
          <p className="text-muted-foreground text-sm">
            Select a document from the sidebar to view its content.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-4 mb-8">
          <StatCard icon={<Folder className="w-5 h-5 text-primary" />} value={backup.folders.length} label="Folders" />
          <StatCard icon={<FileText className="w-5 h-5 text-primary" />} value={backup.totalDocuments} label="Documents" />
          <StatCard icon={<Info className="w-5 h-5 text-primary" />} value={formatFileSize(totalSize)} label="Total Size" />
        </div>

        <div className="rounded-xl border border-border bg-card p-5 mb-6">
          <h2 className="font-semibold mb-3 text-sm">Backup Details</h2>
          <div className="space-y-2">
            <DetailRow label="App" value={backup.info.appName} />
            <DetailRow label="Backup Date" value={backup.info.backupDate} />
            <DetailRow label="App Version" value={backup.info.appVersion} />
            <DetailRow label="Format Version" value={backup.info.version} />
          </div>
        </div>

        <div className="rounded-xl border border-border bg-card p-5">
          <h2 className="font-semibold mb-3 text-sm">Folder Contents</h2>
          <div className="space-y-3">
            {backup.folders.map((folder) => (
              <div key={folder.name} className="p-3 rounded-lg bg-muted/40 border border-border">
                <div className="flex items-center gap-2 mb-2">
                  <Folder className="w-4 h-4 text-primary" />
                  <span className="font-medium text-sm">{folder.name}</span>
                  <span className="text-xs text-muted-foreground ml-auto">{folder.documents.length} docs</span>
                </div>
                <div className="space-y-1">
                  {folder.documents.slice(0, 5).map((doc) => (
                    <div key={doc.name} className="flex items-center gap-2 text-xs text-muted-foreground pl-6">
                      <FileText className="w-3 h-3 shrink-0" />
                      <span className="truncate flex-1">{doc.info?.name ?? doc.name}</span>
                      {doc.info?.modifiedDate && (
                        <span className="shrink-0">{formatDate(doc.info.modifiedDate)}</span>
                      )}
                    </div>
                  ))}
                  {folder.documents.length > 5 && (
                    <p className="text-xs text-muted-foreground pl-6">
                      +{folder.documents.length - 5} more documents
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, value, label }: { icon: React.ReactNode; value: string | number; label: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 flex flex-col items-center gap-2 text-center">
      {icon}
      <span className="text-xl font-bold">{value}</span>
      <span className="text-xs text-muted-foreground">{label}</span>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3 py-1 border-b border-border last:border-0">
      <span className="text-xs text-muted-foreground w-28 shrink-0">{label}</span>
      <span className="text-sm">{value || '—'}</span>
    </div>
  );
}
