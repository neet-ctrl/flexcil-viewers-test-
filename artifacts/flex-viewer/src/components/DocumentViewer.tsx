import { useState, useEffect } from 'react';
import {
  FileText,
  Image,
  Info,
  Download,
  Calendar,
  Clock,
  Layers,
} from 'lucide-react';
import type { FlexDocument } from '@/lib/flexcil-parser';
import { formatDate, formatFileSize } from '@/lib/flexcil-parser';
import { PdfViewer } from './PdfViewer';

interface DocumentViewerProps {
  doc: FlexDocument;
  folderName: string;
}

type Tab = 'pdf' | 'thumbnail' | 'info';

export function DocumentViewer({ doc, folderName }: DocumentViewerProps) {
  const [activeTab, setActiveTab] = useState<Tab>('pdf');
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);

  const displayName = doc.info?.name ?? doc.name;

  useEffect(() => {
    if (doc.thumbnail) {
      const blob = new Blob([doc.thumbnail], { type: 'image/jpeg' });
      const url = URL.createObjectURL(blob);
      setThumbnailUrl(url);
      return () => URL.revokeObjectURL(url);
    } else {
      setThumbnailUrl(null);
    }
  }, [doc.thumbnail]);

  useEffect(() => {
    if (doc.pdfData) setActiveTab('pdf');
    else if (doc.thumbnail) setActiveTab('thumbnail');
    else setActiveTab('info');
  }, [doc]);

  const tabs: { id: Tab; label: string; icon: React.ReactNode; show: boolean }[] = [
    { id: 'pdf', label: 'PDF', icon: <FileText className="w-3.5 h-3.5" />, show: !!doc.pdfData },
    { id: 'thumbnail', label: 'Preview', icon: <Image className="w-3.5 h-3.5" />, show: !!doc.thumbnail },
    { id: 'info', label: 'Info', icon: <Info className="w-3.5 h-3.5" />, show: true },
  ];

  const downloadThumbnail = () => {
    if (!thumbnailUrl) return;
    const a = document.createElement('a');
    a.href = thumbnailUrl;
    a.download = `${displayName}_thumbnail.jpg`;
    a.click();
  };

  return (
    <div className="flex flex-col h-full bg-background">
      <div className="px-6 py-4 border-b border-border bg-card shrink-0">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-xs text-muted-foreground mb-1">
              <Layers className="w-3 h-3" />
              <span>{folderName}</span>
            </div>
            <h2 className="text-lg font-semibold truncate leading-tight">{displayName}</h2>
            {doc.info && (
              <div className="flex flex-wrap gap-3 mt-2">
                {doc.info.createDate && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Calendar className="w-3 h-3" />
                    <span>Created: {formatDate(doc.info.createDate)}</span>
                  </div>
                )}
                {doc.info.modifiedDate && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Clock className="w-3 h-3" />
                    <span>Modified: {formatDate(doc.info.modifiedDate)}</span>
                  </div>
                )}
                {doc.flxData && (
                  <div className="text-xs text-muted-foreground">
                    {formatFileSize(doc.flxData.length)}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="flex gap-1 mt-4">
          {tabs.filter((t) => t.show).map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors
                ${activeTab === tab.id
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-hidden">
        {activeTab === 'pdf' && doc.pdfData && (
          <PdfViewer pdfData={doc.pdfData} docName={displayName} />
        )}

        {activeTab === 'thumbnail' && (
          <div className="h-full flex flex-col items-center justify-center p-8 gap-4 overflow-auto">
            {thumbnailUrl ? (
              <>
                <img
                  src={thumbnailUrl}
                  alt={`Thumbnail for ${displayName}`}
                  className="max-w-full max-h-[70vh] rounded-lg shadow-lg object-contain"
                />
                <button
                  onClick={downloadThumbnail}
                  className="flex items-center gap-2 px-4 py-2 text-sm bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
                >
                  <Download className="w-4 h-4" />
                  Download Thumbnail
                </button>
              </>
            ) : (
              <div className="text-muted-foreground text-sm">No thumbnail available</div>
            )}
          </div>
        )}

        {activeTab === 'info' && (
          <div className="p-6 overflow-auto h-full">
            <div className="max-w-2xl space-y-6">
              <InfoSection title="Document Details">
                <InfoRow label="Name" value={displayName} />
                <InfoRow label="Folder" value={folderName} />
                {doc.info?.key && <InfoRow label="ID" value={doc.info.key} mono />}
                {doc.info?.type !== undefined && (
                  <InfoRow label="Type" value={doc.info.type === 0 ? 'PDF Document' : `Type ${doc.info.type}`} />
                )}
                {doc.info?.createDate && (
                  <InfoRow label="Created" value={formatDate(doc.info.createDate)} />
                )}
                {doc.info?.modifiedDate && (
                  <InfoRow label="Modified" value={formatDate(doc.info.modifiedDate)} />
                )}
                <InfoRow label="File size" value={formatFileSize(doc.flxData?.length ?? 0)} />
                {doc.pdfData && <InfoRow label="PDF size" value={formatFileSize(doc.pdfData.length)} />}
              </InfoSection>

              {doc.pages && doc.pages.length > 0 && (
                <InfoSection title={`Pages (${doc.pages.length})`}>
                  {doc.pages.map((page, i) => (
                    <div key={page.key} className="p-3 rounded-lg bg-muted/50 border border-border">
                      <p className="text-sm font-medium mb-2">Page {i + 1}</p>
                      <div className="grid grid-cols-2 gap-1 text-xs text-muted-foreground">
                        <span>Size: {Math.round(page.frame.width)} × {Math.round(page.frame.height)} pt</span>
                        <span>Rotation: {page.rotate}°</span>
                        {page.attachmentPage && (
                          <span className="col-span-2">Attachment: page {page.attachmentPage.index + 1}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </InfoSection>
              )}

              {doc.drawingsData && doc.drawingsData.length > 0 && (
                <InfoSection title={`Annotations (${doc.drawingsData.length})`}>
                  <p className="text-sm text-muted-foreground">
                    This document has {doc.drawingsData.length} drawing annotation{doc.drawingsData.length !== 1 ? 's' : ''} (handwriting, highlights, etc.).
                  </p>
                </InfoSection>
              )}

              <InfoSection title="Contained Files">
                {doc.pdfData && (
                  <FileRow icon="📄" name="PDF Document" size={doc.pdfData.length} />
                )}
                {doc.thumbnail && (
                  <FileRow icon="🖼️" name="Thumbnail (JPEG)" size={doc.thumbnail.length} />
                )}
                {doc.pages && (
                  <FileRow icon="📋" name="Page Index (JSON)" size={0} />
                )}
                {doc.drawingsData && (
                  <FileRow icon="✏️" name="Drawings / Annotations" size={0} />
                )}
              </InfoSection>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function InfoSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="text-sm font-semibold text-foreground mb-3">{title}</h3>
      <div className="space-y-2">{children}</div>
    </div>
  );
}

function InfoRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex gap-3 py-1.5 border-b border-border last:border-0">
      <span className="text-xs text-muted-foreground w-28 shrink-0 pt-0.5">{label}</span>
      <span className={`text-sm text-foreground break-all ${mono ? 'font-mono text-xs' : ''}`}>{value}</span>
    </div>
  );
}

function FileRow({ icon, name, size }: { icon: string; name: string; size: number }) {
  return (
    <div className="flex items-center gap-3 p-2.5 rounded-lg bg-muted/40 border border-border">
      <span className="text-base">{icon}</span>
      <span className="text-sm flex-1">{name}</span>
      {size > 0 && <span className="text-xs text-muted-foreground">{formatFileSize(size)}</span>}
    </div>
  );
}
