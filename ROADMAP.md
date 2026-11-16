# Ink & Quill implementation tracker

## Implemented in the current codebase
- [x] Native Kotlin / Jetpack Compose Android project
- [x] Offline SQLite storage and record CRUD
- [x] Autosaving rich-text HTML editor with bold, italic, underline, strike, headings, lists, alignment, undo/redo, and text/highlight colors
- [x] Local revision snapshots and restore
- [x] World/parent/scope metadata and core story record types
- [x] Starter templates across writing, characters, worldbuilding, magic, plot, time, visual planning, and research
- [x] Edit existing template records and create basic custom template records
- [x] Deterministic local exact-title and alias mention matching
- [x] Configurable auto/suggest/off mention modes, per-record excluded terms, persisted suggestion queue, and backlinks
- [x] Search and type filters
- [x] Appearance mode, saved editor font size, and selectable Plum/Ocean/Forest/Rose/Amber palettes
- [x] JSON backup export/import
- [x] Local data foundations for folders, collections, attachments, open tabs, link exclusions, and suggestion review
- [x] Document find/replace-all
- [x] Persistent attachment metadata for user-selected images/files (provider URI references; files remain in chosen storage)
- [x] TXT, Markdown, HTML, paginated PDF, DOCX, and EPUB export
- [x] TXT, Markdown, HTML, DOCX, and EPUB import
- [x] Password-encrypted AES-256-GCM backup files, encrypted locally before export through Android's user-selected document provider
- [x] Android debug APK workflow

## Still to implement (do not treat the data foundations above as finished UI)
- [ ] Complete World → Series → Book → Chapter → Scene navigator, multiple independent series and standalone stories, selective multi-project sharing, and alternate character/entity snapshots
- [ ] Full universal template designer: all field types, conditional/repeatable groups, reorderable sections, template schema versioning, recoverable removed fields, import/export, and gallery filtering
- [ ] Link review/ambiguity UI, exact text-position navigation, connection manager, undo, retroactive scan controls, and visual connection explorer
- [ ] Fully interactive folder and collection management, folder filtering, favorites manager, and multi-document tab UI (tables exist for some of these foundations)
- [ ] Robust attachment gallery, inline image insertion, previews, and attachment lifecycle management
- [ ] Structured rich-text document model, hyperlink insertion, tables, page mode/pagination, focus mode, typography controls, and richer formatting fidelity across exports
- [ ] Visual maps, calendars, timelines, family trees, relationship graph, flowcharts, corkboards, moodboards, and research/reference boards
- [ ] Custom font import, line/paragraph spacing, margins, page width, backgrounds/textures, toolbar and home-layout customization
- [ ] PDF import and round-trip fidelity tests
- [ ] Automatic scheduled cloud sync, offline retry queue, conflict handling, sync status, and provider-specific sync adapters; current cloud feature is manual encrypted backup/restore
- [ ] Restore validation, database migration tests, unit/instrumentation/UI/accessibility/performance tests

## Product constraints
- No AI features, SDKs, APIs, or services.
- Local phone storage remains the primary source of truth and the app must remain fully usable offline.
- Cloud backup is optional, encrypted on-device, and user-selected; no mandatory account or subscription.
