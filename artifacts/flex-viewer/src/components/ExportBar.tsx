import { useState } from 'react';
import { Download, Package, Image, FileText, X, Loader2, CheckCircle, FolderOpen } from 'lucide-react';
import type { FlexDocument } from '@/lib/flexcil-parser';
import { exportAsZip, saveToChosenFolder, supportsFolderPicker, type ExportFormat } from '@/lib/zip-export';

interface ExportBarProps {
  selected: Map<string, { doc: FlexDocument; folderName: string }>;
  onClearSelection: () => void;
  backupName: string;
}

type Status = 'idle' | 'exporting' | 'done' | 'error';

export function ExportBar({ selected, onClearSelection, backupName }: ExportBarProps) {
  const [status, setStatus] = useState<Status>('idle');
  const [statusMsg, setStatusMsg] = useState('');

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
  const canPickFolder = supportsFolderPicker();

  async function doZip(format: ExportFormat) {
    setStatus('exporting');
    setStatusMsg('');
    try {
      await exportAsZip(items, format, backupName + '_export');
      setStatus('done');
      setStatusMsg('Downloaded!');
      setTimeout(() => setStatus('idle'), 3000);
    } catch (err) {
      setStatus('error');
      setStatusMsg((err as Error).message);
      setTimeout(() => setStatus('idle'), 4000);
    }
  }

  async function doFolderExport(format: ExportFormat) {
    setStatus('exporting');
    setStatusMsg('');
    try {
      const saved = await saveToChosenFolder(items, format);
      setStatus('done');
      setStatusMsg(`Saved ${saved} file${saved !== 1 ? 's' : ''} to chosen folder`);
      setTimeout(() => setStatus('idle'), 4000);
    } catch (err) {
      const msg = (err as Error).message;
      if (msg === 'The user aborted a request.') {
        setStatus('idle');
      } else {
        setStatus('error');
        setStatusMsg(msg);
        setTimeout(() => setStatus('idle'), 4000);
      }
    }
  }

  const busy = status === 'exporting';

  return (
    <div className="fixed bottom-0 left-72 right-0 z-50 bg-primary text-primary-foreground shadow-2xl border-t border-primary-border">
      <div className="flex flex-wrap items-center gap-2 px-4 py-2.5">
        {/* Count + status */}
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <Package className="w-4 h-4 shrink-0" />
          <span className="text-sm font-semibold whitespace-nowrap">
            {count} doc{count !== 1 ? 's' : ''} selected
          </span>
          {status === 'done' && (
            <span className="flex items-center gap-1 text-xs bg-white/20 px-2 py-0.5 rounded-full whitespace-nowrap">
              <CheckCircle className="w-3 h-3" /> {statusMsg}
            </span>
          )}
          {status === 'error' && (
            <span className="text-xs bg-destructive/40 px-2 py-0.5 rounded-full truncate max-w-xs">
              {statusMsg}
            </span>
          )}
        </div>

        {/* Action buttons */}
        <div className="flex flex-wrap items-center gap-1.5">

          {/* Individual type ZIP downloads */}
          {hasPdfs && (
            <button onClick={() => doZip('pdfs')} disabled={busy}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs bg-white/15 hover:bg-white/25 rounded-lg font-medium transition-colors disabled:opacity-50">
              <FileText className="w-3.5 h-3.5" /> PDFs ZIP
            </button>
          )}
          {hasThumbs && (
            <button onClick={() => doZip('thumbnails')} disabled={busy}
              className="flex items-center gap-1 px-2.5 py-1.5 text-xs bg-white/15 hover:bg-white/25 rounded-lg font-medium transition-colors disabled:opacity-50">
              <Image className="w-3.5 h-3.5" /> Previews ZIP
            </button>
          )}

          {/* Main ZIP button */}
          <button onClick={() => doZip('all')} disabled={busy}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-white/20 hover:bg-white/30 rounded-lg font-medium transition-colors disabled:opacity-50">
            {busy ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
            {busy ? 'Packing…' : 'All as ZIP'}
          </button>

          {/* Folder picker — shown when supported (desktop + modern Android WebView) */}
          {canPickFolder && (
            <>
              <div className="w-px h-5 bg-white/20 mx-0.5" />
              <button onClick={() => doFolderExport('all')} disabled={busy}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-white text-primary hover:bg-white/90 rounded-lg font-semibold transition-colors disabled:opacity-50 whitespace-nowrap">
                <FolderOpen className="w-3.5 h-3.5" />
                Save to Folder
              </button>
            </>
          )}

          {/* Clear */}
          <button onClick={onClearSelection} disabled={busy}
            className="p-1.5 hover:bg-white/20 rounded-lg transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
