# Workspace

## Overview

pnpm workspace monorepo using TypeScript. Each package manages its own dependencies.

## Native Android App (root)

A full native Android app (Kotlin + Jetpack Compose) that is an exact port of the Flexcil Backup Viewer web app. Located at the root of the repository.

### Native App Structure
- `app/` — Kotlin source code
  - `data/` — FlexModel.kt (data classes), FlexParser.kt (ZIP/.flx parsing)
  - `viewmodel/` — FlexViewModel.kt (state, export logic, search)
  - `ui/screens/` — HomeScreen.kt, MainScreen.kt
  - `ui/components/` — SidebarContent.kt, DocumentViewer.kt, PdfViewer.kt, ExportDialog.kt
  - `ui/theme/` — Color.kt, Theme.kt, Type.kt
  - `MainActivity.kt`
- `gradle/libs.versions.toml` — version catalog
- `build.gradle.kts` / `settings.gradle.kts` — root Gradle config
- `.github/workflows/build-native-android.yml` — GitHub Actions to build debug APK

### Native App Features (all working)
- Open .flex backup files via file picker or "Open with"
- Hierarchical folder tree with expand/collapse and doc counts
- Search bar that filters documents by name in real-time
- Multi-select with checkboxes (select all per folder)
- Document viewer with 4 tabs: PDF | Preview | Annotations | Details
- PDF rendering via Android PdfRenderer with pinch-to-zoom
- Thumbnail/cover preview tab
- Annotation summary (pen strokes, highlights, annotations count)
- Full file details (name, size, dates, type, page count)
- Export selected PDFs to any folder (Storage Access Framework)
- Export selected PDFs as a ZIP file
- Single-document export from viewer
- Dark theme matching the web version exactly
- Snackbar feedback on export success/failure

### GitHub Actions Build
Push to `main`/`master` (or run manually) → builds debug APK → uploaded as GitHub artifact (90-day retention).

## Stack

- **Monorepo tool**: pnpm workspaces
- **Node.js version**: 24
- **Package manager**: pnpm
- **TypeScript version**: 5.9
- **API framework**: Express 5
- **Database**: PostgreSQL + Drizzle ORM
- **Validation**: Zod (`zod/v4`), `drizzle-zod`
- **API codegen**: Orval (from OpenAPI spec)
- **Build**: esbuild (CJS bundle)

## Key Commands

- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- `pnpm --filter @workspace/api-server run dev` — run API server locally

See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.
