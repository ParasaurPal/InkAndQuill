# Ink & Quill implementation tracker

## Implemented in the current codebase
- [x] Native Kotlin / Jetpack Compose Android project
- [x] Offline SQLite storage and record CRUD
- [x] Autosaving rich-text HTML editor with bold, italic, underline, strike, headings, lists, and alignment
- [x] Local revision snapshots on content changes
- [x] World/parent/scope metadata and story record types
- [x] Starter editable template records for major writing and worldbuilding categories
- [x] Deterministic local exact-title and alias mention scanning
- [x] Persisted backlinks
- [x] Search and type filters
- [x] Appearance and font-size preferences
- [x] JSON backup export and import
- [x] Android cloud backup disabled; no network permission in manifest
- [x] GitHub Actions debug APK build workflow

## Remaining to complete the full requested product
- [ ] Structured rich-text document model, links, colors, highlights, tables, page layout, pagination, focus mode, tabs, undo/redo
- [ ] Full World → Series → Book → Chapter → Scene navigator with selective sharing and historical snapshots
- [ ] Universal template designer with custom field types, conditional fields, repeatable groups, template import/export/versioning
- [ ] Link review queue, ambiguity resolution, exclusions, case settings, retroactive scanning, undo, exact source navigation
- [ ] Full-text indexing, find/replace, folders, collections, favorites management
- [ ] Image and file attachments, reference galleries
- [ ] Maps, calendars, visual timelines, family trees, relationship graph, flowcharts, corkboards, moodboards
- [ ] PDF, DOCX, TXT, HTML, Markdown, EPUB import/export
- [ ] Custom font import, palettes, margins, page widths, line spacing, toolbar and home layout customization
- [ ] Encrypted client-side, provider-flexible optional cloud backup/sync
- [ ] Restore validation, database migrations, unit/instrumentation/UI/accessibility/performance tests

The app must continue to function completely offline when these optional integrations are added.
