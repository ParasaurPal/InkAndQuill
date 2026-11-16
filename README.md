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
- World timelines and calendar grids (custom month lengths, eras, leap-rule notes, seasons, moons, recurring events), explicit relationships, alternate snapshots, selective project sharing, image-backed linked planning boards with labeled directional connections and auto-layout, and a world-wide connection graph.
- Appearance palettes and custom accent colors, reorderable home/navigation layout, custom font import, focus mode, page-like editing with page breaks, editor typography/page width/spacing/backgrounds, and optional local PIN lock.
- Comprehensive portable JSON backup/restore including attachments and board images; password-encrypted cloud sync through Android's Storage Access Framework with WorkManager retries and explicit conflict choices; TXT, Markdown, HTML, paginated PDF, DOCX, and EPUB export; PDF, text, Markdown, HTML, DOCX, and EPUB import.
- Android automatic device backup is disabled to avoid unintended unencrypted cloud copies. User-initiated cloud backup is encrypted locally and saved to a provider the user selects.

## Build
JDK 17 and Gradle 8.11.1:
`gradle :app:assembleDebug`

GitHub Actions builds and uploads `ink-and-quill-debug` on pushes to `main`.

## Product scope and remaining work
See [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) and [ROADMAP.md](ROADMAP.md). The product is still under active implementation. Optional cloud sync now supports encrypted bidirectional whole-backup exchange, offline WorkManager retry, hash-based change detection, and explicit keep-phone/use-cloud conflict choices. Field-level merging and detailed sync history remain unfinished. Geographic map coordinates/layers, advanced family-tree layout tuning, inventory-specific views, more advanced template formulas and conditional logic, automatic multi-page pagination, richer DOCX/PDF formatting fidelity, detailed sync history/field-level merging, and extensive automated testing remain in the roadmap.


Cloud sync setup and conflict behavior: [docs/CLOUD_SYNC.md](docs/CLOUD_SYNC.md).
