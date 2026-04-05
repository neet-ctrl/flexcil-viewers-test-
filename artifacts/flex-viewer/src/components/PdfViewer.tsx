import { useEffect, useRef, useState, useCallback } from 'react';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, RotateCcw, Lock, Eye, EyeOff } from 'lucide-react';

interface PdfViewerProps {
  pdfData: Uint8Array;
  docName: string;
}

type LoadState = 'loading' | 'ready' | 'password-needed' | 'password-wrong' | 'error';

export function PdfViewer({ pdfData, docName }: PdfViewerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [pdfjsLib, setPdfjsLib] = useState<unknown>(null);
  const [pdf, setPdf] = useState<unknown>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [scale, setScale] = useState(1.2);
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [errorMsg, setErrorMsg] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const renderTaskRef = useRef<unknown>(null);
  const passwordInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    import('pdfjs-dist').then((lib) => {
      (lib as { GlobalWorkerOptions: { workerSrc: string } }).GlobalWorkerOptions.workerSrc =
        new URL('pdfjs-dist/build/pdf.worker.mjs', import.meta.url).toString();
      setPdfjsLib(lib);
    });
  }, []);

  const loadPdf = useCallback(
    (pwd: string = '') => {
      if (!pdfjsLib || !pdfData) return;

      setLoadState('loading');
      setErrorMsg('');
      setPdf(null);
      setCurrentPage(1);

      const lib = pdfjsLib as {
        getDocument: (opts: { data: Uint8Array; password?: string }) => { promise: Promise<unknown> };
        PasswordResponses: { NEED_PASSWORD: number; INCORRECT_PASSWORD: number };
      };

      const opts: { data: Uint8Array; password?: string } = { data: pdfData.slice() };
      if (pwd) opts.password = pwd;

      lib.getDocument(opts).promise
        .then((doc: unknown) => {
          const pdfDoc = doc as { numPages: number };
          setPdf(doc);
          setTotalPages(pdfDoc.numPages);
          setLoadState('ready');
          setPassword('');
        })
        .catch((err: unknown) => {
          const e = err as { name?: string; code?: number; message?: string };
          if (e.name === 'PasswordException') {
            if (e.code === 2) {
              setLoadState('password-wrong');
            } else {
              setLoadState('password-needed');
            }
            setTimeout(() => passwordInputRef.current?.focus(), 100);
          } else {
            setLoadState('error');
            setErrorMsg(e.message ?? 'Unknown error');
          }
        });
    },
    [pdfjsLib, pdfData]
  );

  useEffect(() => {
    if (!pdfjsLib || !pdfData) return;
    loadPdf('');
  }, [pdfjsLib, pdfData, loadPdf]);

  useEffect(() => {
    if (!pdf || !canvasRef.current || loadState !== 'ready') return;

    const pdfDoc = pdf as { getPage: (n: number) => Promise<unknown> };
    let cancelled = false;

    if (renderTaskRef.current) {
      (renderTaskRef.current as { cancel: () => void }).cancel();
    }

    pdfDoc.getPage(currentPage).then((page: unknown) => {
      if (cancelled || !canvasRef.current) return;
      const canvas = canvasRef.current;
      const ctx = canvas.getContext('2d')!;

      const pdfPage = page as {
        getViewport: (opts: { scale: number }) => { width: number; height: number };
        render: (opts: { canvasContext: CanvasRenderingContext2D; viewport: unknown }) => { promise: Promise<void>; cancel: () => void };
      };

      const viewport = pdfPage.getViewport({ scale });
      const dpr = window.devicePixelRatio || 1;

      canvas.width = viewport.width * dpr;
      canvas.height = viewport.height * dpr;
      canvas.style.width = `${viewport.width}px`;
      canvas.style.height = `${viewport.height}px`;
      ctx.scale(dpr, dpr);

      const task = pdfPage.render({ canvasContext: ctx, viewport });
      renderTaskRef.current = task;
      task.promise.catch(() => {});
    });

    return () => { cancelled = true; };
  }, [pdf, currentPage, scale, loadState]);

  const downloadPdf = () => {
    const blob = new Blob([pdfData], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${docName}.pdf`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  };

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (password.trim()) loadPdf(password);
  };

  const isPasswordScreen = loadState === 'password-needed' || loadState === 'password-wrong';

  return (
    <div className="flex flex-col h-full">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-4 py-2 border-b border-border bg-card shrink-0">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage <= 1 || loadState !== 'ready'}
            className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-sm font-medium min-w-[80px] text-center">
            {loadState === 'ready' ? `${currentPage} / ${totalPages}` : '—'}
          </span>
          <button
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage >= totalPages || loadState !== 'ready'}
            className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={() => setScale((s) => Math.max(0.5, s - 0.2))} disabled={loadState !== 'ready'} className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors" title="Zoom out">
            <ZoomOut className="w-4 h-4" />
          </button>
          <span className="text-xs text-muted-foreground w-12 text-center">{Math.round(scale * 100)}%</span>
          <button onClick={() => setScale((s) => Math.min(3, s + 0.2))} disabled={loadState !== 'ready'} className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors" title="Zoom in">
            <ZoomIn className="w-4 h-4" />
          </button>
          <button onClick={() => setScale(1.2)} disabled={loadState !== 'ready'} className="p-1.5 rounded hover:bg-muted disabled:opacity-30 transition-colors" title="Reset zoom">
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

      {/* Content area */}
      <div className="flex-1 overflow-auto bg-muted/30 flex items-center justify-center p-6">

        {/* Loading spinner */}
        {loadState === 'loading' && (
          <div className="flex flex-col items-center gap-3 text-muted-foreground">
            <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
            <p className="text-sm">Loading PDF...</p>
          </div>
        )}

        {/* Password screen */}
        {isPasswordScreen && (
          <div className="w-full max-w-sm">
            <div className="bg-card border border-border rounded-2xl p-8 shadow-lg text-center">
              <div className="w-16 h-16 rounded-2xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 flex items-center justify-center mx-auto mb-5">
                <Lock className="w-8 h-8 text-amber-500" />
              </div>
              <h3 className="text-lg font-bold mb-1">Password Protected</h3>
              <p className="text-sm text-muted-foreground mb-6">
                {loadState === 'password-wrong'
                  ? 'Incorrect password. Please try again.'
                  : `"${docName}" is locked with a password. Enter it to view the PDF.`}
              </p>
              <form onSubmit={handlePasswordSubmit} className="space-y-4">
                <div className="relative">
                  <input
                    ref={passwordInputRef}
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter PDF password"
                    className={`w-full px-4 py-3 pr-12 rounded-xl border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-background
                      ${loadState === 'password-wrong' ? 'border-destructive focus:ring-destructive' : 'border-border'}
                    `}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {loadState === 'password-wrong' && (
                  <p className="text-xs text-destructive text-left">Wrong password — please try again.</p>
                )}
                <button
                  type="submit"
                  disabled={!password.trim()}
                  className="w-full py-3 bg-primary text-primary-foreground rounded-xl font-semibold text-sm hover:bg-primary/90 transition-colors disabled:opacity-40"
                >
                  Unlock PDF
                </button>
              </form>
              <p className="text-xs text-muted-foreground mt-4">
                You can still Download the encrypted PDF or view its Preview tab.
              </p>
            </div>
          </div>
        )}

        {/* Generic error */}
        {loadState === 'error' && (
          <div className="max-w-sm text-center">
            <div className="p-6 rounded-2xl bg-destructive/10 border border-destructive/20">
              <p className="text-destructive font-semibold mb-2">Could not load PDF</p>
              <p className="text-sm text-muted-foreground">{errorMsg}</p>
              <p className="text-xs text-muted-foreground mt-3">Try switching to the Preview tab to see the cover image.</p>
            </div>
          </div>
        )}

        {/* PDF canvas */}
        {loadState === 'ready' && (
          <canvas ref={canvasRef} className="rounded shadow-lg" style={{ display: 'block' }} />
        )}
      </div>
    </div>
  );
}
