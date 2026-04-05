import { useState } from 'react';
import { Download, Package, Image, FileText, X, Loader2, CheckCircle } from 'lucide-react';
import type { FlexDocument } from '@/lib/flexcil-parser';
import { exportAsZip, type ExportFormat } from '@/lib/zip-export';

interface ExportBarProps {
  selected: Map<string, { doc: FlexDocument; folderName: string }>;
  onClearSelection: () => void;
  backupName: string;
}

export function ExportBar({ selected, onClearSelection, backupName }: ExportBarProps) {
  const [exporting, setExporting] = useState(false);
  const [done, setDone] = useState(false);

  if (selected.size === 0) return null;

  const count = selected.size;
  const items = Array.from(selected.values()).map((s) => ({
    name: s.doc.info?.name ?? s.doc.name,
    pdfData: s.doc.pdfData,
    thumbnail: s.doc.thumbnail,
    folderName: s.folderName,
  }));

  const hasPdfs = items.some((i) => i.pdfData);
  const hasThumbs = items.some((i) => i.thumbnail);

  async function doExport(format: ExportFormat) {
    setExporting(true);
    setDone(false);
    try {
      await exportAsZip(items, format, backupName + '_export');
      setDone(true);
      setTimeout(() => setDone(false), 3000);
    } catch (err) {
      alert(`Export failed: ${(err as Error).message}`);
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="fixed bottom-0 left-72 right-0 z-50 bg-primary text-primary-foreground shadow-xl border-t border-primary-border">
      <div className="flex items-center gap-3 px-6 py-3">
        <div className="flex items-center gap-2 flex-1">
          <Package className="w-4 h-4 shrink-0" />
          <span className="text-sm font-semibold">
            {count} document{count !== 1 ? 's' : ''} selected
          </span>
          {done && (
            <span className="flex items-center gap-1 text-xs bg-white/20 px-2 py-1 rounded-full">
              <CheckCircle className="w-3 h-3" /> Downloaded!
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {hasPdfs && (
            <button
              onClick={() => doExport('pdfs')}
              disabled={exporting}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-white/20 hover:bg-white/30 rounded-lg font-medium transition-colors disabled:opacity-50"
            >
              <FileText className="w-3.5 h-3.5" />
              Export PDFs
            </button>
          )}
          {hasThumbs && (
            <button
              onClick={() => doExport('thumbnails')}
              disabled={exporting}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-white/20 hover:bg-white/30 rounded-lg font-medium transition-colors disabled:opacity-50"
            >
              <Image className="w-3.5 h-3.5" />
              Export Previews
            </button>
          )}
          <button
            onClick={() => doExport('all')}
            disabled={exporting}
            className="flex items-center gap-1.5 px-4 py-1.5 text-xs bg-white text-primary hover:bg-white/90 rounded-lg font-semibold transition-colors disabled:opacity-50"
          >
            {exporting ? (
              <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Packing ZIP...</>
            ) : (
              <><Download className="w-3.5 h-3.5" /> Export All as ZIP</>
            )}
          </button>
          <button
            onClick={onClearSelection}
            className="p-1.5 hover:bg-white/20 rounded-lg transition-colors"
            title="Clear selection"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
