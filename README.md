# Ink & Quill

A native Android writing studio built for writers, novelists, poets, journal keepers, and worldbuilders.

## Principles
- **Offline-first:** writing and organization are stored in the app's local SQLite database.
- **No AI anywhere:** no AI features, model calls, AI SDKs, or writing assistants.
- No mandatory account, subscription, or internet connection.
- Local data is the source of truth. Backups are optional and user-controlled.
- Cloud backup files are encrypted on-device with a password (AES-256-GCM) before being handed to the Android storage provider selected by the user. The app does not receive provider credentials.
- World-first story organization: Worlds contain Series and standalone Stories; Series can contain Books, Chapters, and Scenes.
- Templates and mention linking are local and deterministic.

## Current implemented foundation
- Native Jetpack Compose Android app and GitHub Actions debug APK workflow.
- SQLite-backed local records, autosaving rich HTML editor, formatting toolbar, and revision snapshots.
- Writing types for documents, novels, series, stories, books, chapters, scenes, poetry, journals, notes, and worldbuilding records.
- World/parent hierarchy fields, tags, aliases, and scope metadata.
- Local title/alias mention scanner with persisted exact-term backlinks, occurrence navigation, reviewable suggestions, and undoable link changes.
- Search, type filters, folders, collections, favorites, multi-document tabs, recent writing, and a categorized template gallery, dynamic entry forms, editable reusable field designer, and individual template import/export/share.
- World timelines and calendar grids (custom month lengths, eras, leap-rule notes, seasons, moons, recurring events), explicit relationships, alternate snapshots, selective project sharing, image-backed linked planning boards with labeled directional connections and auto-layout, interactive maps with pins, zoom, and nested maps, a manuscript outline view with chapter cards, nested scenes, and reorder, a world module hub grouping timeline, characters, locations, items, languages, flora, and fauna, and a world-wide connection graph.
- Appearance palettes and custom accent colors, reorderable home/navigation layout, custom font import, focus mode, page-like editing with page breaks, editor typography/page width/spacing/backgrounds, and optional local PIN lock.
- Comprehensive portable JSON backup/restore including attachments and board images; password-encrypted cloud sync through Android's Storage Access Framework with WorkManager retries and explicit conflict choices; TXT, Markdown, HTML, paginated PDF, DOCX, and EPUB export; PDF, text, Markdown, HTML, DOCX, and EPUB import.
- Android automatic device backup is disabled to avoid unintended unencrypted cloud copies. User-initiated cloud backup is encrypted locally and saved to a provider the user selects.

## Build
JDK 17 and Gradle 8.11.1:
`gradle :app:assembleDebug`

GitHub Actions builds on pushes to `main` and publishes the APK to the [latest test release](https://github.com/ParasaurPal/InkAndQuill/releases/tag/debug-latest), shown on the repo's main page. Download it directly: [InkAndQuill.apk](https://github.com/ParasaurPal/InkAndQuill/releases/download/debug-latest/InkAndQuill.apk).

## Product scope and remaining work
See [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) and [ROADMAP.md](ROADMAP.md). The product is still under active implementation. Optional cloud sync now supports encrypted bidirectional whole-backup exchange, offline WorkManager retry, hash-based change detection, explicit keep-phone/use-cloud conflict choices, and a persistent recent sync history including failures. Field-level merging and detailed sync history remain unfinished. Advanced family-tree layout tuning, inventory-specific views, more advanced template formulas and conditional logic, automatic multi-page pagination, richer DOCX/PDF formatting fidelity, detailed sync history/field-level merging, and extensive automated testing remain in the roadmap.


Cloud sync setup and conflict behavior: [docs/CLOUD_SYNC.md](docs/CLOUD_SYNC.md).


## Novel Studio and offline web app

The Android app now includes a separate **Novel Studio** workspace backed by its own local SQLite database. It creates Universes, Books, Chapters and Scenes; stores scene planning fields and manuscript text; supports focus mode, local autosave, manual scene checkpoints, plain-text scene export, and portable Fortelling JSON project export/import. Existing Ink & Quill libraries remain untouched by the new store.

A responsive offline-capable web/PWA client is in `web/`. It uses IndexedDB for local persistence and includes manuscript outline/editor, browser rich-text controls, focus mode, Story Bible entries, timelines, text-card boards, writing goals/logs, and JSON project import/export.

Run the web client locally:

```sh
cd web
npm install
npm run dev
```

Create a production web build:

```sh
cd web
npm run build
```

The web client is a PWA and can be installed from a supported browser. It stores content in that browser's IndexedDB; export backups regularly, especially before clearing browser data or changing devices.

**Implementation status:** this is an active implementation. The new schema establishes the wider relational feature model, but advanced canvas drawing/family-tree interactions, full rich-text document parity, DOCX/EPUB publishing, multi-user collaboration, public wiki hosting, and a native desktop package are not yet complete. The product remains no-AI and local-first.


### Expanded offline studio modules

The web client now includes responsive workspaces for the manuscript editor, Story Bible, multiple timelines and events, visual boards with draggable cards/connectors/freehand drawing, family relationship records, custom entity schemas, inline relational tables, writing sprints, word goals and daily logs, editable writing prompts, sharing/role manifests, static read-only wiki export, DOCX/TXT import, DOCX/EPUB/TXT publishing, theme and type-size controls, and portable JSON/ZIP project exports. It remains browser-local and does not transmit manuscript content.

The web CI workflow also runs on pushes to the active Novel Studio feature branch so that the client can be validated while development continues.


#### Native Android planning tools

The Android Novel Studio now opens a native planning workspace from the selected Universe. It exposes the shared Story Bible entity store, timeline and moment records, board cards, and writing-goal records alongside the manuscript hierarchy. The same relational store backs portable project export/import.

#### Portable privacy and publishing

The browser client supports AES-256-GCM encrypted backup files using a password-derived key (PBKDF2-SHA-256), conflict-preserving imports, JSON and ZIP project archives, DOCX text/paragraph formatting import, DOCX/EPUB/TXT export, browser print-to-PDF, board PNG export, and selected-entry static wiki HTML export. The web service worker precaches the compiled PWA shell and assets for offline startup after installation.

**Infrastructure boundary:** live multi-user presence/chat, remote permission enforcement, remote cloud sync, and hosted public wiki deployment require a backend and a user-selected provider/account. This build does not silently upload manuscripts or claim those network services are live. The local role/invitation manifest, local chat log, and static wiki export work without a server.


The web client also offers an optional user-configured WebDAV sync target. It encrypts the full project with AES-GCM before upload, uses credentials only for the current session, merges records by stable IDs, and retains conflicting versions as recovery snapshots. The WebDAV server must allow browser CORS requests; no provider is preselected and sync is never enabled without the user entering an endpoint and key.
