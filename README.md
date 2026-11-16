# Ink & Quill

A native Android writing studio built for writers, novelists, poets, journal keepers, and worldbuilders.

## Principles
- **Offline-first:** writing and organization are stored in the app's local SQLite database.
- **No AI anywhere:** no AI features, model calls, AI SDKs, or writing assistants.
- No mandatory account, subscription, or internet connection.
- Local data is the source of truth. JSON backup/restore is user-controlled.
- World-first story organization: Worlds contain Series and standalone Stories; Series can contain Books, Chapters, and Scenes.
- Templates and mention linking are local and deterministic.

## Current implemented foundation
- Native Jetpack Compose Android app and GitHub Actions debug APK workflow.
- SQLite-backed local records, autosaving rich HTML editor, formatting toolbar, and revision snapshots.
- Writing types for documents, novels, series, stories, books, chapters, scenes, poetry, journals, notes, and worldbuilding records.
- World/parent hierarchy fields, tags, aliases, and scope metadata.
- Local title/alias mention scanner with persisted backlinks.
- Search, type filters, recent writing, editable starter templates.
- Appearance and editor font-size preferences.
- JSON export/import.
- Android backup disabled; no INTERNET permission is declared.

## Build
JDK 17 and Gradle 8.11.1:
`gradle :app:assembleDebug`

GitHub Actions builds and uploads `ink-and-quill-debug` on pushes to `main`.

## Product scope and remaining work
See [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) and [ROADMAP.md](ROADMAP.md). The full requested product is larger than the current implemented foundation; maps, visual relationship tools, a universal template designer, advanced export formats, encrypted provider adapters, and extensive automated testing remain future implementation work.
