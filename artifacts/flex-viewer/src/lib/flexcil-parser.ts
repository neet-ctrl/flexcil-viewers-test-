import { unzipSync, strFromU8 } from 'fflate';

export interface FlexBackupInfo {
  appName: string;
  backupDate: string;
  appVersion: string;
  version: string;
}

export interface FlxDocInfo {
  name: string;
  createDate: number;
  modifiedDate: number;
  type: number;
  key: string;
}

export interface FlxPageInfo {
  key: string;
  frame: { x: number; y: number; width: number; height: number };
  rotate: number;
  attachmentPage?: { file: string; index: number };
}

export interface FlexDocument {
  name: string;
  flxData: Uint8Array;
  info?: FlxDocInfo;
  thumbnail?: Uint8Array;
  pdfData?: Uint8Array;
  pages?: FlxPageInfo[];
  drawingsData?: Record<string, unknown>[];
  annotationsData?: unknown[];
}

export interface FlexFolder {
  name: string;
  path: string;
  documents: FlexDocument[];
}

export interface FlexBackup {
  info: FlexBackupInfo;
  folders: FlexFolder[];
  totalDocuments: number;
}

export async function parseFlexFile(file: File): Promise<FlexBackup> {
  const arrayBuffer = await file.arrayBuffer();
  const uint8 = new Uint8Array(arrayBuffer);

  const outerZip = unzipSync(uint8);

  let backupInfo: FlexBackupInfo = {
    appName: 'Flexcil',
    backupDate: '',
    appVersion: '',
    version: '',
  };

  const folderMap: Record<string, FlexFolder> = {};

  for (const [path, data] of Object.entries(outerZip)) {
    if (path === 'flexcilbackup/info') {
      try {
        backupInfo = JSON.parse(strFromU8(data));
      } catch {}
      continue;
    }

    if (!path.endsWith('.flx')) continue;

    const parts = path.split('/');
    if (parts.length < 4) continue;

    const folderName = parts[2];
    const fileName = parts[3].replace(/\.flx$/, '');

    if (!folderMap[folderName]) {
      folderMap[folderName] = {
        name: folderName,
        path: `flexcilbackup/Documents/${folderName}`,
        documents: [],
      };
    }

    const doc = await parseFlxFile(fileName, data);
    folderMap[folderName].documents.push(doc);
  }

  const folders = Object.values(folderMap);
  folders.forEach((f) =>
    f.documents.sort((a, b) => {
      const ad = a.info?.modifiedDate ?? 0;
      const bd = b.info?.modifiedDate ?? 0;
      return bd - ad;
    })
  );

  return {
    info: backupInfo,
    folders,
    totalDocuments: folders.reduce((s, f) => s + f.documents.length, 0),
  };
}

async function parseFlxFile(name: string, data: Uint8Array): Promise<FlexDocument> {
  const doc: FlexDocument = { name, flxData: data };

  try {
    const innerZip = unzipSync(data);

    if (innerZip['info']) {
      try { doc.info = JSON.parse(strFromU8(innerZip['info'])); } catch {}
    }

    if (innerZip['thumbnail']) {
      doc.thumbnail = innerZip['thumbnail'];
    } else if (innerZip['thumbnail@2x']) {
      doc.thumbnail = innerZip['thumbnail@2x'];
    }

    const pdfKey = Object.keys(innerZip).find((k) => k.startsWith('attachment/PDF/'));
    if (pdfKey) {
      doc.pdfData = innerZip[pdfKey];
    }

    if (innerZip['pages.index']) {
      try { doc.pages = JSON.parse(strFromU8(innerZip['pages.index'])); } catch {}
    }

    const drawingKey = Object.keys(innerZip).find((k) => k.endsWith('.drawings'));
    if (drawingKey) {
      try { doc.drawingsData = JSON.parse(strFromU8(innerZip[drawingKey])); } catch {}
    }

    const annotKey = Object.keys(innerZip).find((k) => k.endsWith('.annotations'));
    if (annotKey) {
      try { doc.annotationsData = JSON.parse(strFromU8(innerZip[annotKey])); } catch {}
    }
  } catch {
  }

  return doc;
}

export function formatDate(timestamp: number): string {
  if (!timestamp) return 'Unknown';
  const ms = timestamp > 1e12 ? timestamp : timestamp * 1000;
  return new Date(ms).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
