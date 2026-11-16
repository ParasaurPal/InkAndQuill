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
- Local title/alias mention scanner with persisted backlinks.
- Search, type filters, recent writing, editable starter templates.
- Appearance and editor font-size preferences.
- JSON backup/restore; password-encrypted cloud backup/restore through Android's Storage Access Framework; TXT, Markdown, HTML, paginated PDF, DOCX, and EPUB export; text/Markdown/HTML/DOCX/EPUB import.
- Android automatic device backup is disabled to avoid unintended unencrypted cloud copies. User-initiated cloud backup is encrypted locally and saved to a provider the user selects.

## Build
JDK 17 and Gradle 8.11.1:
`gradle :app:assembleDebug`

GitHub Actions builds and uploads `ink-and-quill-debug` on pushes to `main`.

## Product scope and remaining work
See [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) and [ROADMAP.md](ROADMAP.md). The full requested product is larger than the current implemented foundation; maps, visual relationship tools, a universal template designer, automatic scheduled cloud sync/offline retry/conflict handling, and extensive automated testing remain future implementation work.
