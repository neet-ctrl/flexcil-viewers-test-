import { useState, useCallback } from 'react';
import { Upload, FileArchive } from 'lucide-react';

interface UploadZoneProps {
  onFile: (file: File) => void;
  loading: boolean;
  error: string | null;
}

export function UploadZone({ onFile, loading, error }: UploadZoneProps) {
  const [dragging, setDragging] = useState(false);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragging(false);
      const file = e.dataTransfer.files[0];
      if (file) onFile(file);
    },
    [onFile]
  );

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) onFile(file);
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4">
      <div className="max-w-lg w-full text-center mb-8">
        <div className="flex items-center justify-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
            <FileArchive className="w-6 h-6 text-primary" />
          </div>
          <div className="text-left">
            <h1 className="text-2xl font-bold tracking-tight">Flexcil Viewer</h1>
            <p className="text-sm text-muted-foreground">Universal .flex backup reader</p>
          </div>
        </div>
        <p className="text-muted-foreground text-sm">
          Open your Flexcil backup (.flex file) to browse all your documents, PDFs, and annotations exactly as organized in the app.
        </p>
      </div>

      <label
        className={`max-w-lg w-full border-2 border-dashed rounded-2xl p-12 flex flex-col items-center gap-4 cursor-pointer transition-all
          ${dragging ? 'border-primary bg-primary/5 scale-[1.01]' : 'border-border hover:border-primary/50 hover:bg-primary/3'}
          ${loading ? 'pointer-events-none opacity-60' : ''}
        `}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
      >
        <input
          type="file"
          accept=".flex"
          className="hidden"
          onChange={handleChange}
          disabled={loading}
        />

        {loading ? (
          <>
            <div className="w-12 h-12 rounded-full border-4 border-primary border-t-transparent animate-spin" />
            <div>
              <p className="font-semibold text-lg">Decoding your backup...</p>
              <p className="text-sm text-muted-foreground mt-1">Extracting all files and folders</p>
            </div>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center">
              <Upload className="w-8 h-8 text-primary" />
            </div>
            <div>
              <p className="font-semibold text-lg">Drop your .flex file here</p>
              <p className="text-sm text-muted-foreground mt-1">or click to browse and select</p>
            </div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted px-4 py-2 rounded-full">
              <FileArchive className="w-3 h-3" />
              Supports Flexcil backup files (.flex)
            </div>
          </>
        )}
      </label>

      {error && (
        <div className="max-w-lg w-full mt-4 p-4 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-sm text-center">
          {error}
        </div>
      )}
    </div>
  );
}
