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
  Sun,
  Moon,
  CheckSquare,
  Square,
} from 'lucide-react';
import type { FlexBackup, FlexDocument } from '@/lib/flexcil-parser';
import { formatDate } from '@/lib/flexcil-parser';
import { useTheme } from '@/lib/theme';

interface SidebarProps {
  backup: FlexBackup;
  selectedDoc: FlexDocument | null;
  onSelectDoc: (doc: FlexDocument, folderName: string) => void;
  onReset: () => void;
  checkedDocs: Map<string, { doc: FlexDocument; folderName: string }>;
  onToggleCheck: (key: string, doc: FlexDocument, folderName: string) => void;
  onToggleFolderCheck: (folderName: string, docs: FlexDocument[]) => void;
}

export function Sidebar({
  backup,
  selectedDoc,
  onSelectDoc,
  onReset,
  checkedDocs,
  onToggleCheck,
  onToggleFolderCheck,
}: SidebarProps) {
  const { theme, toggle: toggleTheme } = useTheme();
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

  function folderCheckState(folder: typeof filteredFolders[0]): 'all' | 'none' | 'partial' {
    const checked = folder.documents.filter((d) => checkedDocs.has(folder.name + '/' + d.name));
    if (checked.length === 0) return 'none';
    if (checked.length === folder.documents.length) return 'all';
    return 'partial';
  }

  return (
    <aside className="w-72 min-w-[220px] max-w-xs flex flex-col h-full bg-sidebar border-r border-sidebar-border overflow-hidden shrink-0">
      <div className="flex items-center gap-2 px-3 py-3 border-b border-sidebar-border">
        <button
          onClick={onReset}
          className="p-1.5 rounded hover:bg-sidebar-accent/50 text-muted-foreground hover:text-foreground transition-colors"
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
        <button
          onClick={toggleTheme}
          className="p-1.5 rounded hover:bg-sidebar-accent/50 text-muted-foreground hover:text-foreground transition-colors"
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
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
            <button onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      {checkedDocs.size > 0 && (
        <div className="px-3 py-1.5 bg-primary/10 border-b border-primary/20">
          <p className="text-xs text-primary font-medium">
            {checkedDocs.size} selected — use Export bar below
          </p>
        </div>
      )}

      <div className="flex-1 overflow-y-auto py-1">
        {filteredFolders.map((folder) => {
          const cs = folderCheckState(folder);
          return (
            <div key={folder.name}>
              <div className="flex items-center">
                <button
                  onClick={(e) => { e.stopPropagation(); onToggleFolderCheck(folder.name, folder.documents); }}
                  className="pl-3 pr-1 py-2 text-muted-foreground hover:text-primary transition-colors shrink-0"
                  title={cs === 'all' ? 'Deselect folder' : 'Select all in folder'}
                >
                  {cs === 'all' ? (
                    <CheckSquare className="w-3.5 h-3.5 text-primary" />
                  ) : cs === 'partial' ? (
                    <CheckSquare className="w-3.5 h-3.5 text-primary/50" />
                  ) : (
                    <Square className="w-3.5 h-3.5" />
                  )}
                </button>
                <button
                  onClick={() => toggleFolder(folder.name)}
                  className="flex-1 flex items-center gap-2 pr-3 py-2 hover:bg-sidebar-accent/50 transition-colors text-left"
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
              </div>

              {expandedFolders[folder.name] && (
                <div>
                  {folder.documents.map((doc) => {
                    const displayName = doc.info?.name ?? doc.name;
                    const isSelected = selectedDoc === doc;
                    const checkKey = folder.name + '/' + doc.name;
                    const isChecked = checkedDocs.has(checkKey);
                    return (
                      <div
                        key={doc.name}
                        className={`flex items-start gap-1 mx-1 my-0.5 rounded-md transition-colors
                          ${isSelected ? 'bg-primary/12' : 'hover:bg-sidebar-accent/30'}`}
                      >
                        <button
                          onClick={(e) => { e.stopPropagation(); onToggleCheck(checkKey, doc, folder.name); }}
                          className="pl-8 pr-1 py-2 text-muted-foreground hover:text-primary transition-colors shrink-0"
                        >
                          {isChecked ? (
                            <CheckSquare className="w-3.5 h-3.5 text-primary" />
                          ) : (
                            <Square className="w-3.5 h-3.5" />
                          )}
                        </button>
                        <button
                          onClick={() => onSelectDoc(doc, folder.name)}
                          className="flex-1 flex items-start gap-2 pr-3 py-2 text-left min-w-0"
                        >
                          <FileText className={`w-3.5 h-3.5 mt-0.5 shrink-0 ${isSelected ? 'text-primary' : 'text-muted-foreground'}`} />
                          <div className="min-w-0 flex-1">
                            <p className={`text-xs font-medium truncate leading-tight ${isSelected ? 'text-primary' : 'text-sidebar-foreground'}`}>
                              {displayName}
                            </p>
                            {doc.info?.modifiedDate && (
                              <p className="text-[10px] text-muted-foreground mt-0.5 truncate">
                                {formatDate(doc.info.modifiedDate)}
                              </p>
                            )}
                            {doc.pdfData && (
                              <span className="text-[9px] bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 px-1 py-0.5 rounded mt-0.5 inline-block">PDF</span>
                            )}
                          </div>
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}

        {filteredFolders.length === 0 && (
          <div className="text-center py-8 text-muted-foreground text-sm">No documents found</div>
        )}
      </div>

      <div className="px-4 py-3 border-t border-sidebar-border bg-sidebar/80">
        <p className="text-[11px] text-muted-foreground">
          {backup.totalDocuments} documents · {backup.folders.length} folders
        </p>
        <p className="text-[11px] text-muted-foreground">Flexcil v{backup.info.appVersion}</p>
      </div>
    </aside>
  );
}
