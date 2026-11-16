# Ink & Quill implementation tracker

This tracker distinguishes implemented code/UI from partial foundations and remaining product work.

## Implemented
- [x] Native Kotlin / Jetpack Compose Android app; GitHub Actions unit-test and debug APK workflow
- [x] Offline SQLite storage, autosave, search (including shared tags), type filters, favorites, nested folders, collections, and persisted multi-document tabs
- [x] Rich HTML editor WebView isolated with a restrictive CSP, no file/content access, no mixed content, and no navigation from document content; initial HTML is Base64-encoded before bootstrap injection
- [x] Rich HTML editor: bold, italic, underline, strike, headings, lists, alignment, indentation, quotes, horizontal rules, hyperlinks, inline images, editable HTML tables, undo/redo, text/highlight colors
- [x] Find/replace all within a document; word count; revision history and restore
- [x] Editor appearance: system/light/dark, five palettes plus custom hex accent, custom font import, font size, line spacing, paragraph spacing, page width/margins, page-like layout mode with explicit page breaks, focus mode, background color/image texture, text color, configurable toolbar
- [x] Customizable home section visibility and bottom navigation tab visibility
- [x] Optional salted-PBKDF2 PIN lock with automatic relock when app leaves foreground
- [x] World → Series/Story/Book/Chapter/Scene parent hierarchy, recursive world tree, world-scoped record creation, allowed parent-type validation, and self/cycle prevention
- [x] Selective multi-project sharing with chosen Series/Stories; explicit labeled record relationships; labeled alternate entity snapshots and restore
- [x] World timelines; custom calendars with user-defined months, month lengths, weekdays, seasons, moons, eras, leap-rule notes, a month grid, dated entries, and yearly recurring events
- [x] Visual planning boards (corkboard, moodboard, map, flowchart, relationship graph, family tree, floor plan, reference board) with draggable image-backed colored cards, persistent positions, user-selected board backgrounds, linked record pins, and visual card-to-card links with labeled directional arrows, and deterministic auto-layout for family trees, flowcharts, timelines, relationship graphs, and corkboards
- [x] Shared reusable typed taxonomy tags across record types, assignment chips, and search integration
- [x] Starter template gallery expanded across novels, series, stories, poetry, journals, nonfiction, research, characters, worldbuilding, systems, plotting, calendars, maps, and references
- [x] Universal template field designer with 17 field types, sections, required/optional, dropdown/multi-select options, hints, conditional-visibility metadata, calculated formula metadata, repeatable-group metadata, field and section edit/reorder, recoverable removal/restore, snapshots, rename, duplicate, and search
- [x] Dynamic template entry forms with required validation, dropdowns, multi-selects, checkboxes, and conditional visibility
- [x] Deterministic local mention matching; ambiguous/generic names skipped; auto/suggest/off, alias/case settings, exclusions, reconciled suggestions and mention offsets, approve/dismiss, undoable link/unlink, retroactive scan, clickable backlinks that find and scroll to matched text
- [x] Attachment metadata, image previews, open/remove controls, inline image insertion, image bytes included in JSON/encrypted backups, strict media-restore validation (invalid, duplicate, missing, unsupported, or unlinked blobs fail instead of being silently skipped), staged media installation with restoration of previous files if the database import fails, startup cleanup of abandoned temporary staging files, and cleanup of app-imported attachment and board-image blobs when their records are deleted, removal of orphaned board connectors and board contents with deleted records/worlds, and cleanup of obsolete imported blobs after successful full replacement
- [x] Portable JSON backup/restore includes records, settings, tags, folders, collections, tabs, templates, relationships, boards, calendars, attachment bytes, and board background images; imports and full cloud restores are transactional so malformed data cannot leave a partially replaced library; backup export stops with a clear error instead of silently omitting inaccessible media
- [x] Password-encrypted AES-256-GCM backup/restore; derived encryption keys are cleared on every exit path; optional provider-backed bidirectional sync with Android Keystore-protected password, initial run, manual sync, 12-hour WorkManager schedule, network constraint/retry, hash-based change detection, explicit keep-phone/use-cloud conflict resolution, and a persistent 30-event sync history including failures
- [x] TXT, Markdown, HTML, paginated PDF, DOCX, EPUB export; PDF, TXT, Markdown, HTML, DOCX, EPUB import

## Partially implemented; still needs completion
- [x] World → multiple Series → Books → Chapters → Scenes and standalone Story → Chapters → Scenes navigation; project-specific workspaces list books/chapters and explicitly shared worldbuilding records
- [ ] Cross-project shared-record manager with filtering, bulk sharing, and navigation across every linked project
- [x] Calculated template fields support arithmetic, parentheses, field references, aggregates, text functions, conditional expressions, coalesce, rounding, and safe error reporting
- [x] Repeatable template groups use versioned structured storage with editable subfields, add/remove controls, required-value checks, and readable document output
- [x] Conditional template visibility supports equality, inequality, numeric comparisons, contains/starts/ends, set membership, and empty checks
- [ ] Template designer still needs template schema migration UI and richer layout customization
- [ ] Mention system still needs exact text-position navigation/highlighting, robust ambiguity picker, link undo history, and a dedicated graph explorer
- [ ] Folder/collection management needs deeper hierarchy editing, bulk operations, and dedicated favorites manager
- [ ] Attachments still need a full in-app gallery, recovery of preserved previous-media files after process termination (requires commit-aware recovery before safe deletion), cleanup of obsolete blobs after successful full replacement, and robust behavior for revoked provider permissions; portable backup currently reads attachment bytes into memory
- [ ] Editor still needs a structured document model, true automatic multi-page pagination, page headers/footers, advanced table editing, imported font validation, and verified formatting/image fidelity across all exports
- [ ] Planning boards still need geographic map layers/coordinates, specialized family-tree and relationship graph layouts, timeline visualization, flowchart-specific connectors, inventory views, and board collaboration
- [x] Custom calendar month/day validation, weekday-aligned month grids, supported recurring leap-day rules, and recurring-event suppression on years where the date does not exist
- [ ] Calendars still need era conversion, richer recurrence rules, and timeline/calendar cross-linking
- [ ] Cloud sync has whole-backup replacement conflict resolution, not field-level merging. Provider-specific adapters, change journal, offline merge queue visibility, restore preview, change journal, field-level merge, provider-specific adapters, and multi-device end-to-end testing remain
- [x] Unit regression tests cover encrypted backup integrity, custom calendar arithmetic, calculated fields, conditional template visibility, repeatable groups, and project hierarchy rules; Android CI runs tests before APK assembly
- [ ] PDF import fidelity tests, comprehensive migration tests, instrumentation/UI/accessibility/performance tests, and real-device regression testing remain

## Non-negotiable product constraints
- No AI features, SDKs, APIs, model downloads, or services.
- Local phone storage is primary; writing and organization remain fully usable offline.
- Cloud backup/sync is optional, encrypted on-device, and user-selected. No mandatory account or subscription.
