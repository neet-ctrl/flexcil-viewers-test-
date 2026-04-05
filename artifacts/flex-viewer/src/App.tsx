import { useState, useCallback } from 'react';
import { UploadZone } from '@/components/UploadZone';
import { Sidebar } from '@/components/Sidebar';
import { DocumentViewer } from '@/components/DocumentViewer';
import { WelcomePane } from '@/components/WelcomePane';
import type { FlexBackup, FlexDocument } from '@/lib/flexcil-parser';
import { parseFlexFile } from '@/lib/flexcil-parser';

function App() {
  const [backup, setBackup] = useState<FlexBackup | null>(null);
  const [selectedDoc, setSelectedDoc] = useState<FlexDocument | null>(null);
  const [selectedFolder, setSelectedFolder] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFile = useCallback(async (file: File) => {
    if (!file.name.endsWith('.flex') && !file.name.endsWith('.zip')) {
      setError('Please select a Flexcil backup file (.flex)');
      return;
    }
    setLoading(true);
    setError(null);
    setSelectedDoc(null);
    setSelectedFolder('');

    try {
      const result = await parseFlexFile(file);
      setBackup(result);
    } catch (err) {
      setError(`Failed to parse file: ${(err as Error).message}`);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleSelectDoc = useCallback((doc: FlexDocument, folderName: string) => {
    setSelectedDoc(doc);
    setSelectedFolder(folderName);
  }, []);

  const handleReset = useCallback(() => {
    setBackup(null);
    setSelectedDoc(null);
    setSelectedFolder('');
    setError(null);
  }, []);

  if (!backup) {
    return <UploadZone onFile={handleFile} loading={loading} error={error} />;
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar
        backup={backup}
        selectedDoc={selectedDoc}
        onSelectDoc={handleSelectDoc}
        onReset={handleReset}
      />
      <main className="flex-1 overflow-hidden">
        {selectedDoc ? (
          <DocumentViewer doc={selectedDoc} folderName={selectedFolder} />
        ) : (
          <WelcomePane backup={backup} />
        )}
      </main>
    </div>
  );
}

export default App;
