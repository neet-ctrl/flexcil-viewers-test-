import { zip } from 'fflate';

export interface ExportItem {
  name: string;
  pdfData?: Uint8Array;
  thumbnail?: Uint8Array;
  folderName: string;
}

export type ExportFormat = 'pdfs' | 'thumbnails' | 'all';

function sanitize(name: string): string {
  return name.replace(/[/\\?%*:|"<>]/g, '_');
}

export async function exportAsZip(
  items: ExportItem[],
  format: ExportFormat = 'all',
  zipName: string = 'flexcil_export'
): Promise<void> {
  return new Promise((resolve, reject) => {
    const files: Record<string, Uint8Array> = {};

    for (const item of items) {
      const folder = sanitize(item.folderName);
      const docName = sanitize(item.name);
      const includePdf = format === 'pdfs' || format === 'all';
      const includeThumb = format === 'thumbnails' || format === 'all';

      if (includePdf && item.pdfData) {
        files[`${folder}/${docName}.pdf`] = item.pdfData;
      }
      if (includeThumb && item.thumbnail) {
        files[`${folder}/${docName}_thumbnail.jpg`] = item.thumbnail;
      }
    }

    if (Object.keys(files).length === 0) {
      reject(new Error('No files to export in the selected format'));
      return;
    }

    zip(files, { level: 0 }, (err, data) => {
      if (err) { reject(err); return; }
      const blob = new Blob([data], { type: 'application/zip' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${sanitize(zipName)}.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 5000);
      resolve();
    });
  });
}

export async function downloadSingleFile(
  data: Uint8Array,
  filename: string,
  mimeType: string
): Promise<void> {
  const blob = new Blob([data], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}
