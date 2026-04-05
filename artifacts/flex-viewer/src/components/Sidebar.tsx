import { useState } from 'react';
import {
  FolderOpen,
  Folder,
  FileText,
  ChevronRight,
  ChevronDown,
  Home,
  Search,
  X,
} from 'lucide-react';
import type { FlexBackup, FlexDocument } from '@/lib/flexcil-parser';
import { formatDate } from '@/lib/flexcil-parser';

interface SidebarProps {
  backup: FlexBackup;
  selectedDoc: FlexDocument | null;
  onSelectDoc: (doc: FlexDocument, folderName: string) => void;
  onReset: () => void;
}

export function Sidebar({ backup, selectedDoc, onSelectDoc, onReset }: SidebarProps) {
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>(
    Object.fromEntries(backup.folders.map((f) => [f.name, true]))
  );
  const [search, setSearch] = useState('');

  const toggleFolder = (name: string) => {
    setExpandedFolders((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  const filteredFolders = backup.folders
    .map((folder) => ({
      ...folder,
      documents: folder.documents.filter((doc) =>
        search
          ? doc.name.toLowerCase().includes(search.toLowerCase()) ||
            (doc.info?.name ?? '').toLowerCase().includes(search.toLowerCase())
          : true
      ),
    }))
    .filter((f) => f.documents.length > 0 || !search);

  return (
    <aside className="w-72 min-w-[220px] max-w-xs flex flex-col h-full bg-sidebar border-r border-sidebar-border overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-sidebar-border">
        <button
          onClick={onReset}
          className="flex items-center gap-2 text-xs text-muted-foreground hover:text-foreground transition-colors"
          title="Open another file"
        >
          <Home className="w-4 h-4" />
        </button>
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-sm truncate text-sidebar-foreground">
            {backup.info.appName || 'Flexcil Backup'}
          </p>
          <p className="text-[11px] text-muted-foreground truncate">{backup.info.backupDate}</p>
        </div>
      </div>

      <div className="px-3 py-2 border-b border-sidebar-border">
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground pointer-events-none" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search documents..."
            className="w-full pl-8 pr-8 py-1.5 text-xs rounded-md bg-background border border-border focus:outline-none focus:ring-1 focus:ring-ring"
          />
          {search && (
            <button
              onClick={() => setSearch('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto py-2">
        {filteredFolders.map((folder) => (
          <div key={folder.name}>
            <button
              onClick={() => toggleFolder(folder.name)}
              className="w-full flex items-center gap-2 px-3 py-2 hover:bg-sidebar-accent/50 transition-colors text-left group"
            >
              <span className="text-muted-foreground">
                {expandedFolders[folder.name] ? (
                  <ChevronDown className="w-3.5 h-3.5" />
                ) : (
                  <ChevronRight className="w-3.5 h-3.5" />
                )}
              </span>
              {expandedFolders[folder.name] ? (
                <FolderOpen className="w-4 h-4 text-primary shrink-0" />
              ) : (
                <Folder className="w-4 h-4 text-primary shrink-0" />
              )}
              <span className="text-sm font-medium truncate text-sidebar-foreground flex-1">{folder.name}</span>
              <span className="text-[10px] text-muted-foreground shrink-0 bg-muted px-1.5 py-0.5 rounded-full">
                {folder.documents.length}
              </span>
            </button>

            {expandedFolders[folder.name] && (
              <div className="pl-6">
                {folder.documents.map((doc) => {
                  const displayName = doc.info?.name ?? doc.name;
                  const isSelected = selectedDoc === doc;
                  return (
                    <button
                      key={doc.name}
                      onClick={() => onSelectDoc(doc, folder.name)}
                      className={`w-full flex items-start gap-2 px-3 py-2 transition-colors text-left rounded-md mx-1 my-0.5
                        ${isSelected ? 'bg-primary/15 text-primary' : 'hover:bg-sidebar-accent/40 text-sidebar-foreground'}`}
                    >
                      <FileText className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${isSelected ? 'text-primary' : 'text-muted-foreground'}`} />
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-medium truncate leading-tight">{displayName}</p>
                        {doc.info?.modifiedDate && (
                          <p className="text-[10px] text-muted-foreground mt-0.5 truncate">
                            {formatDate(doc.info.modifiedDate)}
                          </p>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        ))}

        {filteredFolders.length === 0 && (
          <div className="text-center py-8 text-muted-foreground text-sm">
            No documents found
          </div>
        )}
      </div>

      <div className="px-4 py-3 border-t border-sidebar-border bg-sidebar/80">
        <p className="text-[11px] text-muted-foreground">
          {backup.totalDocuments} documents · {backup.folders.length} folders
        </p>
        <p className="text-[11px] text-muted-foreground">v{backup.info.appVersion}</p>
      </div>
    </aside>
  );
}
