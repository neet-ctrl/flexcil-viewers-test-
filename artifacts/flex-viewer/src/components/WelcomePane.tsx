import { FileText, Folder, Info, Download, Package } from 'lucide-react';
import type { FlexBackup, FlexDocument } from '@/lib/flexcil-parser';
import { formatDate, formatFileSize } from '@/lib/flexcil-parser';
import { exportAsZip } from '@/lib/zip-export';
import { useState } from 'react';

interface WelcomePaneProps {
  backup: FlexBackup;
}

export function WelcomePane({ backup }: WelcomePaneProps) {
  const [exporting, setExporting] = useState(false);

  const totalSize = backup.folders.reduce(
    (sum, f) => sum + f.documents.reduce((s, d) => s + (d.flxData?.length ?? 0), 0),
    0
  );
  const totalPdfs = backup.folders.reduce(
    (sum, f) => sum + f.documents.filter((d) => d.pdfData).length,
    0
  );

  const allDocs = backup.folders.flatMap((f) =>
    f.documents.map((d) => ({
      name: d.info?.name ?? d.name,
      pdfData: d.pdfData,
      thumbnail: d.thumbnail,
      folderName: f.name,
    }))
  );

  async function exportAllPdfs() {
    setExporting(true);
    try {
      await exportAsZip(allDocs, 'pdfs', backup.info.appName || 'flexcil_export');
    } catch (e) {
      alert('Export failed: ' + (e as Error).message);
    } finally {
      setExporting(false);
    }
  }

  async function exportAll() {
    setExporting(true);
    try {
      await exportAsZip(allDocs, 'all', backup.info.appName || 'flexcil_export');
    } catch (e) {
      alert('Export failed: ' + (e as Error).message);
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="flex flex-col h-full overflow-auto p-8">
      <div className="max-w-2xl mx-auto w-full">
        <div className="mb-6">
          <h1 className="text-2xl font-bold mb-1">Backup Opened</h1>
          <p className="text-muted-foreground text-sm">
            Select a document from the sidebar to view it, or export documents below.
          </p>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <StatCard icon={<Folder className="w-5 h-5 text-primary" />} value={backup.folders.length} label="Folders" color="blue" />
          <StatCard icon={<FileText className="w-5 h-5 text-green-600" />} value={backup.totalDocuments} label="Documents" color="green" />
          <StatCard icon={<FileText className="w-5 h-5 text-orange-500" />} value={totalPdfs} label="With PDF" color="orange" />
          <StatCard icon={<Info className="w-5 h-5 text-purple-500" />} value={formatFileSize(totalSize)} label="Total Size" color="purple" />
        </div>

        <div className="rounded-xl border border-border bg-card p-5 mb-4">
          <h2 className="font-semibold mb-3 text-sm">Backup Info</h2>
          <div className="grid grid-cols-2 gap-2">
            <DetailRow label="App" value={backup.info.appName} />
            <DetailRow label="Version" value={backup.info.appVersion} />
            <DetailRow label="Backup Date" value={backup.info.backupDate} />
            <DetailRow label="Format" value={`v${backup.info.version}`} />
          </div>
        </div>

        {totalPdfs > 0 && (
          <div className="rounded-xl border border-border bg-card p-5 mb-6">
            <h2 className="font-semibold mb-3 text-sm">Quick Export</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Export all {totalPdfs} PDFs from this backup at once. Files are organized in folders matching your Flexcil structure.
            </p>
            <div className="flex flex-wrap gap-3">
              <button
                onClick={exportAllPdfs}
                disabled={exporting}
                className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                {exporting ? 'Packing...' : `Export All ${totalPdfs} PDFs`}
              </button>
              <button
                onClick={exportAll}
                disabled={exporting}
                className="flex items-center gap-2 px-4 py-2 bg-secondary text-secondary-foreground rounded-lg text-sm font-medium hover:bg-secondary/80 transition-colors disabled:opacity-50"
              >
                <Package className="w-4 h-4" />
                {exporting ? 'Packing...' : 'Export PDFs + Previews'}
              </button>
            </div>
          </div>
        )}

        <div className="rounded-xl border border-border bg-card p-5">
          <h2 className="font-semibold mb-3 text-sm">Folder Contents</h2>
          <div className="space-y-3">
            {backup.folders.map((folder) => (
              <div key={folder.name} className="p-3 rounded-lg bg-muted/40 border border-border">
                <div className="flex items-center gap-2 mb-2">
                  <Folder className="w-4 h-4 text-primary" />
                  <span className="font-medium text-sm">{folder.name}</span>
                  <span className="text-xs text-muted-foreground ml-auto">
                    {folder.documents.filter((d) => d.pdfData).length}/{folder.documents.length} PDFs
                  </span>
                </div>
                <div className="space-y-1">
                  {folder.documents.slice(0, 6).map((doc: FlexDocument) => (
                    <div key={doc.name} className="flex items-center gap-2 text-xs text-muted-foreground pl-6">
                      <FileText className="w-3 h-3 shrink-0" />
                      <span className="truncate flex-1">{doc.info?.name ?? doc.name}</span>
                      {doc.pdfData && <span className="text-blue-500 shrink-0">PDF</span>}
                      {doc.info?.modifiedDate && (
                        <span className="shrink-0 hidden sm:inline">{formatDate(doc.info.modifiedDate)}</span>
                      )}
                    </div>
                  ))}
                  {folder.documents.length > 6 && (
                    <p className="text-xs text-muted-foreground pl-6">
                      +{folder.documents.length - 6} more...
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

function StatCard({ icon, value, label }: { icon: React.ReactNode; value: string | number; label: string; color: string }) {
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
    <div className="py-1.5 border-b border-border last:border-0">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-medium mt-0.5">{value || '—'}</p>
    </div>
  );
}
