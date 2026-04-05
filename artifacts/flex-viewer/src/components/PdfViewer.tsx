import { useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';

interface PdfViewerProps {
  pdfData: Uint8Array;
  docName: string;
}

export function PdfViewer({ pdfData, docName }: PdfViewerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [pdfjsLib, setPdfjsLib] = useState<unknown>(null);
  const [pdf, setPdf] = useState<unknown>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [scale, setScale] = useState(1.2);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const renderTaskRef = useRef<unknown>(null);

  useEffect(() => {
    import('pdfjs-dist').then((lib) => {
      (lib as { GlobalWorkerOptions: { workerSrc: string } }).GlobalWorkerOptions.workerSrc =
        new URL('pdfjs-dist/build/pdf.worker.mjs', import.meta.url).toString();
      setPdfjsLib(lib);
    });
  }, []);

  useEffect(() => {
    if (!pdfjsLib || !pdfData) return;
    setLoading(true);
    setError(null);
    setPdf(null);
    setCurrentPage(1);

    const lib = pdfjsLib as {
      getDocument: (opts: { data: Uint8Array }) => { promise: Promise<unknown> };
    };

    lib.getDocument({ data: pdfData.slice() }).promise
      .then((doc: unknown) => {
        const pdfDoc = doc as { numPages: number };
        setPdf(doc);
        setTotalPages(pdfDoc.numPages);
        setLoading(false);
      })
      .catch((err: unknown) => {
        setError(`Failed to load PDF: ${(err as Error).message}`);
        setLoading(false);
      });
  }, [pdfjsLib, pdfData]);

  useEffect(() => {
    if (!pdf || !canvasRef.current) return;

    const pdfDoc = pdf as { getPage: (n: number) => Promise<unknown> };
    let cancelled = false;

    if (renderTaskRef.current) {
      (renderTaskRef.current as { cancel: () => void }).cancel();
    }

    pdfDoc.getPage(currentPage).then((page: unknown) => {
      if (cancelled) return;
      const canvas = canvasRef.current!;
      const ctx = canvas.getContext('2d')!;

      const pdfPage = page as {
        getViewport: (opts: { scale: number }) => { width: number; height: number };
        render: (opts: { canvasContext: CanvasRenderingContext2D; viewport: unknown }) => { promise: Promise<void>; cancel: () => void };
      };

      const viewport = pdfPage.getViewport({ scale });
      const devicePixelRatio = window.devicePixelRatio || 1;

      canvas.width = viewport.width * devicePixelRatio;
      canvas.height = viewport.height * devicePixelRatio;
      canvas.style.width = `${viewport.width}px`;
      canvas.style.height = `${viewport.height}px`;

      ctx.scale(devicePixelRatio, devicePixelRatio);

      const renderContext = { canvasContext: ctx, viewport };
      const task = pdfPage.render(renderContext);
      renderTaskRef.current = task;
      task.promise.catch(() => {});
    });

    return () => { cancelled = true; };
  }, [pdf, currentPage, scale]);

  const blobUrl = useRef<string | null>(null);

  const downloadPdf = () => {
    if (blobUrl.current) URL.revokeObjectURL(blobUrl.current);
    const blob = new Blob([pdfData], { type: 'application/pdf' });
    blobUrl.current = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = blobUrl.current;
    a.download = `${docName}.pdf`;
    a.click();
  };

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-4 py-2 border-b border-border bg-card shrink-0">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage <= 1}
            className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-sm font-medium min-w-[80px] text-center">
            {loading ? '...' : `${currentPage} / ${totalPages}`}
          </span>
          <button
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage >= totalPages}
            className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={() => setScale((s) => Math.max(0.5, s - 0.2))} className="p-1.5 rounded hover:bg-muted transition-colors" title="Zoom out">
            <ZoomOut className="w-4 h-4" />
          </button>
          <span className="text-xs text-muted-foreground w-12 text-center">{Math.round(scale * 100)}%</span>
          <button onClick={() => setScale((s) => Math.min(3, s + 0.2))} className="p-1.5 rounded hover:bg-muted transition-colors" title="Zoom in">
            <ZoomIn className="w-4 h-4" />
          </button>
          <button onClick={() => setScale(1.2)} className="p-1.5 rounded hover:bg-muted transition-colors" title="Reset zoom">
            <RotateCcw className="w-4 h-4" />
          </button>
          <div className="w-px h-4 bg-border mx-1" />
          <button
            onClick={downloadPdf}
            className="px-3 py-1.5 text-xs bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors font-medium"
          >
            Download PDF
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto bg-muted/30 flex items-start justify-center p-6">
        {loading && (
          <div className="flex flex-col items-center gap-3 mt-16 text-muted-foreground">
            <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
            <p className="text-sm">Loading PDF...</p>
          </div>
        )}
        {error && (
          <div className="mt-16 p-4 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-sm text-center max-w-sm">
            {error}
          </div>
        )}
        {!loading && !error && (
          <canvas
            ref={canvasRef}
            className="rounded shadow-lg"
            style={{ display: 'block' }}
          />
        )}
      </div>
    </div>
  );
}
