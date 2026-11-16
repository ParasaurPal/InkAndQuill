# Ink & Quill implementation tracker

This tracker distinguishes working features from partial foundations and unfinished requirements. A checked item means the corresponding code and UI exist; it does not imply every edge case has been validated.

## Implemented
- [x] Native Kotlin / Jetpack Compose Android app with GitHub Actions debug APK builds
- [x] Offline SQLite records, autosave, search, type filters, favorites, folders, collections, and persisted multi-document tabs
- [x] Rich HTML editor with bold, italic, underline, strike, headings, ordered/unordered lists, alignment, indentation, block quotes, horizontal rules, links, text/highlight color, undo/redo
- [x] Find and replace all within the current document; word count
- [x] Revision history and restore; labeled alternate entity snapshots and restore
- [x] World, Series, Story, Book, Chapter, Scene and worldbuilding record types; parent hierarchy and recursive world tree
- [x] World-scoped creation, explicit labeled record relationships, selective sharing with chosen Series/Stories
- [x] Custom world calendars with user-defined months, weekdays, seasons, moons, and dated entries
- [x] World timelines and draggable planning boards with persistent card positions and explicit visual links between cards
- [x] Reusable typed taxonomy tags (Category, Property, Use, Element, Planet, Zodiac, Relationship, Custom), per-record assignments, and aliases data foundation
- [x] Starter template library and universal field designer with 17 field types, sections, required/optional, options, hints, field edit/reorder, recoverable removal/restore, template snapshots, rename, duplicate, and search
- [x] Local deterministic title/alias mention scanner; ambiguous/generic terms are skipped; auto/suggest/off modes; case and alias settings; exclusions; persisted suggestions; approve/dismiss; outgoing unlink controls; retroactive scan action; backlinks
- [x] Attachment references, image previews, open/remove controls
- [x] Appearance system/light/dark, five saved palettes, editor size, font family, line spacing, narrow page mode, focus mode
- [x] Customizable home sections and bottom navigation tab visibility
- [x] Optional local PIN lock using salted PBKDF2 verification
- [x] Comprehensive JSON backup/restore for records and app metadata tables
- [x] Password-encrypted AES-256-GCM backup/restore; password protected by Android Keystore for optional scheduled provider-backed backups, initial run plus 12-hour WorkManager schedule with network constraint and retry
- [x] TXT, Markdown, HTML, paginated PDF, DOCX, EPUB export; TXT, Markdown, HTML, DOCX, EPUB import

## Partially implemented; still needs completion
- [ ] Full structured World → multiple Series → Books → Chapters → Scenes navigator, independent standalone Stories, project-aware hierarchy validation, and shared resources surfaced in every relevant project
- [ ] Selective sharing UI exists, but full shared-record views, per-project permissions, and alternate depiction/version linking still need work
- [ ] Template fields can be defined and edited, but conditional visibility, repeatable groups, calculated fields, tables/relationship field editors, section reorder, schema migration UI, original-template restore, import/export/share, and gallery categories/previews remain
- [ ] Mention links need exact text-position navigation, ambiguity resolution UI, undo history, retroactive scan progress, and a dedicated filtered connection explorer
- [ ] Collections and folders need full rename/delete/nesting management; favorites need a dedicated manager
- [ ] Attachments have previews and open/remove controls, but inline editor image insertion, in-app gallery, copied attachment payloads in backups, and robust lifecycle handling remain
- [ ] Editor still needs a structured document model, true paginated page editor, tables, font import, page margins, paragraph spacing, background/texture, toolbar customization, and verified export formatting fidelity
- [ ] Planning boards support draggable cards and links, but true map layers/geographic coordinates, family-tree layouts, timeline visualization, flowcharts, inventory views, corkboard filters, moodboards, and reference-board tools remain
- [ ] Custom calendar basics exist; date validation, era conversion, recurring-event handling, multiple synchronized calendars, and calendar visualization remain
- [ ] Scheduled cloud backup is implemented, but this is one-way encrypted backup—not bidirectional sync. Provider-specific adapters, offline change journal, merge/conflict resolution, restore previews, sync status/history, and multi-device sync remain
- [ ] PDF import, full round-trip fidelity, migration tests, unit/instrumentation/UI/accessibility/performance tests, and real-device regression testing remain

## Non-negotiable product constraints
- No AI features, SDKs, APIs, model downloads, or services.
- Local phone storage remains the primary source of truth; writing and organization work offline.
- Cloud backup/sync is optional, encrypted on-device, and user-selected. No mandatory account or subscription.
