# Ink & Quill implementation tracker

This tracker distinguishes implemented code/UI from partial foundations and remaining product work.

## Implemented
- [x] Native Kotlin / Jetpack Compose Android app; GitHub Actions debug APK workflow
- [x] Offline SQLite storage, autosave, search (including shared tags), type filters, favorites, nested folders, collections, and persisted multi-document tabs
- [x] Rich HTML editor: bold, italic, underline, strike, headings, lists, alignment, indentation, quotes, horizontal rules, hyperlinks, inline images, editable HTML tables, undo/redo, text/highlight colors
- [x] Find/replace all within a document; word count; revision history and restore
- [x] Editor appearance: system/light/dark, five palettes plus custom hex accent, custom font import, font size, line spacing, paragraph spacing, page width/margins, page-like layout mode with explicit page breaks, focus mode, background color/image texture, text color, configurable toolbar
- [x] Customizable home section visibility and bottom navigation tab visibility
- [x] Optional salted-PBKDF2 PIN lock with automatic relock when app leaves foreground
- [x] World → Series/Story/Book/Chapter/Scene parent hierarchy, recursive world tree, and world-scoped record creation
- [x] Selective multi-project sharing with chosen Series/Stories; explicit labeled record relationships; labeled alternate entity snapshots and restore
- [x] World timelines; custom calendars with user-defined months, month lengths, weekdays, seasons, moons, eras, leap-rule notes, a month grid, dated entries, and yearly recurring events
- [x] Visual planning boards (corkboard, moodboard, map, flowchart, relationship graph, family tree, floor plan, reference board) with draggable image-backed colored cards, persistent positions, user-selected board backgrounds, linked record pins, and visual card-to-card links with labeled directional arrows, and deterministic auto-layout for family trees, flowcharts, timelines, relationship graphs, and corkboards
- [x] Shared reusable typed taxonomy tags across record types, assignment chips, and search integration
- [x] Starter template gallery expanded across novels, series, stories, poetry, journals, nonfiction, research, characters, worldbuilding, systems, plotting, calendars, maps, and references
- [x] Universal template field designer with 17 field types, sections, required/optional, dropdown/multi-select options, hints, conditional-visibility metadata, calculated formula metadata, repeatable-group metadata, field and section edit/reorder, recoverable removal/restore, snapshots, rename, duplicate, and search
- [x] Dynamic template entry forms with required validation, dropdowns, multi-selects, checkboxes, and conditional visibility
- [x] Deterministic local mention matching; ambiguous/generic names skipped; auto/suggest/off, alias/case settings, exclusions, persistent suggestions, approve/dismiss, exact matched text and character offsets, undoable link/unlink, retroactive scan, clickable backlinks that find and scroll to matched text
- [x] Attachment metadata, image previews, open/remove controls, inline image insertion, image bytes included in JSON/encrypted backups, strict media-restore validation (invalid, missing, unsupported, or unlinked blobs now fail the import instead of being silently skipped), and cleanup of app-imported attachment blobs when removed
- [x] Portable JSON backup/restore includes records, settings, tags, folders, collections, tabs, templates, relationships, boards, calendars, attachment bytes, and board background images; imports and full cloud restores are transactional so malformed data cannot leave a partially replaced library; backup export stops with a clear error instead of silently omitting inaccessible media
- [x] Password-encrypted AES-256-GCM backup/restore; optional provider-backed bidirectional sync with Android Keystore-protected password, initial run, manual sync, 12-hour WorkManager schedule, network constraint/retry, hash-based change detection, explicit keep-phone/use-cloud conflict resolution, and a persistent 30-event sync history including failures
- [x] TXT, Markdown, HTML, paginated PDF, DOCX, EPUB export; PDF, TXT, Markdown, HTML, DOCX, EPUB import

## Partially implemented; still needs completion
- [ ] Full project-aware World → multiple Series → Books → Chapters → Scenes navigator, richer standalone-story organization, project-specific resource views, and full multi-project shared-record navigation
- [ ] Template designer still needs more advanced calculated expressions, repeatable structured groups, multi-image galleries, conditional logic beyond simple equality, and template schema migration UI
- [ ] Mention system still needs exact text-position navigation/highlighting, robust ambiguity picker, link undo history, and a dedicated graph explorer
- [ ] Folder/collection management needs deeper hierarchy editing, bulk operations, and dedicated favorites manager
- [ ] Attachments still need a full in-app gallery, orphaned-blob cleanup after failed restore/replacement, cleanup of replaced board/background blobs, and robust behavior for revoked provider permissions; portable backup currently reads attachment bytes into memory
- [ ] Editor still needs a structured document model, true automatic multi-page pagination, page headers/footers, advanced table editing, imported font validation, and verified formatting/image fidelity across all exports
- [ ] Planning boards still need geographic map layers/coordinates, specialized family-tree and relationship graph layouts, timeline visualization, flowchart-specific connectors, inventory views, and board collaboration
- [ ] Calendars need validated custom-date arithmetic, era conversion, recurring-event expansion, and calendar grid views
- [ ] Cloud sync has whole-backup replacement conflict resolution, not field-level merging. Provider-specific adapters, change journal, offline merge queue visibility, restore preview, change journal, field-level merge, provider-specific adapters, and multi-device end-to-end testing remain
- [ ] PDF import fidelity tests, comprehensive migration tests, unit/instrumentation/UI/accessibility/performance tests, and real-device regression testing remain

## Non-negotiable product constraints
- No AI features, SDKs, APIs, model downloads, or services.
- Local phone storage is primary; writing and organization remain fully usable offline.
- Cloud backup/sync is optional, encrypted on-device, and user-selected. No mandatory account or subscription.
